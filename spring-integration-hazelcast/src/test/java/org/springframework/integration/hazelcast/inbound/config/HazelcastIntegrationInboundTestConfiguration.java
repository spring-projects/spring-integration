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

package org.springframework.integration.hazelcast.inbound.config;

import com.hazelcast.collection.IList;
import com.hazelcast.collection.IQueue;
import com.hazelcast.collection.ISet;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.replicatedmap.ReplicatedMap;
import com.hazelcast.topic.ITopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.hazelcast.DistributedSQLIterationType;
import org.springframework.integration.hazelcast.HazelcastIntegrationTestUser;
import org.springframework.integration.hazelcast.HazelcastLocalInstanceRegistrar;
import org.springframework.integration.hazelcast.inbound.HazelcastClusterMonitorMessageProducer;
import org.springframework.integration.hazelcast.inbound.HazelcastContinuousQueryMessageProducer;
import org.springframework.integration.hazelcast.inbound.HazelcastDistributedSQLMessageSource;
import org.springframework.integration.hazelcast.inbound.HazelcastEventDrivenMessageProducer;
import org.springframework.messaging.PollableChannel;

/**
 * Configuration Class for Hazelcast Integration Inbound Test
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 */
@Configuration
@EnableIntegration
public class HazelcastIntegrationInboundTestConfiguration {

	@Bean
	public PollableChannel distributedMapChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel distributedMapChannel2() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel distributedListChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel distributedSetChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel distributedQueueChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel topicChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel multiMapChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel replicatedMapChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel cqDistributedMapChannel1() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel cqDistributedMapChannel2() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel cqDistributedMapChannel3() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel dsDistributedMapChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel dsDistributedMapChannel2() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel dsDistributedMapChannel3() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel dsDistributedMapChannel4() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel cmonChannel() {
		return new QueueChannel();
	}

