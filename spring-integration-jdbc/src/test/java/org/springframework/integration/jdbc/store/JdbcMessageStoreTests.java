/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jdbc.store;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.hsqldb.HsqlException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.support.CollectionArgumentResolver;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.predicate.MessagePredicate;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.UUIDConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Will Schipp
 */
@SpringJUnitConfig
@DirtiesContext // close at the end after class
@Transactional
public class JdbcMessageStoreTests {

	@Autowired
	private DataSource dataSource;

	private JdbcMessageStore messageStore;

	@BeforeEach
	public void init() {
		messageStore = new JdbcMessageStore(dataSource);
	}

	@Test
	public void testGetNonExistent() {
		Message<?> result = messageStore.getMessage(UUID.randomUUID());
		assertThat(result).isNull();
	}

	@Test
	public void testAddAndGet() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNotNull();
		assertThat(saved).matches(new MessagePredicate(result));
	}

	@Test
	public void testWithMessageHistory() {
		Message<?> message = new GenericMessage<>("Hello");
		DirectChannel fooChannel = new DirectChannel();
		fooChannel.setBeanName("fooChannel");
		DirectChannel barChannel = new DirectChannel();
		barChannel.setBeanName("barChannel");

		message = MessageHistory.write(message, fooChannel);
		message = MessageHistory.write(message, barChannel);
		messageStore.addMessage(message);
		message = messageStore.getMessage(message.getHeaders().getId());
		MessageHistory messageHistory = MessageHistory.read(message);
		assertThat(messageHistory).isNotNull();
		assertThat(messageHistory.size()).isEqualTo(2);
		Properties fooChannelHistory = messageHistory.get(0);
		assertThat(fooChannelHistory.get("name")).isEqualTo("fooChannel");
		assertThat(fooChannelHistory.get("type")).isEqualTo("channel");
	}

	@Test
	public void testSize() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessage(message);
		assertThat(messageStore.getMessageCount()).isEqualTo(1);
	}

	@Test
	public void testSerializer() {
		// N.B. these serializers are not realistic (just for test purposes)
		messageStore.setSerializer((object, outputStream) -> {
			outputStream.write(object.getPayload().toString().getBytes());
			outputStream.flush();
		});
		messageStore.setDeserializer(inputStream -> {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			return new GenericMessage<>(reader.readLine());
		});
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		assertThat(messageStore.getMessage(message.getHeaders().getId())).isNotNull();
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

	@Test
	public void testAddAndGetWithDifferentRegion() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		messageStore.setRegion("FOO");
		Message<?> result = messageStore.getMessage(saved.getHeaders().getId());
		assertThat(result).isNull();
	}

	@Test
	public void testAddAndUpdate() {
		Message<?> message = MessageBuilder.withPayload("foo").setCorrelationId("X").build();
		message = messageStore.addMessage(message);
		message = MessageBuilder.fromMessage(message).setCorrelationId("Y").build();
		message = messageStore.addMessage(message);
		message = messageStore.getMessage(message.getHeaders().getId());
		assertThat(new IntegrationMessageHeaderAccessor(message).getCorrelationId()).isEqualTo("Y");
	}

	@Test
	public void testAddAndUpdateAlreadySaved() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		Message<String> result = messageStore.addMessage(message);
		assertThat(result).isEqualTo(message);
	}

	@Test
	public void testAddAndUpdateAlreadySavedAndCopied() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).build();
		Message<String> result = messageStore.addMessage(copy);
		assertThat(result).isEqualTo(copy);
		assertThat(result).isEqualTo(saved);
		assertThat(messageStore.getMessage(saved.getHeaders().getId())).isNotNull();
	}

	@Test
	public void testAddAndUpdateWithChange() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		Message<String> saved = messageStore.addMessage(message);
		Message<String> copy = MessageBuilder.fromMessage(saved).setHeader("newHeader", 1).build();
		Message<String> result = messageStore.addMessage(copy);
		assertThat(result).isNotSameAs(saved);
		assertThat(saved).matches(new MessagePredicate(result, "newHeader"));
		assertThat(messageStore.getMessage(saved.getHeaders().getId())).isNotNull();
	}

	@Test
	public void testAddAndRemoveMessageGroup() {
		Message<String> message = MessageBuilder.withPayload("foo").build();
		message = messageStore.addMessage(message);
		assertThat(messageStore.removeMessage(message.getHeaders().getId())).isNotNull();
	}

	@Test
	public void testAddAndGetMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		long now = System.currentTimeMillis();
		messageStore.addMessagesToGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);
		assertThat(group.getTimestamp() >= now).as("Timestamp too early: " + group.getTimestamp() + "<" + now).isTrue();
	}

	@Test
	public void testAddAndRemoveMessageFromMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.removeMessagesFromGroup(groupId, message);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	@Test
	public void testAddAndRemoveMessagesFromMessageGroup() {
		String groupId = "X";
		this.messageStore.setRemoveBatchSize(10);
		List<Message<?>> messages = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
			messages.add(message);
		}
		this.messageStore.addMessagesToGroup(groupId, messages.toArray(new Message<?>[0]));
		MessageGroup group = this.messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(25);
		this.messageStore.removeMessagesFromGroup(groupId, messages);
		group = this.messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	@Test
	public void testRemoveMessageGroup() {
		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();
		String groupId = "X";

		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.removeMessageGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);

		String uuidGroupId = UUIDConverter.getUUID(groupId).toString();
		assertThat(template.queryForList(
				"SELECT * from INT_GROUP_TO_MESSAGE where GROUP_KEY = ?", uuidGroupId).size() == 0).isTrue();
	}

	@Test
	public void testCompleteMessageGroup() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.completeGroup(groupId);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.isComplete()).isTrue();
		assertThat(group.size()).isEqualTo(1);
	}

	@Test
	public void testUpdateLastReleasedSequence() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.setLastReleasedSequenceNumberForGroup(groupId, 5);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.getLastReleasedMessageSequenceNumber()).isEqualTo(5);
	}

	@Test
	public void testMessageGroupCount() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessagesToGroup(groupId, message);
		assertThat(messageStore.getMessageGroupCount()).isEqualTo(1);
	}

	@Test
	public void testMessageGroupSizes() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		messageStore.addMessagesToGroup(groupId, message);
		assertThat(messageStore.getMessageCountForAllMessageGroups()).isEqualTo(1);
	}

	@Test
	public void testOrderInMessageGroup() throws Exception {
		String groupId = "X";

		this.messageStore.addMessagesToGroup(groupId,
				MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(1);
		this.messageStore.addMessagesToGroup(groupId,
				MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		MessageGroup group = this.messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(2);
		assertThat(this.messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("foo");
		assertThat(this.messageStore.pollMessageFromGroup(groupId).getPayload()).isEqualTo("bar");
	}

	@Test
	public void testExpireMessageGroupOnCreateOnly() throws Exception {
		final String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.addMessagesToGroup(groupId, message);
		final CountDownLatch groupRemovalLatch = new CountDownLatch(1);
		messageStore.registerMessageGroupExpiryCallback((messageGroupStore, group) -> {
			messageGroupStore.removeMessageGroup(group.getGroupId());
			groupRemovalLatch.countDown();
		});

		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();

		template.update("UPDATE INT_MESSAGE_GROUP set CREATED_DATE=? where GROUP_KEY=? and REGION=?",
				ps -> {
					ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - 10000));
					ps.setString(2, UUIDConverter.getUUID(groupId).toString());
					ps.setString(3, "DEFAULT");
				});

		messageStore.expireMessageGroups(2000);

		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
		assertThat(groupRemovalLatch.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void testExpireMessageGroupOnIdleOnly() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").setCorrelationId(groupId).build();
		messageStore.setTimeoutOnIdle(true);
		messageStore.addMessagesToGroup(groupId, message);
		messageStore.registerMessageGroupExpiryCallback((messageGroupStore, group) -> messageGroupStore
				.removeMessageGroup(group.getGroupId()));

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.afterPropertiesSet();

		updateMessageGroup(template, groupId, 1000);

		messageStore.expireMessageGroups(2000);
		MessageGroup group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(1);

		updateMessageGroup(template, groupId, 2000);

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(2);

		updateMessageGroup(template, groupId, 2000);

		messageStore.expireMessageGroups(2000);
		group = messageStore.getMessageGroup(groupId);
		assertThat(group.size()).isEqualTo(0);
	}

	private void updateMessageGroup(JdbcTemplate template, final String groupId, final long timeout) {
		template.update("UPDATE INT_MESSAGE_GROUP set UPDATED_DATE=? where GROUP_KEY=? and REGION=?",
				(PreparedStatementSetter) ps -> {
					ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - timeout));
					ps.setString(2, UUIDConverter.getUUID(groupId).toString());
					ps.setString(3, "DEFAULT");
				});

	}

	@Test
	public void testMessagePollingFromTheGroup() throws Exception {
		String groupId = "X";

		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("foo").setCorrelationId(groupId).build());
		Thread.sleep(10);
		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("bar").setCorrelationId(groupId).build());
		Thread.sleep(10);
		messageStore.addMessagesToGroup(groupId, MessageBuilder.withPayload("baz").setCorrelationId(groupId).build());

		messageStore.addMessagesToGroup("Y", MessageBuilder.withPayload("barA").setCorrelationId(groupId).build(),
				MessageBuilder.withPayload("bazA").setCorrelationId(groupId).build());

		MessageGroup group = messageStore.getMessageGroup("X");
		assertThat(group.size()).isEqualTo(3);

		Message<?> message1 = messageStore.pollMessageFromGroup("X");
		assertThat(message1).isNotNull();
		assertThat(message1.getPayload()).isEqualTo("foo");

		group = messageStore.getMessageGroup("X");
		assertThat(group.size()).isEqualTo(2);

		Message<?> message2 = messageStore.pollMessageFromGroup("X");
		assertThat(message2).isNotNull();
		assertThat(message2.getPayload()).isEqualTo("bar");

		group = messageStore.getMessageGroup("X");
		assertThat(group.size()).isEqualTo(1);
	}

	@Test
	public void testSameMessageToMultipleGroups() {
		final String group1Id = "group1";
		final String group2Id = "group2";

		final Message<String> message = MessageBuilder.withPayload("foo").build();

		final MessageBuilder<String> builder1 = MessageBuilder.fromMessage(message);
		final MessageBuilder<String> builder2 = MessageBuilder.fromMessage(message);

		builder1.setSequenceNumber(1);
		builder2.setSequenceNumber(2);

		final Message<?> message1 = builder1.build();
		final Message<?> message2 = builder2.build();

		messageStore.addMessageToGroup(group1Id, message1);
		messageStore.addMessageToGroup(group2Id, message2);

		final Message<?> messageFromGroup1 = messageStore.pollMessageFromGroup(group1Id);
		final Message<?> messageFromGroup2 = messageStore.pollMessageFromGroup(group2Id);

		assertThat(messageFromGroup1).isNotNull();
		assertThat(messageFromGroup2).isNotNull();

		assertThat(messageFromGroup1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(1);
		assertThat(messageFromGroup2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(2);

	}

	@Test
	public void testSameMessageAndGroupToMultipleRegions() {

		final String groupId = "myGroup";
		final String region1 = "region1";
		final String region2 = "region2";

		final JdbcMessageStore messageStore1 = new JdbcMessageStore(dataSource);
		messageStore1.setRegion(region1);

		final JdbcMessageStore messageStore2 = new JdbcMessageStore(dataSource);
		messageStore1.setRegion(region2);

		final Message<String> message = MessageBuilder.withPayload("foo").build();

		final MessageBuilder<String> builder1 = MessageBuilder.fromMessage(message);
		final MessageBuilder<String> builder2 = MessageBuilder.fromMessage(message);

		builder1.setSequenceNumber(1);
		builder2.setSequenceNumber(2);

		final Message<?> message1 = builder1.build();
		final Message<?> message2 = builder2.build();

		messageStore1.addMessageToGroup(groupId, message1);
		messageStore2.addMessageToGroup(groupId, message2);

		final Message<?> messageFromRegion1 = messageStore1.pollMessageFromGroup(groupId);
		final Message<?> messageFromRegion2 = messageStore2.pollMessageFromGroup(groupId);

		assertThat(messageFromRegion1).isNotNull();
		assertThat(messageFromRegion2).isNotNull();

		assertThat(messageFromRegion1.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(1);
		assertThat(messageFromRegion2.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER)).isEqualTo(2);

	}

	@Test
	public void testCompletedNotExpiredGroupINT3037() {
		/*
		 * based on the aggregator scenario as follows;
		 *
		 * send three messages in
		 * 1 of 2
		 * 2 of 2
		 * 2 of 2 (last again)
		 *
		 * expected behavior is that the LAST message (2 of 2 repeat) should be on the discard channel
		 * (discard behavior performed by the AbstractCorrelatingMessageHandler.handleMessageInternal)
		 */
		final JdbcMessageStore messageStore = new JdbcMessageStore(dataSource);
		//init
		String groupId = "group";
		//build the messages
		Message<?> oneOfTwo = MessageBuilder.withPayload("hello")
				.setSequenceNumber(1)
				.setSequenceSize(2)
				.setCorrelationId(groupId)
				.build();
		Message<?> twoOfTwo = MessageBuilder.withPayload("world")
				.setSequenceNumber(2)
				.setSequenceSize(2)
				.setCorrelationId(groupId)
				.build();
		//add to the messageStore
		messageStore.addMessagesToGroup(groupId, oneOfTwo, twoOfTwo);
		//check that 2 messages are there
		assertThat(messageStore.getMessageGroupCount() == 1).isTrue();
		assertThat(messageStore.getMessageCount() == 2).isTrue();
		//retrieve the group (like in the aggregator)
		MessageGroup messageGroup = messageStore.getMessageGroup(groupId);
		//'complete' the group
		messageStore.completeGroup(messageGroup.getGroupId());
		//now clear the messages
		for (Message<?> message : messageGroup.getMessages()) {
			messageStore.removeMessagesFromGroup(groupId, message);
		} //end for
		//'add' the other message --> emulated by getting the messageGroup
		messageGroup = messageStore.getMessageGroup(groupId);
		//should be marked 'complete' --> old behavior it would not
		assertThat(messageGroup.isComplete()).isTrue();
	}

	@Test
	public void testMessageGroupCondition() {
		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("foo").build();
		this.messageStore.addMessagesToGroup(groupId, message);
		this.messageStore.setGroupCondition(groupId, "testCondition");
		assertThat(this.messageStore.getMessageGroup(groupId).getCondition()).isEqualTo("testCondition");
	}

	@Test
	@Transactional(propagation = Propagation.NEVER)
	public void testMessageGroupStreamNoConnectionPoolLeak() throws NoSuchMethodException {
		DataSourceConnectionFactory connFactory = new DataSourceConnectionFactory(this.dataSource);
		PoolableConnectionFactory poolFactory = new PoolableConnectionFactory(connFactory, null);
		GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
		config.setMaxTotal(2);
		config.setMaxWait(Duration.ofMillis(500));
		ObjectPool<PoolableConnection> connPool = new GenericObjectPool<>(poolFactory, config);
		poolFactory.setPool(connPool);
		PoolingDataSource<PoolableConnection> poolingDataSource = new PoolingDataSource<>(connPool);

		JdbcMessageStore pooledMessageStore = new JdbcMessageStore(poolingDataSource);

		CollectionArgumentResolver collectionArgumentResolver = new CollectionArgumentResolver(true);
		collectionArgumentResolver.setBeanFactory(new DefaultListableBeanFactory());
		Method methodForCollectionOfPayloads = getClass().getMethod("methodForCollectionOfPayloads", Collection.class);
		MethodParameter methodParameter = SynthesizingMethodParameter.forExecutable(methodForCollectionOfPayloads, 0);

		String groupId = "X";
		Message<String> message = MessageBuilder.withPayload("test data").build();
		pooledMessageStore.addMessagesToGroup(groupId, message);

		// Before the stream close fix in the 'CollectionArgumentResolver'
		// it failed with "Cannot get a connection, pool error Timeout waiting for idle object"
		for (int i = 0; i < 3; i++) {
			Object result =
					collectionArgumentResolver.resolveArgument(methodParameter,
							new GenericMessage<>(pooledMessageStore.getMessageGroup(groupId).getMessages()));

			assertThat(result)
					.asInstanceOf(InstanceOfAssertFactories.LIST)
					.hasSize(1)
					.contains("test data");
		}

		pooledMessageStore.removeMessageGroup(groupId);
	}

	@Test
	void noTableThrowsExceptionOnStart() {
		try (TestUtils.TestApplicationContext testApplicationContext = TestUtils.createTestApplicationContext()) {
			JdbcMessageStore jdbcMessageStore = new JdbcMessageStore(this.dataSource);
			jdbcMessageStore.setTablePrefix("TEST_");
			testApplicationContext.registerBean("jdbcMessageStore", jdbcMessageStore);
			assertThatExceptionOfType(ApplicationContextException.class)
					.isThrownBy(testApplicationContext::refresh)
					.withRootCauseExactlyInstanceOf(HsqlException.class)
					.withStackTraceContaining("user lacks privilege or object not found: TEST_MESSAGE_GROUP");
		}
	}

	public void methodForCollectionOfPayloads(Collection<String> payloads) {
	}

}
