/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.io.async;

import org.pragmatica.lang.Tuple.Tuple2;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.pragmatica.lang.Tuple.tuple;

/**
 * Representation of timeout value.
 */
//TODO rework as interface
public record Timeout(long timeout) {
    private static final long NANOS_IN_SECOND = 1_000_000_000L;

    public static TimeoutBuilder timeout(long value) {
        return new TimeoutBuilder(value);
    }

    public long milliseconds() {
        return TimeUnit.NANOSECONDS.toMillis(timeout());
    }

    public long nanoseconds() {
        return timeout();
    }

    public long microseconds() {
        return TimeUnit.NANOSECONDS.toMicros(timeout());
    }

    public Tuple2<Long, Integer> secondsAndNanos() {
        return tuple(timeout() / NANOS_IN_SECOND, (int) (timeout() % NANOS_IN_SECOND));
    }

    public Duration duration() {
        return secondsAndNanos().map(Duration::ofSeconds);
    }

    @Override
    public String toString() {
        return "Timeout(" + timeout + "ns)";
    }

    /**
     * Fluent interval conversion builder
     */
    public static final class TimeoutBuilder {
        private final long value;

        private TimeoutBuilder(long value) {
            this.value = value;
        }

        /**
         * Create {@link Timeout} instance by interpreting value as nanoseconds.
         *
         * @return Created instance
         */
        public Timeout nanos() {
            return new Timeout(value);
        }

        /**
         * Create {@link Timeout} instance by interpreting value as microseconds.
         *
         * @return Created instance
         */
        public Timeout micros() {
            return new Timeout(TimeUnit.MICROSECONDS.toNanos(value));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as milliseconds.
         *
         * @return Created instance
         */
        public Timeout millis() {
            return new Timeout(TimeUnit.MILLISECONDS.toNanos(value));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as seconds.
         *
         * @return Created instance
         */
        public Timeout seconds() {
            return new Timeout(TimeUnit.SECONDS.toNanos(value));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as minutes.
         *
         * @return Created instance
         */
        public Timeout minutes() {
            return new Timeout(TimeUnit.MINUTES.toNanos(value));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as hours.
         *
         * @return Created instance
         */
        public Timeout hours() {
            return new Timeout(TimeUnit.HOURS.toNanos(value));
        }

        /**
         * Create {@link Timeout} instance by interpreting value as days.
         *
         * @return Created instance
         */
        public Timeout days() {
            return new Timeout(TimeUnit.DAYS.toNanos(value));
        }
    }
}
