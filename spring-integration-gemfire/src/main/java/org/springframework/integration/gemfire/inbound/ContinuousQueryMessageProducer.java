/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.gemfire.inbound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gemstone.gemfire.cache.query.CqEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.data.gemfire.listener.ContinuousQueryDefinition;
import org.springframework.data.gemfire.listener.ContinuousQueryListener;
import org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer;
import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Responds to a Gemfire continuous query (set using the #query field) that is
 * constantly evaluated against a cache
 * {@link com.gemstone.gemfire.cache.Region}. This is much faster than
 * re-querying the cache manually.
 *
 * @author Josh Long
 * @author David Turanski
 * @since 2.1
 *
 */
public class ContinuousQueryMessageProducer extends ExpressionMessageProducerSupport
		implements ContinuousQueryListener {

	private static Log logger = LogFactory.getLog(ContinuousQueryMessageProducer.class);

	private final String query;

	private final ContinuousQueryListenerContainer queryListenerContainer;

	private volatile String queryName;

	private boolean durable;

	private volatile Set<CqEventType> supportedEventTypes = new HashSet<CqEventType>(Arrays.asList(CqEventType.CREATED,
			CqEventType.UPDATED));

	/**
	 *
	 * @param queryListenerContainer a {@link org.springframework.data.gemfire.listener.ContinuousQueryListenerContainer}
	 * @param query the query string
	 */
	public ContinuousQueryMessageProducer(ContinuousQueryListenerContainer queryListenerContainer, String query) {
		Assert.notNull(queryListenerContainer, "'queryListenerContainer' cannot be null");
		Assert.notNull(query, "'query' cannot be null");
		this.queryListenerContainer = queryListenerContainer;
		this.query = query;
	}

	/**
	 *
	 * @param queryName optional query name
	 */
	public void setQueryName(String queryName) {
		this.queryName = queryName;
	}

	/**
	 *
	 * @param durable true if the query is a durable subscription
	 */
	public void setDurable(boolean durable) {
		this.durable = durable;
	}

	public void setSupportedEventTypes(CqEventType... eventTypes) {
		Assert.notEmpty(eventTypes, "eventTypes must not be empty");
		this.supportedEventTypes = new HashSet<CqEventType>(Arrays.asList(eventTypes));
	}

	@Override
	public String getComponentType() {
		return "gemfire:cq-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (queryName == null) {
			queryListenerContainer.addListener(new ContinuousQueryDefinition(this.query, this, this.durable));
		}
		else {
			queryListenerContainer.addListener(new ContinuousQueryDefinition(this.queryName, this.query, this,
					this.durable));
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.gemfire.listener.QueryListener#onEvent(com.gemstone
	 * .gemfire.cache.query.CqEvent)
	 */
	@Override
	public void onEvent(CqEvent event) {
		if (isEventSupported(event)) {
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("processing cq event key [%s] event [%s]", event.getQueryOperation()
						.toString(), event.getKey()));
			}
			Message<?> cqEventMessage = this.getMessageBuilderFactory().withPayload(evaluatePayloadExpression(event))
					.build();
			sendMessage(cqEventMessage);
		}
	}

	private boolean isEventSupported(CqEvent event) {

		String eventName = event.getQueryOperation().toString() +
				(event.getQueryOperation().toString().endsWith("Y") ? "ED" : "D");
		CqEventType eventType = CqEventType.valueOf(eventName);
		return supportedEventTypes.contains(eventType);
	}

}
