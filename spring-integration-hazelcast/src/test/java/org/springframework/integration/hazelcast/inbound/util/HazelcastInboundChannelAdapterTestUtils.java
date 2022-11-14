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

package org.springframework.integration.hazelcast.inbound.util;

import java.util.Collection;
import java.util.Map;

import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.collection.ICollection;
import com.hazelcast.core.DistributedObjectEvent;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.spi.exception.DistributedObjectDestroyedException;
import com.hazelcast.topic.ITopic;

import org.springframework.integration.hazelcast.HazelcastHeaders;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.message.EntryEventMessagePayload;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Util Class for Hazelcast Inbound Channel Adapters Test Support.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
@SuppressWarnings("unchecked")
public final class HazelcastInboundChannelAdapterTestUtils {

	public static final int TIMEOUT = 30_000;

	public static void verifyEntryEvent(Message<?> msg, String cacheName,
			EntryEventType event) {
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		if (event == EntryEventType.CLEAR_ALL || event == EntryEventType.EVICT_ALL) {
			assertThat(msg.getPayload() instanceof Integer).isTrue();
		}
		else {
			assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		}

		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo(cacheName);
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(event.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
	}

	public static void verifyItemEvent(Message<?> msg, EntryEventType event) {
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE).toString()).isEqualTo(event.toString());
	}

	public static void testEventDrivenForADDEDDistributedMapEntryEvent(
			final IMap<Integer, HazelcastIntegrationTestUser> distributedMap,
			final PollableChannel channel, final String cacheName) {
		HazelcastIntegrationTestUser hazelcastIntegrationTestUser =
				new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1");
		distributedMap.put(1, hazelcastIntegrationTestUser);
		Message<?> msg = channel.receive(TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.ADDED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo(cacheName);

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(1));
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getId()).isEqualTo(1);
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getName()).isEqualTo("TestName1");
		assertThat((((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).getSurname()).isEqualTo("TestSurname1");
	}

	public static void testEventDrivenForDistributedMapEntryEvents(
			final IMap<Integer, HazelcastIntegrationTestUser> distributedMap,
			final PollableChannel channel, final String cacheName) {
		distributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		distributedMap.put(1,
				new HazelcastIntegrationTestUser(1, "TestName1", "TestSurnameUpdated"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.UPDATED);

		distributedMap.remove(1);
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.REMOVED);

		distributedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		distributedMap.clear();
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.CLEAR_ALL);
	}

	public static void testEventDrivenForDistributedCollectionItemEvents(
			final ICollection<HazelcastIntegrationTestUser> distributedObject,
			final PollableChannel channel) {
		HazelcastIntegrationTestUser user =
				new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1");
		distributedObject.add(user);
		Message<?> msg = channel.receive(TIMEOUT);
		verifyItemEvent(msg, EntryEventType.ADDED);

		distributedObject.remove(user);
		msg = channel.receive(TIMEOUT);
		verifyItemEvent(msg, EntryEventType.REMOVED);

		user = new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2");
		distributedObject.add(user);
		msg = channel.receive(TIMEOUT);
		verifyItemEvent(msg, EntryEventType.ADDED);
	}

	public static void testEventDrivenForReplicatedMapEntryEvents(
			final ReplicatedMap<Integer, HazelcastIntegrationTestUser> replicatedMap,
			final PollableChannel channel, final String cacheName) {
		replicatedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		replicatedMap.put(1,
				new HazelcastIntegrationTestUser(1, "TestName1", "TestSurnameUpdated"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.UPDATED);

		replicatedMap.remove(1);
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.REMOVED);

		replicatedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);
	}

	public static void testEventDrivenForTopicMessageEvent(
			final ITopic<HazelcastIntegrationTestUser> topic, final PollableChannel channel) {
		topic.publish(new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg = channel.receive(TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.PUBLISHING_TIME)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo(topic.getName());
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getId()).isEqualTo(1);
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getName()).isEqualTo("TestName1");
		assertThat(((HazelcastIntegrationTestUser) msg.getPayload()).getSurname()).isEqualTo("TestSurname1");
	}

	public static void testEventDrivenForMultiMapEntryEvents(
			final MultiMap<Integer, HazelcastIntegrationTestUser> multiMap,
			final PollableChannel channel, final String cacheName) {
		multiMap.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		Message<?> msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		multiMap.put(1,
				new HazelcastIntegrationTestUser(1, "TestName1", "TestSurnameUpdated"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		multiMap.remove(1);
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.REMOVED);
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.REMOVED);

		multiMap.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.ADDED);

		multiMap.clear();
		msg = channel.receive(TIMEOUT);
		verifyEntryEvent(msg, cacheName, EntryEventType.CLEAR_ALL);
	}

	public static void testContinuousQueryForUPDATEDEntryEventWhenIncludeValueIsFalse(
			final IMap<Integer, HazelcastIntegrationTestUser> cqDistributedMap,
			final PollableChannel channel, final String cacheName) {
		cqDistributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1"));
		cqDistributedMap
				.put(1, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2"));
		Message<?> msg = channel.receive(TIMEOUT);
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof EntryEventMessagePayload).isTrue();
		assertThat(msg.getHeaders().get(HazelcastHeaders.MEMBER)).isNotNull();
		assertThat(msg.getHeaders().get(HazelcastHeaders.EVENT_TYPE)).isEqualTo(EntryEventType.UPDATED.name());
		assertThat(msg.getHeaders().get(HazelcastHeaders.CACHE_NAME)).isEqualTo(cacheName);

		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).key).isEqualTo(Integer.valueOf(1));
		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).oldValue).isNull();
		assertThat(((EntryEventMessagePayload<Integer, HazelcastIntegrationTestUser>) msg
				.getPayload()).value).isNull();
	}

	public static void testDistributedSQLForENTRYIterationType(
			final IMap<Integer, HazelcastIntegrationTestUser> dsDistributedMap,
			final PollableChannel channel) {
		dsDistributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1", 10));
		dsDistributedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2", 20));
		dsDistributedMap
				.put(3, new HazelcastIntegrationTestUser(3, "TestName3", "TestSurname3", 30));
		dsDistributedMap
				.put(4, new HazelcastIntegrationTestUser(4, "TestName4", "TestSurname4", 40));
		dsDistributedMap
				.put(5, new HazelcastIntegrationTestUser(5, "TestName5", "TestSurname5", 50));

		Message<?> msg = channel.receive(TIMEOUT);

		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof Collection).isTrue();
		assertThat((((Map.Entry<?, ?>) ((Collection<?>) msg.getPayload()).iterator().next())
				.getKey())).isEqualTo(4);
		assertThat(((HazelcastIntegrationTestUser) ((Map.Entry<?, ?>) ((Collection<?>) msg
				.getPayload()).iterator().next()).getValue()).getId()).isEqualTo(4);
		assertThat(((HazelcastIntegrationTestUser) ((Map.Entry<?, ?>) ((Collection<?>) msg
				.getPayload()).iterator().next()).getValue()).getName()).isEqualTo("TestName4");
		assertThat(((HazelcastIntegrationTestUser) ((Map.Entry<?, ?>) ((Collection<?>) msg
				.getPayload()).iterator().next()).getValue()).getSurname()).isEqualTo("TestSurname4");
	}

	public static void testDistributedSQLForKEYIterationType(
			final IMap<Integer, HazelcastIntegrationTestUser> dsDistributedMap,
			final PollableChannel channel) {
		dsDistributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1", 10));
		dsDistributedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2", 20));
		dsDistributedMap
				.put(3, new HazelcastIntegrationTestUser(3, "TestName3", "TestSurname3", 30));

		Message<?> msg = channel.receive(TIMEOUT);

		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof Collection).isTrue();
		assertThat(((Collection<?>) msg.getPayload()).iterator().next()).isEqualTo(1);
	}

	public static void testDistributedSQLForLOCAL_KEYIterationType(
			final IMap<Integer, HazelcastIntegrationTestUser> dsDistributedMap,
			final PollableChannel channel) {
		dsDistributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1", 10));
		dsDistributedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2", 20));
		dsDistributedMap
				.put(3, new HazelcastIntegrationTestUser(3, "TestName3", "TestSurname3", 30));

		Message<?> msg = channel.receive(TIMEOUT);

		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof Collection).isTrue();
	}

	public static void testDistributedSQLForVALUEIterationType(
			final IMap<Integer, HazelcastIntegrationTestUser> dsDistributedMap,
			final PollableChannel channel) {
		dsDistributedMap
				.put(1, new HazelcastIntegrationTestUser(1, "TestName1", "TestSurname1", 10));
		dsDistributedMap
				.put(2, new HazelcastIntegrationTestUser(2, "TestName2", "TestSurname2", 20));
		dsDistributedMap
				.put(3, new HazelcastIntegrationTestUser(3, "TestName3", "TestSurname3", 30));
		dsDistributedMap
				.put(4, new HazelcastIntegrationTestUser(4, "TestName4", "TestSurname4", 40));
		dsDistributedMap
				.put(5, new HazelcastIntegrationTestUser(5, "TestName5", "TestSurname5", 50));

		Message<?> msg = channel.receive(TIMEOUT);

		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof Collection).isTrue();
		assertThat(((HazelcastIntegrationTestUser) (((Collection<?>) msg.getPayload()).iterator()
				.next())).getId()).isEqualTo(3);
		assertThat(((HazelcastIntegrationTestUser) (((Collection<?>) msg.getPayload()).iterator()
				.next())).getName()).isEqualTo("TestName3");
		assertThat(((HazelcastIntegrationTestUser) (((Collection<?>) msg.getPayload()).iterator()
				.next())).getSurname()).isEqualTo("TestSurname3");
	}

	public static void testDistributedObjectEventByChannelAndHazelcastInstance(
			final PollableChannel channel, final HazelcastInstance hazelcastInstance,
			final String distributedObjectName) {
		final IMap<Integer, String> distributedMap =
				hazelcastInstance.getMap(distributedObjectName);

		Message<?> msg = channel.receive(TIMEOUT);
		verifyDistributedObjectEvent(msg, DistributedObjectEvent.EventType.CREATED,
				distributedObjectName);

		distributedMap.destroy();

		msg = channel.receive(TIMEOUT);
		try {
			// Since Hazelcast 3.6 we can use DistributedObjectEvent.getDistributedObject() for DESTROYED objects.
			verifyDistributedObjectEvent(msg, DistributedObjectEvent.EventType.DESTROYED, distributedObjectName);
			fail("DistributedObjectDestroyedException expected");
		}
		catch (Exception e) {
			assertThat(e).isInstanceOf(DistributedObjectDestroyedException.class);
		}
	}

	private static void verifyMembershipEvent(final Message<?> msg,
			final int membershipEvent) {
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof MembershipEvent).isTrue();
		assertThat(((MembershipEvent) msg.getPayload()).getEventType()).isEqualTo(membershipEvent);
		assertThat(((MembershipEvent) msg.getPayload()).getMember()).isNotNull();
	}

	private static void verifyDistributedObjectEvent(final Message<?> msg,
			final DistributedObjectEvent.EventType eventType,
			final String distributedObjectName) {
		assertThat(msg).isNotNull();
		assertThat(msg.getPayload()).isNotNull();
		assertThat(msg.getPayload() instanceof DistributedObjectEvent).isTrue();
		assertThat(((DistributedObjectEvent) msg.getPayload()).getEventType()).isEqualTo(eventType);
		assertThat(distributedObjectName).as((((DistributedObjectEvent) msg.getPayload()).getDistributedObject())
				.getName()).isNotNull();
	}

	private HazelcastInboundChannelAdapterTestUtils() {
	}

}
