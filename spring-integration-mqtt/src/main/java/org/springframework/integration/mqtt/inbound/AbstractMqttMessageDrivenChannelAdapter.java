/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.mqtt.inbound;

import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.util.Assert;

/**
 * Abstract class for MQTT Message-Driven Channel Adapters.
 * @author Gary Russell
 * @since 1.0
 *
 */
public abstract class AbstractMqttMessageDrivenChannelAdapter extends MessageProducerSupport {

	private final String url;

	private final String clientId;

	private final String[] topic;

	private volatile MqttMessageConverter converter;

	public AbstractMqttMessageDrivenChannelAdapter(String url, String clientId, String... topic) {
		Assert.hasText(url, "'url' cannot be null or empty");
		Assert.hasText(clientId, "'clientId' cannot be null or empty");
		Assert.notNull(topic, "'topics' cannot be null");
		Assert.isTrue(topic.length > 0, "'topics' cannot be empty");
		Assert.noNullElements(topic, "'topics' cannot have null elements");
		this.url = url;
		this.clientId = clientId;
		this.topic = topic;
	}

	public void setConverter(MqttMessageConverter converter) {
		Assert.notNull(converter, "'converter' cannot be null");
		this.converter = converter;
	}

	protected String getUrl() {
		return url;
	}

	protected String getClientId() {
		return clientId;
	}

	protected MqttMessageConverter getConverter() {
		return converter;
	}

	protected String[] getTopic() {
		return topic;
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.converter == null) {
			this.converter = new DefaultPahoMessageConverter();
		}
	}

	@Override
	public String getComponentType(){
		return "mqtt:inbound-channel-adapter";
	}

}
