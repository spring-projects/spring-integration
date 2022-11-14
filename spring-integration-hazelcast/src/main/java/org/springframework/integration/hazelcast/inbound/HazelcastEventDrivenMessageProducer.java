/*
 * Copyright 2015-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.hazelcast.inbound;

import java.util.HashMap;
import java.util.Map;

import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemEvent;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.MapListener;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import org.springframework.integration.hazelcast.HazelcastHeaders;
import org.springframework.integration.hazelcast.HazelcastIntegrationDefinitionValidator;
import org.springframework.util.Assert;

/**
 * Hazelcast Event Driven Message Producer is a message producer which enables
 * {@link AbstractHazelcastMessageProducer.HazelcastEntryListener},
 * {@link HazelcastEventDrivenMessageProducer.HazelcastItemListener} and
 * {@link HazelcastEventDrivenMessageProducer.HazelcastMessageListener} listeners in order
 * to listen related cache events and sends events to related channel.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastEventDrivenMessageProducer extends AbstractHazelcastMessageProducer {

	public HazelcastEventDrivenMessageProducer(DistributedObject distributedObject) {
		super(distributedObject);
	}

	@Override
	protected void onInit() {
		super.onInit();
		HazelcastIntegrationDefinitionValidator.validateCacheTypeForEventDrivenMessageProducer(this.distributedObject);
	}

	@Override
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void doStart() {
		if (this.distributedObject instanceof IMap) {
			setHazelcastRegisteredEventListenerId(((IMap<?, ?>) this.distributedObject)
					.addEntryListener((MapListener) new HazelcastEntryListener(), true));
		}
		else if (this.distributedObject instanceof MultiMap) {
			setHazelcastRegisteredEventListenerId(((MultiMap<?, ?>) this.distributedObject)
					.addEntryListener(new HazelcastEntryListener(), true));
		}
		else if (this.distributedObject instanceof ReplicatedMap) {
			setHazelcastRegisteredEventListenerId(((ReplicatedMap<?, ?>) this.distributedObject)
					.addEntryListener(new HazelcastEntryListener()));
		}
		else if (this.distributedObject instanceof IList) {
			setHazelcastRegisteredEventListenerId(((IList<?>) this.distributedObject)
					.addItemListener(new HazelcastItemListener(), true));
		}
		else if (this.distributedObject instanceof ISet) {
			setHazelcastRegisteredEventListenerId(((ISet<?>) this.distributedObject)
					.addItemListener(new HazelcastItemListener(), true));
		}
		else if (this.distributedObject instanceof IQueue) {
			setHazelcastRegisteredEventListenerId(((IQueue<?>) this.distributedObject)
					.addItemListener(new HazelcastItemListener(), true));
		}
		else if (this.distributedObject instanceof ITopic) {
			setHazelcastRegisteredEventListenerId(((ITopic<?>) this.distributedObject)
					.addMessageListener(new HazelcastMessageListener()));
		}
	}

	@Override
	protected void doStop() {
		if (this.distributedObject instanceof IMap) {
			((IMap<?, ?>) this.distributedObject).removeEntryListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof MultiMap) {
			((MultiMap<?, ?>) this.distributedObject).removeEntryListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof ReplicatedMap) {
			((ReplicatedMap<?, ?>) this.distributedObject).removeEntryListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof IList) {
			((IList<?>) this.distributedObject).removeItemListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof ISet) {
			((ISet<?>) this.distributedObject).removeItemListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof IQueue) {
			((IQueue<?>) this.distributedObject).removeItemListener(getHazelcastRegisteredEventListenerId());
		}
		else if (this.distributedObject instanceof ITopic) {
			((ITopic<?>) this.distributedObject).removeMessageListener(getHazelcastRegisteredEventListenerId());
		}
	}

	@Override
	public String getComponentType() {
		return "hazelcast:inbound-channel-adapter";
	}

	private class HazelcastItemListener<E> extends AbstractHazelcastEventListener<ItemEvent<E>>
			implements ItemListener<E> {

		@Override
		public void itemAdded(ItemEvent<E> item) {
			processEvent(item);
		}

		@Override
		public void itemRemoved(ItemEvent<E> item) {
			processEvent(item);
		}

		@Override
		protected void processEvent(ItemEvent<E> event) {
			if (getCacheEvents().contains(event.getEventType().toString())) {
				sendMessage(event,
						event.getMember().getSocketAddress(EndpointQualifier.MEMBER), getCacheListeningPolicy());
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Received ItemEvent : " + event);
			}
		}

		@Override
		protected org.springframework.messaging.Message<?> toMessage(ItemEvent<E> event) {
			final Map<String, Object> headers = new HashMap<>();
			headers.put(HazelcastHeaders.EVENT_TYPE, event.getEventType().name());
			headers.put(HazelcastHeaders.MEMBER, event.getMember().getSocketAddress(EndpointQualifier.MEMBER));

			return getMessageBuilderFactory().withPayload(event.getItem()).copyHeaders(headers).build();
		}

	}

	private class HazelcastMessageListener<E> extends AbstractHazelcastEventListener<Message<E>>
			implements MessageListener<E> {

		@Override
		public void onMessage(Message<E> message) {
			processEvent(message);
		}

		@Override
		protected void processEvent(Message<E> event) {
			sendMessage(event,
					event.getPublishingMember().getSocketAddress(EndpointQualifier.MEMBER), getCacheListeningPolicy());

			if (logger.isDebugEnabled()) {
				logger.debug("Received Message : " + event);
			}
		}

		@Override
		protected org.springframework.messaging.Message<?> toMessage(Message<E> event) {
			Assert.notNull(event.getMessageObject(), "message must not be null");

			final Map<String, Object> headers = new HashMap<>();
			headers.put(HazelcastHeaders.MEMBER,
					event.getPublishingMember().getSocketAddress(EndpointQualifier.MEMBER));
			headers.put(HazelcastHeaders.CACHE_NAME, event.getSource());
			headers.put(HazelcastHeaders.PUBLISHING_TIME, event.getPublishTime());

			return getMessageBuilderFactory().withPayload(event.getMessageObject()).copyHeaders(headers).build();
		}

	}

}
