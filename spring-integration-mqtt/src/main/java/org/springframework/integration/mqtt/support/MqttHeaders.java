/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.mqtt.support;

/**
 * Spring Integration headers.
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public class MqttHeaders {

	private static final String prefix = "mqtt_";

	public static final String QOS = prefix + "qos";

	public static final String DUPLICATE = prefix + "duplicate";

	public static final String RETAINED = prefix + "retained";

	public static final String TOPIC = prefix + "topic";
	private MqttHeaders() {
		throw new AssertionError();
	}
}
