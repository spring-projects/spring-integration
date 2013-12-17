/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.Comparator;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.Message;

/**
 * @author Dave Syer
 *
 * @since 2.0
 *
 */
public class SequenceNumberComparator implements Comparator<Message<?>> {

	/**
	 * If both messages have a sequence number then compare that, otherwise if one has a sequence number and the other
	 * doesn't then the numbered message comes first, or finally of neither has a sequence number then they are equal in
	 * rank.
	 */
	public int compare(Message<?> o1, Message<?> o2) {
		Integer sequenceNumber1 = new IntegrationMessageHeaderAccessor(o1).getSequenceNumber();
		Integer sequenceNumber2 = new IntegrationMessageHeaderAccessor(o2).getSequenceNumber();
		if (sequenceNumber1 == sequenceNumber2) {
			return 0;
		}
		if (sequenceNumber1 == null) {
			return -sequenceNumber2;
		}
		if (sequenceNumber2 == null) {
			return sequenceNumber1;
		}
		return sequenceNumber1.compareTo(sequenceNumber2);
	}

}
