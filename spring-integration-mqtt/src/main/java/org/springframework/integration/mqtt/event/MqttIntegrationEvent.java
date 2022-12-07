/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.mqtt.event;

import org.springframework.integration.events.IntegrationEvent;
import org.springframework.lang.Nullable;

/**
 * Base class for Mqtt Events. For {@link #getSourceAsType()}, you should use a subtype
 * of {@link org.springframework.integration.mqtt.core.MqttComponent} for the receiving
 * variable.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 */
@SuppressWarnings("serial")
public abstract class MqttIntegrationEvent extends IntegrationEvent {

	public MqttIntegrationEvent(Object source) {
		super(source);
	}

	public MqttIntegrationEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
