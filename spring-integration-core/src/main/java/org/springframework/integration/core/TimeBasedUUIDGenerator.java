/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.core;

import java.util.UUID;

/**
 * Will generate time-based UUID (version 1 UUID).
 * This will allow Message ID to be unique but also contain an 
 * embedded timestamp which could be retrieved via UUID.timestamp()
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class TimeBasedUUIDGenerator {
	public static final Object lock = new Object();
	private static long lastTime;

	/**
	 * Will generate unique time based UUID where the next UUID is 
	 * always greater then the previous.
	 * 
	 * @return
	 */
	public final static UUID generateId() {
		return generateIdFromTimestamp(System.currentTimeMillis());
	}
	/**
	 * 
	 * @param currentTimeMillis
	 * @return
	 */
	public final static UUID generateIdFromTimestamp(long currentTimeMillis){
		long time;
		synchronized (lock) {
			if (currentTimeMillis > lastTime) {
				lastTime = currentTimeMillis;
			} else  {
				currentTimeMillis = ++lastTime;
			}
		}
		
		time = currentTimeMillis;
		
		// low Time
		time = currentTimeMillis << 32;
		
		// mid Time
		time |= (currentTimeMillis & 0xFFFF00000000L) >> 16;

		// hi Time
		time |= 0x1000 | ((currentTimeMillis >> 48) & 0x0FFF); // version 1

		return new UUID(time, System.nanoTime());
	}
}
