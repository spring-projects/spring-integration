/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jdbc.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServiceActivator {

	private static final Log log = LogFactory.getLog(ServiceActivator.class);

	private static Map<String, String> seen = new ConcurrentHashMap<String, String>();

	private final CountDownLatch latch;

	private int maxNumberOfMessages = 10;
	private int threadSleep = 10;

	public ServiceActivator(int maxNumberOfMessages, int threadSleep) {
		super();
		this.maxNumberOfMessages = maxNumberOfMessages;
		latch = new CountDownLatch(maxNumberOfMessages);
		this.threadSleep = threadSleep;
	}

	public ServiceActivator() {
		super();
		this.maxNumberOfMessages = 10;
		latch = new CountDownLatch(maxNumberOfMessages);
	}

	public void process(final String message) {
		log.info("Received: " + message);

		if (seen.containsKey(message)) {
			log.error("Already seen: " + message);
		} else {
			seen.put(message, message);
			log.info("Pre: " + message);

			if (this.threadSleep >0) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			log.info("Post: " + message + "; latch: " + latch.getCount());
			latch.countDown();
		}
	}

	public boolean await(int milliseconds) throws InterruptedException {
		return latch.await(milliseconds, TimeUnit.MILLISECONDS);
	}
}