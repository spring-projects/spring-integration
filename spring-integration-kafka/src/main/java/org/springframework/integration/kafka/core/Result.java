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

import java.util.Collections;
import java.util.Map;

/**
 * The result of a Kafka SimpleConsumer operation
 *
 * @author Marius Bogoevici
 */
public class Result<T> {

	private final Map<Partition, T> results;

	private final Map<Partition, Short> errors;

	Result(Map<Partition, T> results, Map<Partition, Short> errors) {
		this.results = Collections.unmodifiableMap(results);
		this.errors = Collections.unmodifiableMap(errors);
	}

	public Map<Partition, T> getResults() {
		return results;
	}

	public T getResult(Partition partition) throws IllegalArgumentException {
		if (this.results.containsKey(partition)) {
			return this.results.get(partition);
		}
		else {
			throw new IllegalArgumentException(" No result received for " + partition.toString());
		}
	}

	public Map<Partition, Short> getErrors() {
		return errors;
	}
	
	public short getError(Partition partition) throws IllegalArgumentException {
		if (this.getErrors().containsKey(partition)) {
			return this.getErrors().get(partition);
		}
		else {
			throw new IllegalArgumentException("No error received for " + partition.toString());
		}
	}

}
