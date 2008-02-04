/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.router;

import java.util.Comparator;

import org.springframework.integration.message.Message;

/**
 * A {@link Comparator} implementation based on the '<code>sequenceNumber</code>'
 * property of a {@link Message Message's} header.
 * 
 * @author Mark Fisher
 */
public class MessageSequenceComparator implements Comparator<Message<?>> {

	public int compare(Message<?> message1, Message<?> message2) {
		int s1 = message1.getHeader().getSequenceNumber();
		int s2 = message2.getHeader().getSequenceNumber();
		return (s1 < s2) ? -1 : (s1 == s2) ? 0 : 1;
	}

}
