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

package org.springframework.integration.aggregator;

import java.io.Serializable;
import java.util.Comparator;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 * @author Gary Russell
 */
@SuppressWarnings("serial")
public class MessageSequenceComparator implements Comparator<Message<?>>, Serializable {

	@Override
	public int compare(Message<?> o1, Message<?> o2) {
		int sequenceNumber1 = new IntegrationMessageHeaderAccessor(o1).getSequenceNumber();
		int sequenceNumber2 = new IntegrationMessageHeaderAccessor(o2).getSequenceNumber();

		return Integer.compare(sequenceNumber1, sequenceNumber2);
	}

}
