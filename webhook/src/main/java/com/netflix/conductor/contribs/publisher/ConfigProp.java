/*
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.publisher;

import java.util.Optional;

public interface ConfigProp {

    /**
     * @param key Name of the property
     * @param defaultValue Default value when not specified
     * @return User defined integer property.
     */
    default int getIntProperty(String key, int defaultValue) {

        String val = getProperty(key, Integer.toString(defaultValue));
        try {
            defaultValue = Integer.parseInt(val);
        } catch (NumberFormatException ignored) {
        }
        return defaultValue;
    }

    /**
     * @param key Name of the property
     * @param defaultValue Default value when not specified
     * @return User defined string property.
     */
    default String getProperty(String key, String defaultValue) {

        String val;
        val = System.getenv(key.replace('.', '_'));
        if (val == null || val.isEmpty()) {
            val = Optional.ofNullable(System.getProperty(key)).orElse(defaultValue);
        }
        return val;
    }

    default boolean getBooleanProperty(String name, boolean defaultValue) {

        String val = getProperty(name, null);

        if (val != null) {
            return Boolean.parseBoolean(val);
        } else {
            return defaultValue;
        }
    }

    default boolean getBoolProperty(String name, boolean defaultValue) {
        String value = getProperty(name, null);
        if (null == value || value.trim().length() == 0) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
