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

package org.springframework.integration.jms;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageHandler;

/**
 * A MessageConsumer that sends the converted Message payload within
 * a JMS Message.
 * 
 * @author Mark Fisher
 */
public class JmsSendingMessageHandler extends AbstractJmsTemplateBasedAdapter implements MessageHandler {

	public final void handleMessage(final Message<?> message) {
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		this.getJmsTemplate().convertAndSend(message);
	}

}
