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
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenEndpoint} that uses Spring's MessageListener container
 * support is a better option.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.JmsDestinationPollingSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsDestinationPollingSource
		extends org.springframework.integration.jms.inbound.JmsDestinationPollingSource {

	public JmsDestinationPollingSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
