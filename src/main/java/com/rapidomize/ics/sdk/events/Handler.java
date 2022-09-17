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

package com.rapidomize.ics.sdk.events;

public interface Handler {

    String BASE_EP_PATH = "/api/v1/";
    String EP_PATH  = BASE_EP_PATH + "mo/";
    String ICAPP_EP_PATH  = BASE_EP_PATH + "icapp/";
    String AGW_PATH = BASE_EP_PATH+ "agw/";

    void inbound(Message msg) throws Exception;
    void outbound(byte code, Message msg) throws Exception;
}
