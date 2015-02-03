/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.core;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import kafka.api.PartitionMetadata;
import kafka.cluster.Broker;
import kafka.common.ErrorMapping;
import kafka.javaapi.TopicMetadata;
import scala.Some;
import scala.collection.Seq;
import scala.collection.mutable.HashSet;
import scala.collection.mutable.HashSet$;

/**
 * @author Marius Bogoevici
 */
public class MetadataCacheTests {


	public static final String NONEXISTENT_TOPIC = "nonexistent";

	public static final String EXISTING_TOPIC = "existing";

	public static final String EXISTING_TOPIC_2 = "existing2";

	@Test
	public void testMetadataCacheEmpty() throws Exception {
		MetadataCache metadataCache = new MetadataCache(Collections.<TopicMetadata>emptySet());

		// blank metadatacache is empty
		assertThat(metadataCache.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(NONEXISTENT_TOPIC), nullValue());
	}

	@Test
	public void testMetadataCacheInitialized() throws Exception {
		ArrayList<TopicMetadata> topicMetadatas = new ArrayList<TopicMetadata>();
		HashSet<PartitionMetadata> partitionMetadatas = HashSet$.MODULE$.empty();
		partitionMetadatas.$plus$eq(new PartitionMetadata(0, asScalaOption(new Broker(0, "host0", 1000)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		partitionMetadatas.$plus$eq(new PartitionMetadata(1, asScalaOption(new Broker(0, "host1", 1001)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		topicMetadatas.add(new TopicMetadata(new kafka.api.TopicMetadata(EXISTING_TOPIC, partitionMetadatas.toSeq(),
				ErrorMapping.NoError())));
		MetadataCache metadataCache = new MetadataCache(topicMetadatas);

		// Initializing MetadataCache with some topic metadatas will find the data in
		assertThat(metadataCache.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC), hasSize(2));
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 0)));
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 1)));
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)), notNullValue());
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)).getHost(), equalTo("host0"));
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)).getPort(), equalTo(1000));
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)), notNullValue());
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)).getHost(), equalTo("host1"));
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)).getPort(), equalTo(1001));
	}

	@Test
	public void testMetadataCacheMerged() throws Exception {
		ArrayList<TopicMetadata> topicMetadatas = new ArrayList<TopicMetadata>();
		HashSet<PartitionMetadata> partitionMetadatas = HashSet$.MODULE$.empty();
		partitionMetadatas.$plus$eq(new PartitionMetadata(0, asScalaOption(new Broker(0, "host0", 1000)), emptySeq(),
				emptySeq(), ErrorMapping.NoError()));
		partitionMetadatas.$plus$eq(new PartitionMetadata(1, asScalaOption(new Broker(0, "host1", 1001)), emptySeq(),
				emptySeq(), ErrorMapping.NoError()));
		topicMetadatas.add(new TopicMetadata(new kafka.api.TopicMetadata(EXISTING_TOPIC, partitionMetadatas.toSeq(),
				ErrorMapping.NoError())));

		MetadataCache metadataCache = new MetadataCache(Collections.<TopicMetadata>emptySet());

		assertThat(metadataCache.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC), nullValue());


		MetadataCache newMetadataCache = metadataCache.merge(topicMetadatas);

		// merging MetadataCache with new topic metadata results in an object that also has the new data
		assertThat(newMetadataCache.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(newMetadataCache.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(newMetadataCache.getPartitions(EXISTING_TOPIC), hasSize(2));
		assertThat(newMetadataCache.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 0)));
		assertThat(newMetadataCache.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 1)));
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)), notNullValue());
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)).getHost(), equalTo("host0"));
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)).getPort(), equalTo(1000));
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)), notNullValue());
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)).getHost(), equalTo("host1"));
		assertThat(newMetadataCache.getLeader(new Partition(EXISTING_TOPIC, 1)).getPort(), equalTo(1001));
	}

	@Test
	public void testMetadataCacheMergedWithOverride() throws Exception {
		List<TopicMetadata> topicMetadatas = new ArrayList<TopicMetadata>();
		HashSet<PartitionMetadata> partitionMetadatas = HashSet$.MODULE$.empty();
		partitionMetadatas.$plus$eq(new PartitionMetadata(0, asScalaOption(new Broker(0, "host0", 1000)), emptySeq(),
				emptySeq(), ErrorMapping.NoError()));
		partitionMetadatas.$plus$eq(new PartitionMetadata(1, asScalaOption(new Broker(0, "host1", 1001)), emptySeq(),
				emptySeq(), ErrorMapping.NoError()));
		topicMetadatas.add(new TopicMetadata(new kafka.api.TopicMetadata(EXISTING_TOPIC, partitionMetadatas.toSeq(),
				ErrorMapping.NoError())));

		MetadataCache metadataCache = new MetadataCache(Collections.<TopicMetadata>emptySet());

		//empty MetadataCache has no data
		assertThat(metadataCache.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC, 0)), nullValue());
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC), nullValue());
		assertThat(metadataCache.getLeader(new Partition(EXISTING_TOPIC_2, 0)), nullValue());
		assertThat(metadataCache.getPartitions(EXISTING_TOPIC_2), nullValue());

		MetadataCache metadataCacheWithTopic1 = metadataCache.merge(topicMetadatas);

		// merging MetadataCache with new topic metadata results in an object that also has the new data
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCacheWithTopic1.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCacheWithTopic1.getPartitions(EXISTING_TOPIC), hasSize(2));
		assertThat(metadataCacheWithTopic1.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 0)));
		assertThat(metadataCacheWithTopic1.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 1)));
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 0)), notNullValue());
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 0)).getHost(), equalTo("host0"));
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 0)).getPort(), equalTo(1000));
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 1)), notNullValue());
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 1)).getHost(), equalTo("host1"));
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 1)).getPort(), equalTo(1001));
		assertThat(metadataCacheWithTopic1.getLeader(new Partition(EXISTING_TOPIC, 2)), nullValue());

		ArrayList<TopicMetadata> topic2Metadatas = new ArrayList<TopicMetadata>();
		HashSet<PartitionMetadata> topic2partitionMetadatas = HashSet$.MODULE$.empty();
		topic2partitionMetadatas.$plus$eq(new PartitionMetadata(0, asScalaOption(new Broker(0, "host1", 1002)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		topic2partitionMetadatas.$plus$eq(new PartitionMetadata(1, asScalaOption(new Broker(0, "host2", 1003)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		topic2partitionMetadatas.$plus$eq(new PartitionMetadata(2, asScalaOption(new Broker(0, "host3", 1004)), emptySeq(),
				emptySeq(), ErrorMapping.NoError()));
		topic2Metadatas.add(new TopicMetadata(new kafka.api.TopicMetadata(EXISTING_TOPIC_2, topic2partitionMetadatas.toSeq(),
				ErrorMapping.NoError())));
		MetadataCache metadataCacheWithTopic2 = metadataCacheWithTopic1.merge(topic2Metadatas);


		assertThat(metadataCacheWithTopic2.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCacheWithTopic2.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC), hasSize(2));
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 0)));
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 1)));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 0)), notNullValue());
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 0)).getHost(), equalTo("host0"));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 0)).getPort(), equalTo(1000));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 1)), notNullValue());
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 1)).getHost(), equalTo("host1"));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 1)).getPort(), equalTo(1001));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC, 2)), nullValue());
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC_2), notNullValue());
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC_2), hasSize(3));
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 0)));
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 1)));
		assertThat(metadataCacheWithTopic2.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 2)));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 0)), notNullValue());
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 0)).getHost(), equalTo("host1"));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 0)).getPort(), equalTo(1002));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 1)), notNullValue());
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 1)).getHost(), equalTo("host2"));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 1)).getPort(), equalTo(1003));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 2)), notNullValue());
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 2)).getHost(), equalTo("host3"));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 2)).getPort(), equalTo(1004));
		assertThat(metadataCacheWithTopic2.getLeader(new Partition(EXISTING_TOPIC_2, 3)), nullValue());

		// merging overrides data
		List<TopicMetadata> overridingTopicMetadatas = new ArrayList<TopicMetadata>();
		HashSet<PartitionMetadata> overridingPartitionsMetadatas = HashSet$.MODULE$.empty();
		overridingPartitionsMetadatas.$plus$eq(new PartitionMetadata(0, asScalaOption(new Broker(0, "host10", 2000)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		overridingPartitionsMetadatas.$plus$eq(new PartitionMetadata(1, asScalaOption(new Broker(0, "host11", 2001)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		overridingPartitionsMetadatas.$plus$eq(new PartitionMetadata(2, asScalaOption(new Broker(0, "host12", 2002)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		overridingPartitionsMetadatas.$plus$eq(new PartitionMetadata(3, asScalaOption(new Broker(0, "host13", 2003)),
				emptySeq(), emptySeq(), ErrorMapping.NoError()));
		overridingTopicMetadatas.add(new TopicMetadata(new kafka.api.TopicMetadata(EXISTING_TOPIC,
				overridingPartitionsMetadatas.toSeq(), ErrorMapping.NoError())));

		MetadataCache metadataCacheOverridden = metadataCacheWithTopic2.merge(overridingTopicMetadatas);

		assertThat(metadataCacheOverridden.getLeader(new Partition(NONEXISTENT_TOPIC, 0)), nullValue());
		assertThat(metadataCacheOverridden.getPartitions(NONEXISTENT_TOPIC), nullValue());
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC), hasSize(4));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 0)));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 1)));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 2)));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC), hasItem(new Partition(EXISTING_TOPIC, 3)));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 0)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 0)).getHost(), equalTo("host10"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 0)).getPort(), equalTo(2000));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 1)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 1)).getHost(), equalTo("host11"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 1)).getPort(), equalTo(2001));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 2)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 2)).getHost(), equalTo("host12"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 2)).getPort(), equalTo(2002));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 3)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 3)).getHost(), equalTo("host13"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 3)).getPort(), equalTo(2003));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC, 4)), nullValue());
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC_2), hasSize(3));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 0)));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 1)));
		assertThat(metadataCacheOverridden.getPartitions(EXISTING_TOPIC_2), hasItem(new Partition(EXISTING_TOPIC_2, 2)));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 0)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 0)).getHost(), equalTo("host1"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 0)).getPort(), equalTo(1002));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 1)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 1)).getHost(), equalTo("host2"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 1)).getPort(), equalTo(1003));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 2)), notNullValue());
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 2)).getHost(), equalTo("host3"));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 2)).getPort(), equalTo(1004));
		assertThat(metadataCacheOverridden.getLeader(new Partition(EXISTING_TOPIC_2, 3)), nullValue());

	}

	public Seq<Broker> emptySeq() {
		return HashSet$.MODULE$.<Broker>empty().toSeq();
	}

	public <T> Some<T> asScalaOption(T object) {
		return new Some<T>(object);
	}

}
