/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.endpoint;

import java.time.Duration;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.support.PeriodicTrigger;

/**
 * @author Jonas Partner
 * @author Gary Russell
 * @author Andreas Baer
 * @author Artem Bilan
 */
public class PollingEndpointStub extends AbstractPollingEndpoint {

	@Override
	protected Object getResourceToBind() {
		return this;
	}

	@Override
	protected String getResourceKey() {
		return "PollingEndpointStub";
	}

	@Override
	public String getComponentType() {
		return "polling-endpoint-stub";
	}

	@Override
	protected void onInit() {
		super.onInit();
		setTrigger(new PeriodicTrigger(Duration.ofMillis(500)));
	}

	@Override
	protected void handleMessage(Message<?> message) {
		throw new RuntimeException("intentional test failure");
	}

	@Override
	protected Message<?> receiveMessage() {
		return new GenericMessage<>("test message");
	}

}
