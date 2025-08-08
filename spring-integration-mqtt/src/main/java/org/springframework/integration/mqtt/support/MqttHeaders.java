/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.support;

/**
 * Spring Integration headers.
 *
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public final class MqttHeaders {

	public static final String PREFIX = "mqtt_";

	public static final String QOS = PREFIX + "qos";

	public static final String ID = PREFIX + "id";

	public static final String RECEIVED_QOS = PREFIX + "receivedQos";

	public static final String DUPLICATE = PREFIX + "duplicate";

	public static final String RETAINED = PREFIX + "retained";

	public static final String RECEIVED_RETAINED = PREFIX + "receivedRetained";

	public static final String TOPIC = PREFIX + "topic";

	public static final String RECEIVED_TOPIC = PREFIX + "receivedTopic";

	public static final String MESSAGE_EXPIRY_INTERVAL = PREFIX + "messageExpiryInterval";

	public static final String TOPIC_ALIAS = PREFIX + "topicAlias";

	public static final String RESPONSE_TOPIC = PREFIX + "responseTopic";

	public static final String CORRELATION_DATA = PREFIX + "correlationData";

	private MqttHeaders() {
		throw new AssertionError();
	}

}
