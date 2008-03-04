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

package org.springframework.integration.router.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.config.AbstractMessageHandlerCreator;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.AggregatorAdapter;

/**
 * Creates a {@link AggregatorAdapter AggregatorAdapter} adapter for methods that aggregate messages.
 *
 * @author Marius Bogoevici
 */
public class AggregatorMessageHandlerCreator extends AbstractMessageHandlerCreator {

	private static final String DEFAULT_REPLY_CHANNEL = "defaultReplyChannel";
	
	private static final String DISCARD_CHANNEL = "discardChannel";
	
	private static final String SEND_TIMEOUT = "sendTimeout";
	
	private static final String SEND_PARTIAL_RESULTS_ON_TIMEOUT = "sendPartialResultsOnTimeout";
	
	private static final String REAPER_INTERVAL = "reaperInterval";
	
	private static final String TIMEOUT = "timeout";
	
	private static final String TRACKED_CORRELATION_ID_CAPACITY = "trackedCorrelationIdCapacity";

	private final MessageBus messageBus;


	public AggregatorMessageHandlerCreator(MessageBus messageBus) {
		this.messageBus = messageBus;
	}

	public MessageHandler doCreateHandler(Object object, Method method, Map<String, ?> attributes) {
		AggregatingMessageHandler messageHandler = new AggregatingMessageHandler(new AggregatorAdapter(object, method));
		if (attributes.containsKey(DEFAULT_REPLY_CHANNEL))
			messageHandler.setDefaultReplyChannel(messageBus.lookupChannel((String)attributes.get(DEFAULT_REPLY_CHANNEL)));
		if (attributes.containsKey(DISCARD_CHANNEL))
			messageHandler.setDiscardChannel(messageBus.lookupChannel((String)attributes.get(DISCARD_CHANNEL)));
		if (attributes.containsKey(SEND_TIMEOUT))
			messageHandler.setSendTimeout((Long)attributes.get(SEND_TIMEOUT));
		if (attributes.containsKey(SEND_PARTIAL_RESULTS_ON_TIMEOUT))
			messageHandler.setSendPartialResultOnTimeout((Boolean)attributes.get(SEND_PARTIAL_RESULTS_ON_TIMEOUT));
		if (attributes.containsKey(REAPER_INTERVAL))
			messageHandler.setReaperInterval((Long)attributes.get(REAPER_INTERVAL));
		if(attributes.containsKey(TIMEOUT))
			messageHandler.setTimeout((Long)attributes.get(TIMEOUT));
		if(attributes.containsKey(TRACKED_CORRELATION_ID_CAPACITY))
			messageHandler.setTrackedCorrelationIdCapacity((Integer)attributes.get(TRACKED_CORRELATION_ID_CAPACITY));		
		return messageHandler;
	}
	
}