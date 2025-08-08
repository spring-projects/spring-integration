/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.mqtt.event;

/**
 * @author Gary Russell
 * @since 4.2.2
 *
 */
@SuppressWarnings("serial")
public class MqttSubscribedEvent extends MqttIntegrationEvent {

	private final String message;

	public MqttSubscribedEvent(Object source, String message) {
		super(source);
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}

	@Override
	public String toString() {
		return "MqttSubscribedEvent [message=" + this.message + ", source=" + source + "]";
	}

}
