/*
 * Copyright 2016-2022 the original author or authors.
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

package org.springframework.integration.hazelcast.outbound.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.MessageListener;

import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.HazelcastTestRequestHandlerAdvice;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Util Class for Hazelcast Outbound Channel Adapter Test Support
 *
 * @author Eren Avsarogullari
 *
 * @since 6.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class HazelcastOutboundChannelAdapterTestUtils {

	public static final int DATA_COUNT = 100;

	public static final int DEFAULT_AGE = 5;

	public static final String TEST_NAME = "Test_Name";

	public static final String TEST_SURNAME = "Test_Surname";

	public static void testWriteToDistributedMap(MessageChannel channel,
			Map<?, ?> distributedMap,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		testWriteToMap(channel, distributedMap, requestHandlerAdvice);
	}

	private static void testWriteToMap(MessageChannel channel,
			Map<?, ?> distributedMap,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			sendMessageToChannel(channel);
			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
			verifyMapForPayload(new TreeMap(distributedMap));
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}
	}

	public static void testBulkWriteToDistributedMap(MessageChannel channel,
			Map<?, ?> distributedMap,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			Map<Integer, HazelcastIntegrationTestUser> userMap =
					new HashMap<>(DATA_COUNT);
			for (int index = 1; index <= DATA_COUNT; index++) {
				userMap.put(index, getTestUser(index));
			}

			channel.send(new GenericMessage<>(userMap));

			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
			verifyMapForPayload(new TreeMap(distributedMap));
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}
	}

	public static void testWriteToMultiMap(MessageChannel channel,
			MultiMap<Integer, HazelcastIntegrationTestUser> multiMap,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			sendMessageToChannel(channel);
			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
			verifyMultiMapForPayload(multiMap);
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}
	}

	public static void testWriteToReplicatedMap(MessageChannel channel,
			ReplicatedMap<Integer, HazelcastIntegrationTestUser> replicatedMap,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		testWriteToMap(channel, replicatedMap, requestHandlerAdvice);
	}

	public static void testWriteToDistributedList(MessageChannel channel,
			List<HazelcastIntegrationTestUser> distributedList,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		testWriteToDistributedCollection(channel, distributedList, requestHandlerAdvice);
	}

	private static void testWriteToDistributedCollection(MessageChannel channel,
			Collection<HazelcastIntegrationTestUser> distributedList,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			sendMessageToChannel(channel);
			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
			verifyCollection(distributedList, DATA_COUNT);
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}
	}

	public static void testWriteToDistributedSet(MessageChannel channel,
			Set<HazelcastIntegrationTestUser> distributedSet,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			sendMessageToChannel(channel);
			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
			final List<HazelcastIntegrationTestUser> list = new ArrayList(distributedSet);
			Collections.sort(list);
			verifyCollection(list, DATA_COUNT);
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}
	}

	public static void testWriteToDistributedQueue(MessageChannel channel,
			Queue<HazelcastIntegrationTestUser> distributedQueue,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		testWriteToDistributedCollection(channel, distributedQueue, requestHandlerAdvice);
	}

	public static void testWriteToTopic(MessageChannel channel,
			ITopic<HazelcastIntegrationTestUser> topic,
			HazelcastTestRequestHandlerAdvice requestHandlerAdvice) {
		try {
			topic.addMessageListener(new MessageListener() {

				private int index = 1;

				@Override
				public void onMessage(com.hazelcast.topic.Message message) {
					HazelcastIntegrationTestUser user =
							(HazelcastIntegrationTestUser) message.getMessageObject();
					verifyHazelcastIntegrationTestUser(user, index);
					index++;
				}
			});
			sendMessageToChannel(channel);
			assertThat(requestHandlerAdvice.executeLatch.await(10, TimeUnit.SECONDS)).isTrue();
		}
		catch (InterruptedException e) {
			fail("Test has been failed due to " + e.getMessage());
		}

	}

	public static HazelcastIntegrationTestUser getTestUser(int index) {
		return new HazelcastIntegrationTestUser(index, TEST_NAME, TEST_SURNAME,
				index + DEFAULT_AGE);
	}

	public static void verifyMapForPayload(
			final Map<Integer, HazelcastIntegrationTestUser> map) {
		int index = 1;
		assertThat(map).isNotNull();
		assertThat(map.size() == DATA_COUNT).isEqualTo(true);
		for (Map.Entry<Integer, HazelcastIntegrationTestUser> entry : map.entrySet()) {
			assertThat(entry).isNotNull();
			assertThat(entry.getKey().intValue()).isEqualTo(index);
			verifyHazelcastIntegrationTestUser(entry.getValue(), index);
			index++;
		}
	}

	public static void verifyCollection(
			final Collection<HazelcastIntegrationTestUser> coll, final int dataCount) {
		int index = 1;
		assertThat(coll).isNotNull();
		assertThat(coll.size() == dataCount).isEqualTo(true);
		for (HazelcastIntegrationTestUser user : coll) {
			verifyHazelcastIntegrationTestUser(user, index);
			index++;
		}
	}

	public static void verifyHazelcastIntegrationTestUser(
			HazelcastIntegrationTestUser user, int index) {
		assertThat(user).isNotNull();
		assertThat(user.getId()).isEqualTo(index);
		assertThat(user.getName()).isEqualTo(TEST_NAME);
		assertThat(user.getSurname()).isEqualTo(TEST_SURNAME);
		assertThat(user.getAge()).isEqualTo(index + DEFAULT_AGE);
	}

	private static void sendMessageToChannel(final MessageChannel channel) {
		for (int index = 1; index <= DATA_COUNT; index++) {
			channel.send(new GenericMessage<>(getTestUser(index)));
		}
	}

	private static void verifyMultiMapForPayload(
			final MultiMap<Integer, HazelcastIntegrationTestUser> multiMap) {
		int index = 1;
		assertThat(multiMap).isNotNull();
		assertThat(multiMap.size() == DATA_COUNT).isEqualTo(true);
		SortedSet<Integer> keys = new TreeSet<>(multiMap.keySet());
		for (Integer key : keys) {
			assertThat(key).isNotNull();
			assertThat(key.intValue()).isEqualTo(index);
			HazelcastIntegrationTestUser user = multiMap.get(key).iterator().next();
			verifyHazelcastIntegrationTestUser(user, index);
			index++;
		}
	}

	private HazelcastOutboundChannelAdapterTestUtils() {
	}

}
