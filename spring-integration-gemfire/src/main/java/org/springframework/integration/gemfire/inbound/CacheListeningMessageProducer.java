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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.endpoint.ExpressionMessageProducerSupport;
import org.springframework.util.Assert;

import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.CacheListener;
import com.gemstone.gemfire.cache.EntryEvent;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.util.CacheListenerAdapter;

/**
 * An inbound endpoint that listens to a GemFire region for events and then publishes Messages to
 * a channel. The default supported event types are CREATED and UPDATED. See the {@link EventType}
 * enum for all options. A SpEL expression may be provided to generate a Message payload by
 * evaluating that expression against the {@link EntryEvent} instance as the root object. If no
 * payloadExpression is provided, the {@link EntryEvent} itself will be the payload.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @since 2.1
 */
@SuppressWarnings({"rawtypes", "unchecked"})
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
		if (logger.isInfoEnabled()) {
			logger.info("adding MessageProducingCacheListener to GemFire Region '" + this.region.getName() + "'");
		}
		this.region.getAttributesMutator().addCacheListener(this.listener);
	}

	@Override
	protected void doStop() {
		if (logger.isInfoEnabled()) {
			logger.info("removing MessageProducingCacheListener from GemFire Region '" + this.region.getName() + "'");
		}
		try {
			this.region.getAttributesMutator().removeCacheListener(this.listener);
		} catch (CacheClosedException e) {
			if (logger.isDebugEnabled()){
				logger.debug(e.getMessage(),e);
			}
		}

	}

	private class MessageProducingCacheListener extends CacheListenerAdapter {

		@Override
		public void afterCreate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.CREATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterUpdate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.UPDATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterInvalidate(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.INVALIDATED)) {
				this.processEvent(event);
			}
		}

		@Override
		public void afterDestroy(EntryEvent event) {
			if (supportedEventTypes.contains(EventType.DESTROYED)) {
				this.processEvent(event);
			}
		}

		private void processEvent(EntryEvent event) {
				this.publish(evaluatePayloadExpression(event));

		}

		private void publish(Object payload) {
			sendMessage(CacheListeningMessageProducer.this.getMessageBuilderFactory().withPayload(payload).build());
		}
	}

}
