/*
 * Copyright 2022 Orkes, Inc.
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
package io.orkes.conductor.id;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.logging.log4j.core.util.UuidUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.utils.IDGenerator;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnProperty(name = "conductor.id.generator", havingValue = "time_based")
@Slf4j
public class TimeBasedUUIDGenerator extends IDGenerator {

    private static final LocalDate JAN_1_2020 = LocalDate.of(2020, 1, 1);

    private static final int uuidLength = UUID.randomUUID().toString().length();

    private static Calendar uuidEpoch = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private static final long epochMillis;

    static {
        uuidEpoch.clear();
        uuidEpoch.set(1582, 9, 15, 0, 0, 0); //
        epochMillis = uuidEpoch.getTime().getTime();
    }

    public TimeBasedUUIDGenerator() {
        log.info("Using TimeBasedUUIDGenerator to generate Ids");
    }

    public String generate() {
        UUID uuid = UuidUtil.getTimeBasedUuid();
        return uuid.toString();
    }

    public static long getDate(String id) {
        UUID uuid = UUID.fromString(id);
        if (uuid.version() != 1) {
            return 0;
        }
        long time = (uuid.timestamp() / 10000L) + epochMillis;
        return time;
    }
}
