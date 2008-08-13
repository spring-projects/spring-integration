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

package org.springframework.integration.splitter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHeaders;

/**
 * A {@link MessageHandler} implementation for splitting a single Message
 * into multiple reply Messages. If an object and method (or methodName)
 * pair are provided, the provided method will be invoked and its return
 * value will be split if it is a Collection or Array. If no object and
 * method are provided, this handler will split the Message payload
 * itself if it is a Collection or an Array. In either case, if the
 * Message payload or return value from a Method invocation is not a
 * Collection or Array, then the single Object will be returned as the
 * payload of a single reply Message.
 * 
 * @author Mark Fisher
 */
public class SplitterMessageHandler extends AbstractMessageHandler {

	private volatile String delimiters;


	public SplitterMessageHandler(Object object, Method method) {
		super(object, method);
	}

	public SplitterMessageHandler(Object object, String methodName) {
		super(object, methodName);
	}

	public SplitterMessageHandler() {
		super();
	}


	/**
	 * Set delimiters to use for tokenizing String values. The default
	 * is <code>null</code> indicating that no tokenization should occur.
	 * If delimiters are provided, they will be applied to any String
	 * payload, or if an Object and Method have been provided, tokenization
	 * will be applied to any String return value from the invoked Method.
	 */
	public void setDelimiters(String delimiters) {
		this.delimiters = delimiters;
	}

	protected CompositeMessage createReplyMessage(Object result, Message<?> requestMessage) {
		MessageHeaders requestHeaders = requestMessage.getHeaders();
		List<Message<?>> results = new ArrayList<Message<?>>();
		if (result instanceof Collection) {
			Collection<?> items = (Collection<?>) result;
			int sequenceNumber = 0;
			int sequenceSize = items.size();
			for (Object item : items) {
				results.add(this.createSplitMessage(item, requestHeaders, ++sequenceNumber, sequenceSize));
			}
		}
		else if (result.getClass().isArray()) {
			Object[] items = (Object[]) result;
			int sequenceNumber = 0;
			int sequenceSize = items.length;
			for (Object item : items) {
				results.add(this.createSplitMessage(item, requestHeaders, ++sequenceNumber, sequenceSize));
			}
		}
		else if (result instanceof String && this.delimiters != null) {
			StringTokenizer tokenizer = new StringTokenizer((String) result, this.delimiters);
			int sequenceNumber = 0;
			int sequenceSize = tokenizer.countTokens();
			while (tokenizer.hasMoreElements()) {
				results.add(this.createSplitMessage(
						tokenizer.nextToken(), requestHeaders, ++sequenceNumber, sequenceSize));
			}
		}
		else {
			results.add(this.createSplitMessage(result, requestHeaders, 1, 1));
		}
		if (results.isEmpty()) {
			return null;
		}
		return new CompositeMessage(results);
	}

	@Override
	protected Message<?> postProcessReplyMessage(Message<?> replyMessage, Message<?> requestMessage) {
		Object requestId = requestMessage.getHeaders().getId();
		if (replyMessage instanceof CompositeMessage) {
			List<Message<?>> sequentialMessages = new ArrayList<Message<?>>();
			List<Message<?>> replyList = ((CompositeMessage) replyMessage).getPayload();
			int sequenceSize = replyList.size();
			int sequenceNumber = 0;
			for (Message<?> message : replyList) {
				sequentialMessages.add(this.setSplitMessageHeaders(
						MessageBuilder.fromMessage(message), requestId, ++sequenceNumber, sequenceSize));
			}
			return new CompositeMessage(sequentialMessages);
		}
		return this.setSplitMessageHeaders(MessageBuilder.fromMessage(replyMessage), requestId, 1, 1);
	}

	private Message<?> createSplitMessage(Object item, MessageHeaders requestHeaders, int sequenceNumber, int sequenceSize) {
		if (item instanceof Message<?>) {
			return this.setSplitMessageHeaders(MessageBuilder.fromMessage((Message<?>) item),
					requestHeaders.getId(), sequenceNumber, sequenceSize);
		}
		return this.setSplitMessageHeaders(MessageBuilder.fromPayload(item),
				requestHeaders.getId(), sequenceNumber, sequenceSize);
	}

	private Message<?> setSplitMessageHeaders(MessageBuilder<?> builder, Object requestMessageId, int sequenceNumber, int sequenceSize) {
		return builder.setCorrelationId(requestMessageId)
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize).build();
	}

}
