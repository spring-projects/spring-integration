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

package org.springframework.integration.aggregator;

import java.util.Comparator;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;

/**
 * A {@link Comparator} implementation based on the 'sequence number'
 * property of a {@link Message Message's} header.
 *
 * @author Mark Fisher
 */
public class MessageSequenceComparator implements Comparator<Message<?>> {

	public int compare(Message<?> message1, Message<?> message2) {
		Integer s1 = new IntegrationMessageHeaderAccessor(message1).getSequenceNumber();
		Integer s2 = new IntegrationMessageHeaderAccessor(message2).getSequenceNumber();
		if (s1 == null) {
			s1 = 0;
		}
		if (s2 == null) {
			s2 = 0;
		}
		return s1.compareTo(s2);
	}

}
