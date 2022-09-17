/*
 * Copyright (c) 2018-2022, Rapidomize.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * OR contact:
 * contact@rapidomize.com
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.rapidomize.ics.sdk.transport;

import com.rapidomize.ics.sdk.common.MediaType;
import com.rapidomize.ics.sdk.common.Json;
import com.rapidomize.ics.sdk.events.Handler;
import com.rapidomize.ics.sdk.events.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static com.rapidomize.ics.sdk.events.Message.*;

final class Http extends Transport {

    private  final Logger logger = LoggerFactory.getLogger(Http.class);


    final String host;
    final int port;
    final URI uri;

    EventLoopGroup clientGroup;
    Bootstrap client;
    protected Channel clientChanenel;
    ChannelHandlerContext bctx;

    boolean isWS = false;

    //RTM API
    WsHandler wsh;

    Http(int trans, String appId, String token, Handler handler)  {
        super(handler);
//        if(trans < 0 || trans > 1) throw new IllegalArgumentException("invalid transport type");
        if(token == null || token.isEmpty()) throw new IllegalArgumentException("token cannot be null/empty");
        if(appId == null || appId.isEmpty()) throw new IllegalArgumentException("App/Device ID cannot be null/empty");


        isWS = trans == WSS;

        try {
            this.appId = appId;
            setToken(token);

            host = CONF.getProperty("ep.host");
            if(host == null || host.isEmpty())
                throw new IllegalArgumentException("invalid uri, cannot be null/empty");

            String epUri = (isWS ?"wss://"+host+"/w":"https://"+host) + Handler.EP_PATH;
            epUri = epUri.endsWith("/")? epUri+appId :epUri+"/"+appId;
            uri = new URI(isWS ? epUri+"?token="+ token :epUri);

            port = uri.getPort() < 0?443:uri.getPort();

            init();

        } catch (Exception e) {
            //FIXME: retry on failure
            throw new IllegalStateException(e);
        }
    }

    //for extensions - HTTPS only
    protected Http(int trans, Handler handler) {
        super(handler);
        if(trans < 0 || trans > 1) throw new IllegalArgumentException("invalid transport type");

        try {
            this.appId = "";
            this.token = "";
            final String ephost = CONF.getProperty("ep.host");

            if(ephost == null || ephost.isEmpty())
                throw new IllegalArgumentException("invalid uri, cannot be null/empty");

            final String epUri = isWS ?"wss://":"https://"+ephost + Handler.EP_PATH;

            uri = new URI(epUri);

            host = uri.getHost();
            port = uri.getPort() < 0?443:uri.getPort();

            init();
        } catch (Exception e) {
            //FIXME: retry on failure
            throw new IllegalStateException(e);
        }
    }

    private void init() throws Exception {
        final SslContext sslCtx = sslCtx();

        wsh = isWS ? new WsHandler(WebSocketClientHandshakerFactory.newHandshaker(uri,
                            WebSocketVersion.V13, null, true, new DefaultHttpHeaders()), this)
                   : null;

        clientGroup = new NioEventLoopGroup();
        client = new Bootstrap();
        client.group(clientGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        if (sslCtx != null) {
                            p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                        }

                        p.addLast(new HttpClientCodec());
                        p.addLast(new HttpObjectAggregator(1024 * 1024));
                        if(isWS) {
//                            p.addLast(WebSocketClientCompressionHandler.INSTANCE);
//                            p.addLast(new WebSocketClientProtocolHandler(uri,
//                                              WebSocketVersion.V13, null, true, null, 1024*1024));
//                            p.addLast(new WebSocketClientProtocolHandler(
//                                              WebSocketClientHandshakerFactory.newHandshaker(uri,
//                                              WebSocketVersion.V13, null, false, new DefaultHttpHeaders())));
//                            p.addLast(new WsHandler(_this));
                            p.addLast(new IdleStateHandler(0, 0, 30*60)); //30min
                            p.addLast(wsh);

                        }else p.addLast(new HttpHandler());
                    }
                });

        /*if(!isWS) //if not websocket, we just connect
           connect();*/
    }

    public void connect(){
        logger.debug("Attempting to connect ...");
        try {

            ChannelFuture f = client.connect(host, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture cf) throws Exception {
                    if (!cf.isSuccess()) {
                        if (cf.cause() != null) {
                            logger.error("{}", cf.cause().getMessage());
                        }
                    }
                }
            });
            clientChanenel = f.sync().channel();

            // Now we are sure the future is completed.
            if (f.isDone())
                logger.debug(clientChanenel.isActive()?"successfully connected!":"not connected");

            if (isWS)
                wsh.handshakeFuture().sync();

            ebo.reset();
        } catch (Exception e) {
            String msg = e.getMessage();
            if(msg!=null && !msg.startsWith("Connection refused"))
                logger.error("Failed to connect to server {}", msg);
        }
    }

    //FIXME: need to close the pipeline e.g. for ws -> ch.write(new CloseWebSocketFrame());
    public void disconnect() throws Exception{
        if(!clientGroup.isShuttingDown() && !clientGroup.isShutdown())
            clientGroup.shutdownGracefully();
    }

    private SslContext sslCtx(){

        try {
            SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
            SslContextBuilder sslCtxBuilder =  SslContextBuilder.forClient().sslProvider(provider);

            /*String cafile = CONF.getProperty("cafile");
            if(cafile!=null)
                sslCtxBuilder.trustManager(new File(cafile));*/
            sslCtxBuilder.trustManager(getTrustManagerFactory());

            return sslCtxBuilder.build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setAppId(String appId) {
        this.appId = appId;
    }


    //for extensions
    @Override
    public void setToken(String token) throws Exception {
        // java.util.Base64 is not supported in Android API level below 26.
        // https://developer.android.com/reference/java/util/Base64
        //FIXME:  + and /?
        this.token =  token.split("\\.").length >= 3?"Bearer "+ token
                :"Basic "+new String(Base64.encodeBase64((":" + token).getBytes()), Charset.forName("US-ASCII"));
    }

    /**
     * @param msg
     * @return
     * @throws Exception
     */
    @Override
    public Object send(Message msg) throws Exception {
        //if(msg.getCode() == READ && msg.getPayload() != null) throw new IllegalArgumentException("empty payload");
        //if(msg.getCode() < 0) throw new IllegalArgumentException("invalid message code: "+ msg.getCode());

        if(clientChanenel==null || !clientChanenel.isOpen()) {
            logger.info("Attempting to connect ...");
            connect();
        }
        if(clientChanenel==null || !clientChanenel.isOpen()) throw new IllegalStateException("Connecting to server is refused");

        if(isWS) {

            final String uri = msg.getUri().startsWith("/w")? msg.getUri().substring(2):msg.getUri();

            Json pl = new Json(URI,  uri, MSG, msg.getPayload());
            /*if(jwt.equals("Basic")) pl.put("tkn",  "Basic "+ toBase64String((":"+token).getBytes()));
            else pl.put("tkn",  "Bearer "+ token);*/

            clientChanenel.writeAndFlush(new TextWebSocketFrame(pl.toJson())).addListener(futureListener());
            return true;
        }

        //FIXME: handle ACK & NAK.
        HttpMethod method = code(msg.getCode());
        //if(method == null) return false;

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, msg.getUri());
        HttpHeaders hds = request.headers();
        hds.set(HttpHeaderNames.HOST, host);
        hds.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        hds.set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
        hds.set(HttpHeaderNames.AUTHORIZATION, token);
        //Content-Encoding: deflate
        //Accept-Encoding: deflate, gzip
        if(msg.getPayload()!=null) {
            byte [] payload = msg.getPayload().toString().getBytes();

            hds.set(HttpHeaderNames.CONTENT_LENGTH, payload.length);
            request.content().writeBytes(payload);
        }
//        request.content().writeBytes(Unpooled.copiedBuffer(msg.getPayload()));

        logger.debug("sending json to: {}, msg: {}", msg.getUri(), msg.getPayload());
        ChannelFuture cf = clientChanenel.writeAndFlush(request);
        cf.addListener(futureListener());

        return true;
    }

    @ChannelHandler.Sharable
    private class WsHandler extends SimpleChannelInboundHandler<Object> {

        private WebSocketClientHandshaker handshaker;
        private ChannelPromise handshakeFuture;
        Http parent;

        public WsHandler(WebSocketClientHandshaker handshaker, Http parent) {
            this.handshaker = handshaker;
            this.parent = parent;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                Message recvmsg = new Message(Message.TYPE.SSM);
                try {
                    handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                    logger.debug("connection success!");
                    handshakeFuture.setSuccess();
                    try{
                        recvmsg.setCode(Message.ACK);
                        recv(recvmsg);
                    } catch (Exception e) {
                        logger.error("subscribing: ", e);
                        recvmsg.setCode(Message.UNKNOWN);
                        try {
                            recv(recvmsg);
                        } catch (Exception e1) {
                            logger.error("Unexpected handling of NAK msg", e1);
                        }
                    }
                } catch (WebSocketHandshakeException e) {
                    logger.warn("connection failed!");
                    handshakeFuture.setFailure(e);
                    recvmsg.setCode(Message.UNKNOWN);
                    try {
                        recv(recvmsg);
                    } catch (Exception e1) {
                        logger.error("Unexpected handling of NAK msg", e1);
                    }
                }
                return;
            }

            if (msg instanceof FullHttpResponse) {
                FullHttpResponse response = (FullHttpResponse) msg;
                throw new IllegalStateException("Unexpected (status=" + response.status() +
                        ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
            }

            if (msg instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;

                try {

                    Message recvmsg = new Message(Message.TYPE.SSM);


                    //full response?
                    final String pl = textFrame.text();
                    int contentLength= pl!=null?pl.length():0;
                    if(contentLength > 0) {
                        Json payload = Json.fromJson(pl);

                        final Long op = payload.getLong(CODE);
                        final String err = payload.getString(ERR);
                        if (op != null) {
                            recvmsg.setCode((byte) (long) op);
                        }else if(payload.size() == 0) {
                            recvmsg.setCode(Message.ACK);
                        }else if (err != null)
                            recvmsg.setCode(Message.UNKNOWN);

                        recvmsg.setMid(payload.getInt(MID));
                        recvmsg.setUri(payload.getString(URI));
                        //with websocket we have only json as media type
                        recvmsg.setPayload((err==null)?payload.getJson(MSG) : new Json("err", err));
                    }
                    //TODO for ACK we need to verify MID
                    Integer mId = recvmsg.getMid();

                    byte code = recvmsg.getCode();
                    logger.debug("received msg code: {}, msg: {} ",
                            Message.str(code),
                            (contentLength > 0? recvmsg.toString():""));

                    recv(recvmsg);
                }catch (IllegalArgumentException e){
                    logger.info("", e);
                    nak(e.getMessage());
                }catch (Throwable t){
                    logger.error("unexpected", t);
                    nak(t.getMessage());
                }
            } else if (msg instanceof PongWebSocketFrame) {
                logger.debug("WebSocket Client received pong");
            } else if (msg instanceof CloseWebSocketFrame) {
                logger.warn("Server has closed the connection!");
                //TODO: should we re-initiate the session?
                ch.close();
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.ALL_IDLE) {
                    ctx.writeAndFlush(new PingWebSocketFrame());
                }
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
            if(ebo.shouldRetry()) {
                logger.warn("Failed to connect to server, retrying ...");

                ctx.channel().eventLoop().schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ctx.channel().close();
                            //FIXME: this may create multiple sockets/threads?
                            parent.init();
                        } catch (Exception e) {}
                    }
                }, 0L, TimeUnit.SECONDS);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!handshakeFuture.isDone()) {
                handshakeFuture.setFailure(cause);
            }
            if((cause instanceof IOException)
                    && cause.getMessage().matches(".*connection.*closed.*remote\\shost")){
                logger.info("Connection was forcibly closed by server!");
            }else {
                if ((cause instanceof IOException)
                        && cause.getMessage().matches(".*connection.*closed.*remote\\shost")) {
                    logger.info("Connection was forcibly closed by server!");
                } else
                    logger.warn("", cause);
            }

            ctx.close();
        }

        public ChannelFuture handshakeFuture() {
            return handshakeFuture;
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            handshakeFuture = ctx.newPromise();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            handshaker.handshake(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.debug("Channel Inactive!");
        }
    }

    private class HttpHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        @Override
        public void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {

            try {

                Message recvmsg = new Message(Message.TYPE.SSM);

                //logger.debug("{} {}", response.protocolVersion().toString(), response.status());

                String cnt = null;
                byte code = UNDEFINED;
                if (!response.headers().isEmpty()) {
                    code = valueOf(response.status().code());
                    recvmsg.setCode(code);//(CLIENT_ERROR.contains(code) || SERVER_ERROR.contains(code))?Message.NAK:Message.ACK
                    if(Message.NO_CONTENT != code)
                        cnt = response.headers().get("Content-Type");
                }

                //full response?
                ByteBuf content = response.content();
                int contentLength= 0;
                if (content.isReadable()) {
                    contentLength = content.readableBytes();
                    byte[] body = null;
                    if(contentLength != 0) {
                        //TODO: accepts only json responses.
                        if(!appjson(cnt)) {
                            logger.error("Invalid response media type: {} - for payload: {}", cnt, new String(body));
                            nak("Invalid response media type");
                            return;
                        }
                        body = new byte[contentLength];
                        content.readBytes(body);
                        recvmsg.setPayload(Json.fromJson(body));
                    }
                }

                logger.debug("received msg code: {}, msg: {} ",
                        Message.str(code),
                        (contentLength > 0? recvmsg.toString():""));

                recv(recvmsg);
            }catch (IllegalArgumentException e){
                logger.info("", e);
                nak(e.getMessage());
            }catch (Throwable t){
                logger.error("unexpected", t);
                nak(t.getMessage());
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if((cause instanceof IOException)
                    && cause.getMessage().matches(".*connection.*closed.*remote\\shost")){
                logger.info("Connection was forcibly closed by server!");
            }else{
                if((cause instanceof IOException)
                        && cause.getMessage().matches(".*connection.*closed.*remote\\shost")){
                    logger.info("Connection was forcibly closed by server!");
                }else
                    logger.warn("", cause);
            }

            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            bctx = ctx;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.debug("Channel Inactive!");
        }
    }

    //NoP for now, we must let the handler make the decision about this!
    private void nak(String smsg) throws Exception {
        Message msg = new Message();
        msg.setPayload( new Json("err", smsg));
        msg.setCode(Message.BAD_REQUEST);
//            send(msg);
    }

    private String mainType = "application/json";
    private String str = mainType + "; charset=utf-8";

    private boolean appjson(String type) {
        return !(type == null || type.isEmpty()) && type.startsWith(MediaType.JSON);
/*String [] prm = type.split(";");
        if(prm.length == 0) return false;
        if(prm.length == 1) return mainType.equals(type);
        if(!mainType.equals(prm[0])) return false;
        return str.equals(type);*/
    }

    private final ChannelFutureListener futureListener() {
        return future -> {
            if (future.isSuccess()) {
                logger.debug("success sending request");
            } else {
                Throwable cause = future.cause();
                if(cause instanceof IOException){
                    String msg = cause.getMessage();
                    if(msg !=null && msg.matches(".*connection.*closed.*remote\\shost"))
                        logger.info("Connection was forcibly closed by server!");
                    else logger.info(msg);
                }else
                    logger.warn("", cause);
    //                future.channel().close();
            }
        };
    }

    private boolean isJWT(String token) throws IOException {
        String[] parts = token.split("\\.");
        return parts.length >= 3;
    }

    //TODO: for responding the server/cloud, as we make a POST, status is not reported as a HTTP response
    HttpMethod code(byte code){

        switch (code){
            case READ: return HttpMethod.GET;
            case Message.WRITE:
            case Message.EXEC: return HttpMethod.POST;
            case Message.UPDATE: return HttpMethod.PUT;
            case Message.DELETE: return HttpMethod.DELETE;

            case Message.ACK: return HttpMethod.POST;
        }
        throw new IllegalArgumentException("invalid code");
    }

    //mapping from HTTP status to Message code
    byte valueOf(int status){

        switch (status){
            /** Informational 1xx */
            case 100: return Message.CONTINUE;
            case 102: return Message.PROCESSING;

            /** Successful 2xx */
            case 200: return Message.ACK;
            case 201: return Message.CREATED;
            case 202: return Message.ACCEPTED;
            case 204: return Message.CHANGED;
            case 205:
            case 206: return Message.ACK;

            /** Redirection 3xx */
            case 300:
            case 301:
            case 302:
            case 303:
            case 304:
            case 305:
            case 307: return Message.NOT_CHANGED;

            /** Client Error 4xx */
            case 400: return Message.BAD_REQUEST;
            case 401: return Message.UNAUTHORIZED;
            case 403: return Message.FORBIDDEN;
            case 404: return Message.NOT_FOUND;
            case 405: return Message.FORBIDDEN;
            case 406: return Message.BAD_REQUEST;

            case 408: return Message.REQUEST_TIMEOUT;
            case 409: return Message.CONFLICT;
            case 410: return Message.GONE;
            case 411: return Message.INVALID_CONTENT_SIZE;
            case 412: return Message.PRECONDITION_FAILED;
            case 413: return Message.PAYLOAD_TOO_LARGE;
            case 415: return Message.UNSUPPORTED_MEDIA_TYPE;

            case 407:
            case 414:
            case 416:
            case 417:
            case 426: return Message.UNKNOWN;
            case 428: return Message.BAD_REQUEST;
            case 429: return Message.TOO_MANY_REQUESTS;
            case 431: return Message.PAYLOAD_TOO_LARGE;

            /** Server Error 5xx */
            case 500:
            case 502:
            case 503:
            case 505: return Message.SERVICE_UNAVAILABLE;

            case 504: return Message.GATEWAY_TIMEOUT;
            case 501: return Message.FORBIDDEN;
            case 520: return Message.UNKNOWN;
        }

        return Message.UNKNOWN;
    }
}
