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

package org.springframework.integration.hazelcast.outbound.config;

import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.HazelcastTestRequestHandlerAdvice;
import org.springframework.integration.hazelcast.outbound.HazelcastCacheWritingMessageHandler;
import org.springframework.integration.hazelcast.outbound.util.HazelcastOutboundChannelAdapterTestUtils;
import org.springframework.messaging.MessageChannel;

/**
 * Configuration Class for Hazelcast Integration Outbound Test
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@Configuration
@EnableIntegration
public class HazelcastIntegrationOutboundTestConfiguration {

	@Bean
	public MessageChannel distMapChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel distMapBulkChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel distListChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel distSetChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel distQueueChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel topicChannel2() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel multiMapChannel2() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel replicatedMapChannel2() {
		return new DirectChannel();
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> distMap() {
		return testHzInstance().getMap("Distributed_Map1");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> distBulkMap() {
		return testHzInstance().getMap("Distributed_Bulk_Map1");
	}

	@Bean
	public List<HazelcastIntegrationTestUser> distList() {
		return testHzInstance().getList("Distributed_List1");
	}

	@Bean
	public Set<HazelcastIntegrationTestUser> distSet() {
		return testHzInstance().getSet("Distributed_Set1");
	}

	@Bean
	public Queue<HazelcastIntegrationTestUser> distQueue() {
		return testHzInstance().getQueue("Distributed_Queue1");
	}

	@Bean
	public ITopic<HazelcastIntegrationTestUser> topic() {
		return testHzInstance().getTopic("Topic1");
	}

	@Bean
	public MultiMap<Integer, HazelcastIntegrationTestUser> multiMap() {
		return testHzInstance().getMultiMap("Multi_Map1");
	}

	@Bean
	public ReplicatedMap<Integer, HazelcastIntegrationTestUser> replicatedMap() {
		return testHzInstance().getReplicatedMap("Replicated_Map1");
	}

	@Bean(destroyMethod = "shutdown")
	public HazelcastInstance testHzInstance() {
		return Hazelcast.newHazelcastInstance();
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice distMapRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice distBulkMapRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(1);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice distListRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice distSetRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice distQueueRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice topicRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice multiMapRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	public HazelcastTestRequestHandlerAdvice replicatedMapRequestHandlerAdvice() {
		return new HazelcastTestRequestHandlerAdvice(
				HazelcastOutboundChannelAdapterTestUtils.DATA_COUNT);
	}

	@Bean
	@ServiceActivator(inputChannel = "distMapChannel", adviceChain = "distMapRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject(distMap());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "distMapBulkChannel",
			adviceChain = "distBulkMapRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler2() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject(distBulkMap());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "distListChannel",
			adviceChain = "distListRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler3() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject((DistributedObject) distList());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "distSetChannel",
			adviceChain = "distSetRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler4() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject((DistributedObject) distSet());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "distQueueChannel",
			adviceChain = "distQueueRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler5() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject((DistributedObject) distQueue());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "topicChannel2",
			adviceChain = "topicRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler6() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject(topic());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "multiMapChannel2",
			adviceChain = "multiMapRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler7() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject(multiMap());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

	@Bean
	@ServiceActivator(inputChannel = "replicatedMapChannel2",
			adviceChain = "replicatedMapRequestHandlerAdvice")
	public HazelcastCacheWritingMessageHandler hazelcastCacheWritingMessageHandler8() {
		final HazelcastCacheWritingMessageHandler handler =
				new HazelcastCacheWritingMessageHandler();
		handler.setDistributedObject(replicatedMap());
		handler
				.setKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
		handler.setExtractPayload(true);

		return handler;
	}

}
