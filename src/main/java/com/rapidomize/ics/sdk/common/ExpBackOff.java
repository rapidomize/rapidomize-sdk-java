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

package com.rapidomize.ics.sdk.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;


/**
 * simple retry mechanism ;)
 */
public class ExpBackOff {
    private  final Logger logger = LoggerFactory.getLogger(ExpBackOff.class);
    protected static final Conf CONF = Conf.getInstance();

    final float FACTOR=1.3f;
    int retryCnt =0;
    int maxRetryCnt=CONF.getIntProperty("transport.retry.max");
    int strategy=CONF.getIntProperty("transport.retry.strategy");
    long MAX=60*1000L; //1 min
    long MIN=500L;//0.5 sec
    long delay=MIN;
    Random rnd = null;

    public ExpBackOff() {
    }

    public ExpBackOff(long max, long min) {
        this.MAX = max;
        this.MIN = min;
    }

    public ExpBackOff maxRetryCnt(int maxRetryCnt) {
        this.maxRetryCnt = maxRetryCnt;
        return this;
    }

    public ExpBackOff strategy(int strategy) {
        this.strategy = strategy;
        return this;
    }

    public ExpBackOff max(long max) {
        this.MAX = max;
        return this;
    }

    public ExpBackOff min(long min) {
        this.MIN = min;
        return this;
    }

    public void reset(){
        retryCnt=0;
        delay=MIN;
    }

    public long nextDelay(){
        retryCnt++;
        if(delay < MAX) {
            if(strategy == 0) //simple exponential backoff
                delay *= FACTOR;
            else { //add jitter
                long pow =  (long)Math.pow(2, retryCnt) + rnd.nextInt(800) + MIN;
                if(pow < 0) delay=MAX;
                else delay = Math.min(MAX, pow);
            }
        }else delay = MAX;
        if(maxRetryCnt > 0 && retryCnt > maxRetryCnt){
            reset();
        }

        return delay;
    }

    public boolean shouldRetry(){
        boolean ret=true;

        try {
            delay = nextDelay();
            if(retryCnt == 0) ret = false;//we have reset the retry, to either give up, for try again with higher frequency
            else Thread.sleep(delay);
        } catch (InterruptedException e) {
            //
        } finally {
            if(ret)
                logger.info("retrying with delay {}ms ...", delay);
        }
        return ret;
    }

}
