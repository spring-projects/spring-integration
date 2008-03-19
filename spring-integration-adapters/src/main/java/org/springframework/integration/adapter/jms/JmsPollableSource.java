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

package org.springframework.integration.adapter.jms;

import java.util.Arrays;
import java.util.Collection;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.adapter.PollableSource;
import org.springframework.jms.core.JmsTemplate;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenSourceAdapter} that uses Spring's MessageListener
 * container support is highly recommended.
 * 
 * @author Mark Fisher
 */
public class JmsPollableSource extends AbstractJmsTemplateBasedAdapter implements PollableSource<Object> {

	public JmsPollableSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

	public JmsPollableSource(ConnectionFactory connectionFactory, Destination destination) {
		super(connectionFactory, destination);
	}

	public JmsPollableSource(ConnectionFactory connectionFactory, String destinationName) {
		super(connectionFactory, destinationName);
	}

	public JmsPollableSource() {
		super();
	}


	public Collection<Object> poll(int limit) {
		return Arrays.asList(this.getJmsTemplate().receiveAndConvert());
	}

}
