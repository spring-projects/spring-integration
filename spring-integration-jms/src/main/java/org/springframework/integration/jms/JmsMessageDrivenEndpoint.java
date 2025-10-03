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

import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * A message-driven endpoint that receive JMS messages, converts them into
 * Spring Integration Messages, and then sends the result to a channel.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsMessageDrivenEndpoint extends org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint {

	/**
	 * Construct an instance with an externally configured container.
	 * @param listenerContainer the container.
	 * @param listener the listener.
	 */
	public JmsMessageDrivenEndpoint(AbstractMessageListenerContainer listenerContainer,
			org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener listener) {

		super(listenerContainer, listener);
	}

}
