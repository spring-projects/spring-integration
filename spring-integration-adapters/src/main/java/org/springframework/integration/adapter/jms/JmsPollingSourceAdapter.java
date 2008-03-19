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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.adapter.PollingSourceAdapter;
import org.springframework.jms.core.JmsTemplate;

/**
 * A convenience adapter that wraps a {@link JmsPollableSource}.
 * 
 * @author Mark Fisher
 */
public class JmsPollingSourceAdapter extends PollingSourceAdapter<Object> {

	public JmsPollingSourceAdapter(JmsTemplate jmsTemplate) {
		super(new JmsPollableSource(jmsTemplate));
	}

	public JmsPollingSourceAdapter(ConnectionFactory connectionFactory, Destination destination) {
		super(new JmsPollableSource(connectionFactory, destination));
	}

	public JmsPollingSourceAdapter(ConnectionFactory connectionFactory, String destinationName) {
		super(new JmsPollableSource(connectionFactory, destinationName));
	}

}