	@Bean
	public PollableChannel cmonChannel2() {
		return new QueueChannel();
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDistributedMap() {
		return testHazelcastInstance().getMap("Test_Distributed_Map");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDistributedMap2() {
		return testHazelcastInstance().getMap("Test_Distributed_Map2");
	}

	@Bean
	public IList<HazelcastIntegrationTestUser> testDistributedList() {
		return testHazelcastInstance().getList("Test_Distributed_List");
	}

	@Bean
	public ISet<HazelcastIntegrationTestUser> testDistributedSet() {
		return testHazelcastInstance().getSet("Test_Distributed_Set");
	}

	@Bean
	public IQueue<HazelcastIntegrationTestUser> testDistributedQueue() {
		return testHazelcastInstance().getQueue("Test_Distributed_Queue");
	}

	@Bean
	public ITopic<HazelcastIntegrationTestUser> testTopic() {
		return testHazelcastInstance().getTopic("Test_Topic");
	}

	@Bean
	public MultiMap<Integer, HazelcastIntegrationTestUser> testMultiMap() {
		return testHazelcastInstance().getMultiMap("Test_Multi_Map");
	}

	@Bean
	public ReplicatedMap<Integer, HazelcastIntegrationTestUser> testReplicatedMap() {
		return testHazelcastInstance().getReplicatedMap("Test_Replicated_Map");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap1() {
		return testHazelcastInstance().getMap("Test_CQ_Distributed_Map1");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap2() {
		return testHazelcastInstance().getMap("Test_CQ_Distributed_Map2");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testCQDistributedMap3() {
		return testHazelcastInstance().getMap("Test_CQ_Distributed_Map3");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap() {
		return testHazelcastInstance().getMap("Test_DS_Distributed_Map");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap2() {
		return testHazelcastInstance().getMap("Test_DS_Distributed_Map2");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap3() {
		return testHazelcastInstance().getMap("Test_DS_Distributed_Map3");
	}

	@Bean
	public IMap<Integer, HazelcastIntegrationTestUser> testDSDistributedMap4() {
		return testHazelcastInstance().getMap("Test_DS_Distributed_Map4");
	}

	@Bean
	public Config hazelcastConfig() {
		Config config = new Config();
		config.getCPSubsystemConfig().setCPMemberCount(0)
				.setSessionHeartbeatIntervalSeconds(1);
		return config;
	}

	@Bean(destroyMethod = "shutdown")
	public HazelcastInstance testHazelcastInstance() {
		return Hazelcast.newHazelcastInstance(hazelcastConfig());
	}

	@Bean(HazelcastLocalInstanceRegistrar.BEAN_NAME)
	public HazelcastLocalInstanceRegistrar hazelcastLocalInstanceRegistrar() {
		return new HazelcastLocalInstanceRegistrar(testHazelcastInstance());
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testDistributedMap());
		producer.setOutputChannel(distributedMapChannel());
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer2() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testDistributedMap2());
		producer.setOutputChannel(distributedMapChannel2());
		producer.setCacheEventTypes("ADDED,REMOVED,UPDATED,CLEAR_ALL");
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer3() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testDistributedList());
		producer.setOutputChannel(distributedListChannel());
		producer.setCacheEventTypes("ADDED,REMOVED");
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer4() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testDistributedSet());
		producer.setOutputChannel(distributedSetChannel());
		producer.setCacheEventTypes("ADDED,REMOVED");
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer5() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testDistributedQueue());
		producer.setOutputChannel(distributedQueueChannel());
		producer.setCacheEventTypes("ADDED,REMOVED");
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer6() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testTopic());
		producer.setOutputChannel(topicChannel());
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer7() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testReplicatedMap());
		producer.setOutputChannel(replicatedMapChannel());
		producer.setCacheEventTypes("ADDED,REMOVED,UPDATED");
		return producer;
	}

	@Bean
	public HazelcastEventDrivenMessageProducer hazelcastEventDrivenMessageProducer8() {
		final HazelcastEventDrivenMessageProducer producer =
				new HazelcastEventDrivenMessageProducer(testMultiMap());
		producer.setOutputChannel(multiMapChannel());
		producer.setCacheEventTypes("ADDED,REMOVED,CLEAR_ALL");
		return producer;
	}

	@Bean
	public HazelcastContinuousQueryMessageProducer hazelcastContinuousQueryMessageProducer() {
		final HazelcastContinuousQueryMessageProducer producer =
				new HazelcastContinuousQueryMessageProducer(testCQDistributedMap1(),
						"name=TestName1");
		producer.setOutputChannel(cqDistributedMapChannel1());
		return producer;
	}

	@Bean
	public HazelcastContinuousQueryMessageProducer hazelcastContinuousQueryMessageProducer2() {
		final HazelcastContinuousQueryMessageProducer producer =
				new HazelcastContinuousQueryMessageProducer(testCQDistributedMap2(),
						"name=TestName1 OR name=TestName2");
		producer.setOutputChannel(cqDistributedMapChannel2());
		producer.setCacheEventTypes("ADDED,REMOVED,UPDATED,CLEAR_ALL");
		return producer;
	}

	@Bean
	public HazelcastContinuousQueryMessageProducer hazelcastContinuousQueryMessageProducer3() {
		final HazelcastContinuousQueryMessageProducer producer =
				new HazelcastContinuousQueryMessageProducer(testCQDistributedMap3(),
						"surname=TestSurname2");
		producer.setOutputChannel(cqDistributedMapChannel3());
		producer.setCacheEventTypes("UPDATED");
		producer.setIncludeValue(false);
		return producer;
	}

	@Bean
	public HazelcastClusterMonitorMessageProducer hazelcastClusterMonitorMessageProducer() {
		final HazelcastClusterMonitorMessageProducer producer =
				new HazelcastClusterMonitorMessageProducer(testHazelcastInstance());
		producer.setOutputChannel(cmonChannel());
		return producer;
	}

	@Bean
	public HazelcastClusterMonitorMessageProducer hazelcastClusterMonitorMessageProducer2() {
		final HazelcastClusterMonitorMessageProducer producer =
				new HazelcastClusterMonitorMessageProducer(testHazelcastInstance());
		producer.setOutputChannel(cmonChannel2());
		producer.setMonitorEventTypes("DISTRIBUTED_OBJECT");
		return producer;
	}

	@Bean
	@InboundChannelAdapter(value = "dsDistributedMapChannel",
			poller = @Poller(maxMessagesPerPoll = "1"))
	public HazelcastDistributedSQLMessageSource hazelcastDistributedSQLMessageSource() {
		final HazelcastDistributedSQLMessageSource messageSource =
				new HazelcastDistributedSQLMessageSource(testDSDistributedMap(),
						"name='TestName4' AND surname='TestSurname4'");
		messageSource.setIterationType(DistributedSQLIterationType.ENTRY);
		return messageSource;
	}

	@Bean
	@InboundChannelAdapter(value = "dsDistributedMapChannel2",
			poller = @Poller(maxMessagesPerPoll = "1"))
	public HazelcastDistributedSQLMessageSource hazelcastDistributedSQLMessageSource2() {
		final HazelcastDistributedSQLMessageSource messageSource =
				new HazelcastDistributedSQLMessageSource(testDSDistributedMap2(),
						"name='TestName1' AND surname='TestSurname1'");
		messageSource.setIterationType(DistributedSQLIterationType.KEY);
		return messageSource;
	}

	@Bean
	@InboundChannelAdapter(value = "dsDistributedMapChannel3",
			poller = @Poller(maxMessagesPerPoll = "1"))
	public HazelcastDistributedSQLMessageSource hazelcastDistributedSQLMessageSource3() {
		final HazelcastDistributedSQLMessageSource messageSource =
				new HazelcastDistributedSQLMessageSource(testDSDistributedMap3(),
						"age > 5");
		messageSource.setIterationType(DistributedSQLIterationType.KEY);
		return messageSource;
	}

	@Bean
	@InboundChannelAdapter(value = "dsDistributedMapChannel4",
			poller = @Poller(maxMessagesPerPoll = "1"))
	public HazelcastDistributedSQLMessageSource hazelcastDistributedSQLMessageSource4() {
		final HazelcastDistributedSQLMessageSource messageSource =
				new HazelcastDistributedSQLMessageSource(testDSDistributedMap4(),
						"name='TestName3' AND surname='TestSurname3'");
		messageSource.setIterationType(DistributedSQLIterationType.VALUE);
		return messageSource;
	}

}
