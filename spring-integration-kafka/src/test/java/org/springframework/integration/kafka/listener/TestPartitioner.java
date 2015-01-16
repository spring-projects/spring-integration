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


package org.springframework.integration.kafka.listener;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

/**
 * @author Marius Bogoevici
 */
public class TestPartitioner implements Partitioner {

	public TestPartitioner(VerifiableProperties properties) {
	}

	@Override
	public int partition(Object key, int numPartitions) {
		if (key != null) {
			if (key instanceof Number) {
				return ((Number) key).intValue() % numPartitions;
			}
			else {
				try {
					return Integer.parseInt(key.toString());
				}
				catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		else {
			return 0;
		}
	}
}
