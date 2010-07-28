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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;

/**
 * This class implements all the strategy interfaces needed for a default resequencer.
 * 
 * @author Iwein Fuld
 * @author Dave Syer
 * 
 * @since 2.0
 */
public class ResequencingMessageGroupProcessor implements MessageGroupProcessor {

	private volatile Comparator<Message<?>> comparator = new SequenceNumberComparator();

	/**
	 * A comparator to use to order messages before processing. The default is to order by sequence number.
	 * 
	 * @param comparator the comparator to use to order messages
	 */
	public void setComparator(Comparator<Message<?>> comparator) {
		this.comparator = comparator;
	}

	public void processAndSend(MessageGroup group, MessagingTemplate messagingTemplate, MessageChannel outputChannel) {
		Collection<Message<?>> messages = group.getUnmarked();
		if (messages.size() > 0) {
			List<Message<?>> sorted = new ArrayList<Message<?>>(messages);
			Collections.sort(sorted, comparator);
			for (Message<?> message : sorted) {
				messagingTemplate.send(outputChannel, message);
			}
		}
	}

}
