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

/**
 * JMS MessageListener that converts a JMS Message into a Spring Integration
 * Message and sends that Message to a channel. If the 'expectReply' value is
 * <code>true</code>, it will also wait for a Spring Integration reply Message
 * and convert that into a JMS reply.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ChannelPublishingJmsMessageListener
		extends org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener {

}
