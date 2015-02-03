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

import java.util.Collection;
import java.util.Map;

import com.gs.collections.api.block.function.Function;
import com.gs.collections.impl.map.mutable.UnifiedMap;
import com.gs.collections.impl.utility.Iterate;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;

/**
 * Immutable store for the partition/broker mapping
 *
 * @author Marius Bogoevici
 */
class MetadataCache {

	public static final GetTopicNameFunction getTopicNameFunction = new GetTopicNameFunction();

	public static final ToIndexedPartitionMetadataFunction toIndexedPartitionMetadataFunction =
			new ToIndexedPartitionMetadataFunction();

	private final Map<String, Map<Partition, PartitionMetadata>> metadatasByTopic;

	public MetadataCache(Iterable<TopicMetadata> topicMetadatas) {
		UnifiedMap<String, Map<Partition, PartitionMetadata>> topicData = UnifiedMap.newMap();
		for (TopicMetadata topicMetadata : topicMetadatas) {
			Map<Partition, PartitionMetadata> partitionData = toPartitionMetadataMap(topicMetadata);
			topicData.put(topicMetadata.topic(), partitionData);
		}
		this.metadatasByTopic = topicData.toImmutable().castToMap();
	}

	private MetadataCache(Map<String, Map<Partition, PartitionMetadata>> metadatasByTopic) {
		this.metadatasByTopic = metadatasByTopic;
	}

	public MetadataCache merge(final Iterable<TopicMetadata> topicMetadatas) {
		UnifiedMap<String, Map<Partition, PartitionMetadata>> unifiedMap = UnifiedMap.newMap(this.metadatasByTopic);
		return new MetadataCache(Iterate.addToMap(topicMetadatas, getTopicNameFunction,
				toIndexedPartitionMetadataFunction, unifiedMap).toImmutable().castToMap());
	}

	public Collection<Partition> getPartitions(String topic) {
		if (this.metadatasByTopic.containsKey(topic)) {
			return this.metadatasByTopic.get(topic).keySet();
		} 
		else {
			return null;
		}
	}

	public BrokerAddress getLeader(Partition partition) {
		if (!this.metadatasByTopic.containsKey(partition.getTopic())) {
			return null;
		}
		Map<Partition, PartitionMetadata> partitionMetadatasForTopic = this.metadatasByTopic.get(partition.getTopic());
		if (!partitionMetadatasForTopic.containsKey(partition)) {
			return null;
		}
		return new BrokerAddress(partitionMetadatasForTopic.get(partition).leader());
	}


	private static Map<Partition, PartitionMetadata> toPartitionMetadataMap(TopicMetadata topicMetadata) {
		Map<Partition, PartitionMetadata> partitionData = UnifiedMap.newMap();
		for (PartitionMetadata partitionMetadata : topicMetadata.partitionsMetadata()) {
			partitionData.put(new Partition(topicMetadata.topic(), partitionMetadata.partitionId()), partitionMetadata);
		}
		return partitionData;
	}

	@SuppressWarnings("serial")
	private static class GetTopicNameFunction implements Function<TopicMetadata, String> {
		
		@Override
		public String valueOf(TopicMetadata object) {
			return object.topic();
		}
		
	}

	@SuppressWarnings("serial")
	private static class ToIndexedPartitionMetadataFunction implements
			Function<TopicMetadata, Map<Partition, PartitionMetadata>> {
		
		@Override
		public Map<Partition, PartitionMetadata> valueOf(TopicMetadata object) {
			return toPartitionMetadataMap(object);
		}
		
	}
	
}
