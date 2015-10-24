/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.support;

import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * No-op implementation of @link ProducerListener}, to be used as base class for other implementations.
 *
 * @author Marius Bogoevici
 * @since 1.3
 */
public class DefaultProducerListener implements ProducerListener {

	@Override
	public void onSuccess(String topic, Integer partition, Object key, Object value, RecordMetadata recordMetadata) {
	}

	@Override
	public void onError(String topic, Integer partition, Object key, Object value, Exception exception) {
	}
}
