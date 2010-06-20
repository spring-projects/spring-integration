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

import java.lang.management.ManagementFactory;
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
	private static boolean canNotDetermineMac = true;
	private static long lastTime;
	private static long processId = Long.valueOf(ManagementFactory.getRuntimeMXBean().getName().hashCode());
	private static long pidMac;
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
		pidMac = macAddressAsLong;
		if (pidMac == 0){
			pidMac = (processId << 32) | System.currentTimeMillis();
		} else {
			canNotDetermineMac = false;
			pidMac = (processId << 32) | macAddressAsLong;
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
		if (canNotDetermineMac){
			logger.warn("UUID generation process was not able to determine your MAC address. There is a slight chance for this UUID not to be globally unique");
		}
		
		return new UUID(time, pidMac);
	}
}
