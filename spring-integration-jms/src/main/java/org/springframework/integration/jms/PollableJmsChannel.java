/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.jms;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class PollableJmsChannel extends AbstractJmsChannel implements PollableChannel {

	private volatile String messageSelector;

	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public void setMessageSelector(String messageSelector) {
		this.messageSelector = messageSelector;
	}

	public Message<?> receive() {
		if (!this.getInterceptors().preReceive(this)) {
 			return null;
 		}
		Object object;
		if (this.messageSelector == null) {
			object = this.getJmsTemplate().receiveAndConvert();
		}
		else {
			object = this.getJmsTemplate().receiveSelectedAndConvert(this.messageSelector);
		}

		if (object == null) {
			return null;
		}
		Message<?> replyMessage = null;
		if (object instanceof Message<?>) {
			replyMessage = (Message<?>) object;
		}
		else {
			replyMessage = this.getMessageBuilderFactory().withPayload(object).build();
		}
		return this.getInterceptors().postReceive(replyMessage, this) ;
	}

	public Message<?> receive(long timeout) {
		try {
			DynamicJmsTemplateProperties.setReceiveTimeout(timeout);
			return this.receive();
		}
		finally {
			DynamicJmsTemplateProperties.clearReceiveTimeout();
		}
	}

}
