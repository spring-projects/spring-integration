/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.store.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Gunnar Hillert
 *
 */
public class TestService {

	private static final Log log = LogFactory.getLog(TestService.class);

	private Map<String, String> seen = new ConcurrentHashMap<String, String>();

	private AtomicInteger duplicateMessagesCount = new AtomicInteger(0);

	private final CountDownLatch latch;

	private int maxNumberOfMessages = 10;

	private int threadSleep = 10;

	public TestService(int maxNumberOfMessages, int threadSleep) {
		super();
		this.maxNumberOfMessages = maxNumberOfMessages;
		latch = new CountDownLatch(maxNumberOfMessages);
		this.threadSleep = threadSleep;
	}

	public TestService() {
		super();
		this.maxNumberOfMessages = 10;
		latch = new CountDownLatch(maxNumberOfMessages);
	}

	public void process(final String message) {
		log.info("Received: " + message);

		if (seen.containsKey(message)) {
			log.error("Already seen: " + message);
			duplicateMessagesCount.addAndGet(1);
		}
		else {
			seen.put(message, message);
			log.info("Pre: " + message);

			if (this.threadSleep > 0) {
				try {
					Thread.sleep(10);
				}
				catch (InterruptedException e) {
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

	public Map<String, String> getSeenMessages() {
		return seen;
	}

	public int getDuplicateMessagesCount() {
		return duplicateMessagesCount.get();
	}

}
