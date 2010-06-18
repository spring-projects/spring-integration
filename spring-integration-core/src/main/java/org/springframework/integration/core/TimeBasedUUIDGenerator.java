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

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
	private static final Log logger = LogFactory.getLog(TimeBasedUUIDGenerator.class);
	private static long  macAddressAsLong = 0;
	
	static {
		try {
			InetAddress address = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(address);
			if (ni != null) {
				byte[] mac = ni.getHardwareAddress();
				//Converts array of unsigned bytes to an long
				if (mac != null) {
					for (int i = 0; i < mac.length; i++) {					
						macAddressAsLong |= mac[i] & 0xFF;
						macAddressAsLong <<= 8;
					}
				}
			} 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
		long macNanoTime;
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
		
		macNanoTime = macAddressAsLong;
		if (macNanoTime == 0){
			logger.warn("Can not determine machine's MAC address. Will use System.nanoTime() for UUID generation, however there is a slim chance of it not being globally unique");
			macNanoTime = System.nanoTime();
		} else {
			//considering the type of time returned by nanoTime, this will ensure that 
			// even if more then one process is running on the same machine, the id is still unique
			macNanoTime |= System.nanoTime() & 0xFF;
			macNanoTime <<= 8;
		}
		return new UUID(time, macNanoTime);
	}
}
