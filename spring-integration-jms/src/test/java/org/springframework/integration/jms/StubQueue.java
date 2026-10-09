/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.Queue;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class StubQueue implements Queue {

	private final String name;

	public StubQueue() {
		this.name = null;
	}

	public StubQueue(String name) {
		this.name = name;
	}

	public String getQueueName() {
		return this.name;
	}

}
