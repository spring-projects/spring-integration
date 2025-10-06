/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jms;

import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * An {@link org.springframework.integration.jms.channel.AbstractJmsChannel} implementation
 * for message-driven subscriptions.
 * Also implements a {@link BroadcastCapableChannel} to represent possible pub-sub semantics
 * when configured against JMS topic.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.channel.SubscribableJmsChannel}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class SubscribableJmsChannel extends org.springframework.integration.jms.channel.SubscribableJmsChannel {

	public SubscribableJmsChannel(AbstractMessageListenerContainer container, JmsTemplate jmsTemplate) {
		super(container, jmsTemplate);
	}

}
