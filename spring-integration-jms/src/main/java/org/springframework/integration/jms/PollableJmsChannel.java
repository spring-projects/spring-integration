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

import org.springframework.jms.core.JmsTemplate;

/**
 * A JMS-backed channel from which messages can be received through polling.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.channel.PollableJmsChannel}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class PollableJmsChannel extends org.springframework.integration.jms.channel.PollableJmsChannel {

	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
