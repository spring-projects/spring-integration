/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessagingException;

/**
 * A Channel Adapter implementation for connecting a
 * {@link org.springframework.integration.message.MessageSource}
 * to a {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class InboundChannelAdapter extends AbstractEndpoint {

	@Override
	protected boolean sendInternal(Message<?> message) {
		try {
			boolean sent = this.getMessageExchangeTemplate().send(message, this.getTarget());
			if (sent && this.getSource() instanceof MessageDeliveryAware) {
				((MessageDeliveryAware) this.getSource()).onSend(message);
			}
			return sent;
		}
		catch (Exception e) {
			if (this.getSource() instanceof MessageDeliveryAware) {
				((MessageDeliveryAware) this.getSource()).onFailure(message, e);
			}
			throw (e instanceof MessagingException) ? (MessagingException) e
					: new MessageDeliveryException(message, "channel adapter failed to send message to target", e);
		}
	}

}
