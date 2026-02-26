/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.util;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for converting various time representations to {@link Duration} objects.
 * Supports both ISO-8601 duration strings (e.g., "PT30S") and numeric values with
 * associated time units.
 *
 * TODO: There is probably a better place to put this, or is implemented somewhere else.  This code was lift and shifted
 * from PeriodicTriggerFactoryBean
 *
 * @author Glenn Renfro
 *
 * @since 7.1
 */
public abstract class DurationUtil {

	/**
	 * Convert a string value to a {@link Duration}.
	 * <p>
	 * If the value is an ISO-8601 duration string (starting with 'P' or 'p'),
	 * it will be parsed using {@link Duration#parse(CharSequence)}.
	 * Otherwise, the value is treated as a numeric string and converted to a
	 * {@link Duration} using the provided {@link TimeUnit}.
	 * @param value the string value to convert, either an ISO-8601 duration string or a numeric string
	 * @param timeUnit the time unit to use when interpreting numeric values
	 * @return a {@link Duration} representing the specified time period
	 */
	public static Duration toDuration(String value, TimeUnit timeUnit) {
		if (isDurationString(value)) {
			return Duration.parse(value);
		}
		return toDuration(Long.parseLong(value), timeUnit);
	}

	private static boolean isDurationString(String value) {
		return (value.length() > 1 && (isP(value.charAt(0)) || isP(value.charAt(1))));
	}

	private static Duration toDuration(long value, TimeUnit timeUnit) {
		return Duration.of(value, timeUnit.toChronoUnit());
	}

	private static boolean isP(char ch) {
		return (ch == 'P' || ch == 'p');
	}

	private DurationUtil() {

	}

}
