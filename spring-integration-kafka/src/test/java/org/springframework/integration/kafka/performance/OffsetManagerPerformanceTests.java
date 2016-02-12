/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.kafka.performance;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.integration.kafka.util.TopicUtils.ensureTopicCreated;

import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.listener.KafkaNativeOffsetManager;
import org.springframework.integration.kafka.listener.KafkaTopicOffsetManager;
import org.springframework.integration.kafka.listener.OffsetManager;
import org.springframework.integration.kafka.listener.WindowingOffsetManager;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.util.StopWatch;

import com.gs.collections.api.RichIterable;
import com.gs.collections.api.block.function.Function2;
import com.gs.collections.api.multimap.Multimap;
import com.gs.collections.api.multimap.MutableMultimap;
import com.gs.collections.api.tuple.Pair;
import com.gs.collections.impl.factory.Multimaps;
import com.gs.collections.impl.tuple.Tuples;
import scala.collection.JavaConversions;
import scala.collection.Map;
import scala.collection.immutable.List$;
import scala.collection.immutable.Map$;
import scala.collection.immutable.Seq;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class OffsetManagerPerformanceTests {

	public static final int UPDATE_COUNT = 100000;

	@Rule
	public KafkaEmbedded embedded = new KafkaEmbedded(1);

	private final StopWatch stopWatch = new StopWatch("OffsetManagers Performance");

	@Test
	public void testPerformance() throws Exception {
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect(embedded.getZookeeperConnectionString());

		KafkaTopicOffsetManager kafkaTopicOffsetManager =
				new KafkaTopicOffsetManager(zookeeperConnect, "zkTopic");

		KafkaNativeOffsetManager kafkaNativeOffsetManager = new KafkaNativeOffsetManager(
				new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeperConnect)), zookeeperConnect);

		KafkaTopicOffsetManager topicOffsetManagerForAssertion =
				new KafkaTopicOffsetManager(zookeeperConnect, "zkTopic");

		KafkaNativeOffsetManager nativeOffsetManagerForAssertion = new KafkaNativeOffsetManager(
				new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeperConnect)), zookeeperConnect);

		doTest("Just KafkaTopicOffsetManager", kafkaTopicOffsetManager, topicOffsetManagerForAssertion, false);
		doTest("Just KafkaNativeOffsetManager", kafkaNativeOffsetManager, nativeOffsetManagerForAssertion, false);
		doTest("Window KafkaTopicOffsetManager", kafkaTopicOffsetManager, topicOffsetManagerForAssertion, true);
		doTest("Window KafkaNativeOffsetManager", kafkaNativeOffsetManager, nativeOffsetManagerForAssertion, true);

		System.out.println(this.stopWatch.prettyPrint());
	}

	private void doTest(String description, OffsetManager offsetManager,
	                    OffsetManager offsetManagerForAssertion, boolean window) throws Exception {
		createTopic(this.embedded.getZkClient(), "sometopic", 1, 1, 1);
		Partition partition = new Partition("sometopic", 0);

		((InitializingBean) offsetManager).afterPropertiesSet();

		offsetManager.updateOffset(partition, 0);

		if (window) {
			WindowingOffsetManager windowingOffsetManager = new WindowingOffsetManager(offsetManager);
			windowingOffsetManager.setCount(100);
			windowingOffsetManager.afterPropertiesSet();
			offsetManager = windowingOffsetManager;
		}

		this.stopWatch.start(description);
		for (long i = 1; i < UPDATE_COUNT; i++) {
			offsetManager.updateOffset(partition, i);
		}
		stopWatch.stop();
		((DisposableBean) offsetManager).destroy();

		((InitializingBean) offsetManagerForAssertion).afterPropertiesSet();
		Assert.assertThat(offsetManagerForAssertion.getOffset(partition), is(99999L));
	}

	@SuppressWarnings("unchecked")
	public void createTopic(ZkClient zkClient, String topicName, int partitionCount, int brokers, int replication) {
		MutableMultimap<Integer, Integer> partitionDistribution =
				createPartitionDistribution(partitionCount, brokers, replication);
		ensureTopicCreated(zkClient, topicName, partitionCount, new Properties(),
				toKafkaPartitionMap(partitionDistribution));
	}


	public MutableMultimap<Integer, Integer> createPartitionDistribution(int partitionCount, int brokers,
	                                                                     int replication) {
		MutableMultimap<Integer, Integer> partitionDistribution = Multimaps.mutable.list.with();
		for (int i = 0; i < partitionCount; i++) {
			for (int j = 0; j < replication; j++) {
				partitionDistribution.put(i, (i + j) % brokers);
			}
		}
		return partitionDistribution;
	}

	@SuppressWarnings({"rawtypes", "serial", "deprecation", "unchecked"})
	private Map toKafkaPartitionMap(Multimap<Integer, Integer> partitions) {
		java.util.Map<Object, Seq<Object>> m = partitions.toMap()
				.collect(new Function2<Integer, RichIterable<Integer>, Pair<Object, Seq<Object>>>() {

					@Override
					public Pair<Object, Seq<Object>> value(Integer argument1, RichIterable<Integer> argument2) {
						return Tuples.pair((Object) argument1,
								List$.MODULE$.fromArray(argument2.toArray(new Object[0])).toSeq());
					}

				});
		return Map$.MODULE$.apply(JavaConversions.asScalaMap(m).toSeq());
	}

}
