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

import org.springframework.integration.channel.MessagingTemplate;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.store.MessageGroup;

/**
 * A processor for <i>correlated</i> groups of messages.
 * 
 * @author Iwein Fuld
 * @see org.springframework.integration.aggregator.CorrelatingMessageHandler
 */
public interface MessageGroupProcessor {

	/**
	 * Process the given group and send the resulting message(s) to the output channel using the messaging template.
	 * Implementations are free to send as little or as many messages based on the invocation as needed. For example an
	 * aggregating processor will send only a single message representing the group, where a resequencing strategy will
	 * send all messages in the group individually.
	 */
	void processAndSend(MessageGroup group, MessagingTemplate messagingTemplate, MessageChannel outputChannel);

}
