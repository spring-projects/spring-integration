/*
 * Copyright 2002-2017 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geode.cache.CacheClosedException;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.util.CacheListenerAdapter;

import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * An inbound endpoint that listens to a GemFire region for events and then publishes Messages to
 * a channel. The default supported event types are CREATED and UPDATED. See the {@link EventType}
 * enum for all options. A SpEL expression may be provided to generate a Message payload by
 * evaluating that expression against the {@link EntryEvent} instance as the root object. If no
 * payloadExpression is provided, the {@link EntryEvent} itself will be the payload.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.1
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class CacheListeningMessageProducer extends ExpressionMessageProducerSupport {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Region region;

	private final CacheListener<?, ?> listener;

	private volatile Set<EventType> supportedEventTypes =
			new HashSet<EventType>(Arrays.asList(EventType.CREATED, EventType.UPDATED));


	public CacheListeningMessageProducer(Region<?, ?> region) {
		Assert.notNull(region, "region must not be null");
		this.region = region;
		this.listener = new MessageProducingCacheListener();
	}


	public void setSupportedEventTypes(EventType... eventTypes) {
		Assert.notEmpty(eventTypes, "eventTypes must not be empty");
		this.supportedEventTypes = new HashSet<EventType>(Arrays.asList(eventTypes));
	}

	@Override
	public String getComponentType() {
		return "gemfire:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("adding MessageProducingCacheListener to GemFire Region '" + this.region.getName() + "'");
		}
		this.region.getAttributesMutator().addCacheListener(this.listener);
	}

	@Override
	protected void doStop() {
		if (this.logger.isInfoEnabled()) {
			this.logger.info("removing MessageProducingCacheListener from GemFire Region '" + this.region.getName() + "'");
		}
		try {
			this.region.getAttributesMutator().removeCacheListener(this.listener);
		}
		catch (CacheClosedException e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(e.getMessage(), e);
			}
		}

	}

	private class MessageProducingCacheListener extends CacheListenerAdapter {

		@Override
		public void afterCreate(EntryEvent event) {
			if (CacheListeningMessageProducer.this.supportedEventTypes.contains(EventType.CREATED)) {
				processEvent(event);
			}
		}

		@Override
		public void afterUpdate(EntryEvent event) {
			if (CacheListeningMessageProducer.this.supportedEventTypes.contains(EventType.UPDATED)) {
				processEvent(event);
			}
		}

		@Override
		public void afterInvalidate(EntryEvent event) {
			if (CacheListeningMessageProducer.this.supportedEventTypes.contains(EventType.INVALIDATED)) {
				processEvent(event);
			}
		}

		@Override
		public void afterDestroy(EntryEvent event) {
			if (CacheListeningMessageProducer.this.supportedEventTypes.contains(EventType.DESTROYED)) {
				processEvent(event);
			}
		}

		private void processEvent(EntryEvent event) {
			publish(evaluatePayloadExpression(event));

		}

		private void publish(Object object) {
			Message<?> message = null;
			if (object instanceof Message) {
				message = (Message<?>) object;
			}
			else {
				message = getMessageBuilderFactory().withPayload(object).build();
			}
			sendMessage(message);
		}
	}

}
