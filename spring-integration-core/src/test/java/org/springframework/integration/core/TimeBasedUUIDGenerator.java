/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.core;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 */
class TimeBasedUUIDGenerator {

	static final Lock lock = new ReentrantLock();

	private static final Logger logger = Logger.getLogger(TimeBasedUUIDGenerator.class.getName());

	private static final long macAddress = getMac();

	private static boolean canNotDetermineMac = true;

	private static long lastTime;

	private static long clockSequence = 0;

	private TimeBasedUUIDGenerator() {
		super();
	}

	/**
	 * Will generate unique time based UUID where the next UUID is
	 * always greater then the previous.
	 */
	static UUID generateId() {
		return generateIdFromTimestamp(System.currentTimeMillis());
	}

	static UUID generateIdFromTimestamp(long currentTimeMillis) {
		long time;

		lock.tryLock();
		try {
			if (currentTimeMillis > lastTime) {
				lastTime = currentTimeMillis;
				clockSequence = 0;
			}
			else {
				++clockSequence;
			}
		}
		finally {
			lock.unlock();
		}

		time = currentTimeMillis;

		// low Time
		time = currentTimeMillis << 32;

		// mid Time
		time |= ((currentTimeMillis & 0xFFFF00000000L) >> 16);

		// hi Time
		time |= 0x1000 | ((currentTimeMillis >> 48) & 0x0FFF); // version 1

		long clock_seq_hi_and_reserved = clockSequence;

		clock_seq_hi_and_reserved <<= 48;

		long cls = clock_seq_hi_and_reserved;

		long lsb = cls | macAddress;
		if (canNotDetermineMac) {
			logger.warning("UUID generation process was not able to determine your MAC address. " +
					"Returning random UUID (non version 1 UUID)");
			return UUID.randomUUID();
		}
		else {
			return new UUID(time, lsb);
		}
	}

	private static long getMac() {
		long macAddressAsLong = 0;
		try {
			InetAddress address = InetAddress.getLocalHost();
			NetworkInterface ni = NetworkInterface.getByInetAddress(address);
			if (ni != null) {
				//byte[] mac = ni.getHardwareAddress(); // availabe since Java 1.6
				byte[] mac = "01:23:45:67:89:ab".getBytes();
				//Converts array of unsigned bytes to an long
				for (byte aMac : mac) {
					macAddressAsLong <<= 8;
					macAddressAsLong ^= (long) aMac & 0xFF;
				}
			}
			canNotDetermineMac = false;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return macAddressAsLong;
	}

}
