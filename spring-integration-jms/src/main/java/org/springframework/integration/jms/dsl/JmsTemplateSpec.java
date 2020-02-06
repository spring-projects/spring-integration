/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import org.springframework.integration.jms.DynamicJmsTemplate;
import org.springframework.jms.support.converter.MessageConverter;

/**
 * A {@link JmsDestinationAccessorSpec} for a {@link DynamicJmsTemplate}.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class JmsTemplateSpec extends JmsDestinationAccessorSpec<JmsTemplateSpec, DynamicJmsTemplate> {

	protected JmsTemplateSpec() {
		super(new DynamicJmsTemplate());
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setMessageConverter(MessageConverter)
	 */
	public JmsTemplateSpec jmsMessageConverter(MessageConverter messageConverter) {
		this.target.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param deliveryPersistent the deliveryPersistent.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setDeliveryPersistent(boolean)
	 */
	public JmsTemplateSpec deliveryPersistent(boolean deliveryPersistent) {
		this.target.setDeliveryPersistent(deliveryPersistent);
		return _this();
	}

	/**
	 * @param explicitQosEnabled the explicitQosEnabled.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setExplicitQosEnabled(boolean)
	 */
	public JmsTemplateSpec explicitQosEnabled(boolean explicitQosEnabled) {
		this.target.setExplicitQosEnabled(explicitQosEnabled);
		return _this();
	}

	/**
	 * May be overridden at run time by the message priority header.
	 * @param priority the priority.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setPriority(int)
	 */
	public JmsTemplateSpec priority(int priority) {
		this.target.setPriority(priority);
		return _this();
	}

	/**
	 * @param timeToLive the timeToLive.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setTimeToLive(long)
	 */
	public JmsTemplateSpec timeToLive(long timeToLive) {
		this.target.setTimeToLive(timeToLive);
		return _this();
	}

	/**
	 * @param receiveTimeout the receiveTimeout.
	 * @return the spec.
	 * @see org.springframework.jms.core.JmsTemplate#setReceiveTimeout(long)
	 */
	public JmsTemplateSpec receiveTimeout(long receiveTimeout) {
		this.target.setReceiveTimeout(receiveTimeout);
		return _this();
	}

}
