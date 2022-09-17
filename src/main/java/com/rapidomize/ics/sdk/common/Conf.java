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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class Conf extends Properties{

    public Conf() {
        String conf = "client.properties";
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream(conf);
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream(conf);
                if (in == null) throw new IllegalArgumentException("Config file [" + conf + "] not found");
            }

            load(in);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Conf getInstance(){return new Conf();}

    public int getIntProperty(String prop) {
        return Integer.parseInt(getProperty(prop));
    }
    public int getIntProperty(String prop, String defaultValue) {
        return Integer.parseInt(getProperty(prop, defaultValue));
    }

    public long getLongProperty(String prop) {
        return Long.parseLong(getProperty(prop));
    }

    public long getLongProperty(String prop, String defaultValue) {
        return Long.parseLong(getProperty(prop, defaultValue));
    }

    public boolean getBooleanProperty(String prop) {
        return Boolean.parseBoolean(getProperty(prop));
    }

}
