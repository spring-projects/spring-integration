/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.gemfire.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.Scope;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GenericRegionFactoryBean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class GemfireMessageStoreTests {

	private static CacheFactoryBean cacheFactoryBean;

	private static Region<Object, Object> region;

	@Test
	public void addAndGetMessage() {
		GemfireMessageStore store = new GemfireMessageStore(region);
		Message<?> message = MessageBuilder.withPayload("test").build();
		store.addMessage(message);
		Message<?> retrieved = store.getMessage(message.getHeaders().getId());
		assertEquals(message, retrieved);
	}

	@Test
	public void testRegionConstructor() throws Exception {
		GenericRegionFactoryBean<Object, Object> region = new GenericRegionFactoryBean<>();
		region.setName("someRegion");
		region.setCache(cacheFactoryBean.getObject());
		region.afterPropertiesSet();

		GemfireMessageStore store = new GemfireMessageStore(region.getObject());
		assertSame(region.getObject(), TestUtils.getPropertyValue(store, "messageStoreRegion"));

		region.destroy();
	}

	@Test
	public void testWithMessageHistory() {
		GemfireMessageStore store = new GemfireMessageStore(region);

		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		store.addMessage(message);
		message = store.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertNotNull(messageHistory);
		assertEquals(2, messageHistory.size());
		Properties fooChannelHistory = messageHistory.get(0);
		assertEquals("fooChannel", fooChannelHistory.get("name"));
		assertEquals("channel", fooChannelHistory.get("type"));
	}

	@Test
	public void testAddAndRemoveMessagesFromMessageGroup() {
		GemfireMessageStore messageStore = new GemfireMessageStore(region);

		String groupId = "X";
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messageStore.addMessagesToGroup(groupId, message);
			messages.add(message);
		}
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(25, group.size());
		messageStore.removeMessagesFromGroup(groupId, messages);
		group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Test
	public void testAddAndRemoveMessagesFromMessageGroupWithPrefix() {
		GemfireMessageStore messageStore = new GemfireMessageStore(region, "foo_");

		String groupId = "X";
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messageStore.addMessagesToGroup(groupId, message);
			messages.add(message);
		}

		MessageGroupMetadata messageGroupMetadata =
				(MessageGroupMetadata) region.get("foo_" + "MESSAGE_GROUP_" + groupId);

		assertNotNull(messageGroupMetadata);
		assertEquals(25, messageGroupMetadata.size());

		messageStore.removeMessagesFromGroup(groupId, messages);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertEquals(0, group.size());
	}

	@Before
	public void prepare() {
		if (region != null) {
			region.clear();
		}
	}

	@BeforeClass
	public static void init() throws Exception {
		cacheFactoryBean = new CacheFactoryBean();
		Cache cache = (Cache) cacheFactoryBean.getObject();
		region = cache.createRegionFactory().setScope(Scope.LOCAL).create("sig-tests");
	}

	@AfterClass
	public static void cleanup() throws Exception {
		if (region != null) {
			region.close();
		}
		if (cacheFactoryBean != null) {
			cacheFactoryBean.destroy();
		}
	}

}
