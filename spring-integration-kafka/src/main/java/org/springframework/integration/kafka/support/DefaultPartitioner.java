/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.kafka.support;

import kafka.producer.Partitioner;
import kafka.utils.Utils;

/**
 * @author Soby Chacko
 * @since 0.5
 *
 * This class is for internal use only and therefore is at default access level
 */
class DefaultPartitioner implements Partitioner {
	/**
	 * Uses the key to calculate a partition bucket id for routing
	 * the data to the appropriate broker partition
	 * @return an integer between 0 and numPartitions-1
	 */
	@Override
	public int partition(final Object key, final int numPartitions) {
		return Utils.abs(key.hashCode()) % numPartitions;
	}

}
