/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.support;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.IdGenerator;

/**
 * Alternative {@link IdGenerator} implementations.
 *
 * @author Andy Wilkinson
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public class IdGenerators {

	/**
	 * UUID.randomUUID().
	 *
	 */
	public static class JdkIdGenerator implements IdGenerator {

		@Override
		public UUID generateId() {
			return UUID.randomUUID();
		}

	}

	/**
	 * Based on the two {@link AtomicLong}s, for {@code topBits} and {@code bottomBits},
	 * respectively.
	 * Begins with {0, 1}; incremented on each use.
	 * <p>
	 * Note: after each {@code 2^63} generations, a duplicate {@link UUID}
	 * can be returned in a multi-threaded environment, if a second thread sees the old topBits
	 * before it is incremented by the thread detecting the bottomBits roll over.
	 * The duplicate would be from around the previous rollover and
	 * the chance of such a value still being in the system is exceedingly small.
	 * If your system might be impacted by this situation, you should choose another {@link IdGenerator}.
	 * Also note that there is no persistence, so this generator starts at {0, 1} each time the system
	 * is initialized. Therefore, it is not suitable when persisting messages based on their ID; it should
	 * only be used when the absolute best performance is required and messages are not persisted.
	 */
	public static class SimpleIncrementingIdGenerator implements IdGenerator {

		private final AtomicLong topBits = new AtomicLong();

		private final AtomicLong bottomBits = new AtomicLong();

		@Override
		public UUID generateId() {
			long lowerBits = this.bottomBits.incrementAndGet();
			if (lowerBits == 0) {
				return new UUID(this.topBits.incrementAndGet(), lowerBits);
			}
			else {
				return new UUID(this.topBits.get(), lowerBits);
			}
		}

	}

}
