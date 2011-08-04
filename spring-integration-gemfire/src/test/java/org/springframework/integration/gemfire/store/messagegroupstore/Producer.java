/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gemfire.store.messagegroupstore;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Simple endpoint that we can use to send in a lot of test messages.
 *
 * @author Josh Long
 * @since 2.1
 */
@Component
public class Producer {

	private MessagingTemplate messagingTemplate = new MessagingTemplate();

	@Value("#{i}")
	private MessageChannel messageChannel;

	@Value("${correlation-header}")
	private String correlationHeader ;


	@PostConstruct
	public void start() throws Throwable {
		this.messagingTemplate.setDefaultChannel(this.messageChannel);
	}

	/**
	 * @param lines
	 * @throws Throwable
	 */
	public void sendManyMessages(int correlationValue, Collection<String> lines) throws Throwable {
		Assert.notNull( lines, "the collection must be non-null");
		Assert.notEmpty( lines, "the collection must not be empty");
		int ctr = 0;
		int size = lines.size() ;
		for (String l : lines) {
			Message<?> msg = MessageBuilder.withPayload(l)
					.setCorrelationId( this.correlationHeader)
				    .setHeader(this.correlationHeader , correlationValue)
					.setSequenceNumber(++ctr)
					.setSequenceSize(size)
					.build();
			this.messagingTemplate.send(msg);
		}
	}

}
