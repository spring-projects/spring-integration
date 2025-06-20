/*
 * Copyright 2020-present the original author or authors.
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

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * An extension of {@link MqttComponent} for Eclipse Paho components.
 *
 * @author Gary Russell
 * @since 5.4
 *
 */
public interface MqttPahoComponent extends MqttComponent<MqttConnectOptions> {

	@Override
	MqttConnectOptions getConnectionInfo();

}
