/*
 * Copyright 2002-2020 the original author or authors.
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

	private MqttHeaders() {
		throw new AssertionError();
	}

}
