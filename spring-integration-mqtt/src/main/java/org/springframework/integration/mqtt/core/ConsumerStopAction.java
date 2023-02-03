/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.mqtt.core;

/**
 * Action to take regarding subscriptions when consumer stops.
 *
 * @author Gary Russell
 *
 * @since 4.2.3
 *
 * @deprecated since 5.5.17
 * in favor of standard {@link org.eclipse.paho.client.mqttv3.MqttConnectOptions#setCleanSession(boolean)}.
 * Will be removed in 6.1.0.
 */
@Deprecated
public enum ConsumerStopAction {

	/**
	 * Never unsubscribe.
	 */
	UNSUBSCRIBE_NEVER,

	/**
	 * Always unsubscribe.
	 */
	UNSUBSCRIBE_ALWAYS,

	/**
	 * Unsubscribe if clean session is true.
	 */
	UNSUBSCRIBE_CLEAN

}
