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
package org.springframework.integration.kafka.inbound;

import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.kafka.support.KafkaConsumerContext;

import java.util.List;
import java.util.Map;

/**
 * @author Soby Chacko
 * @since 0.5
 *
 */
public class KafkaHighLevelConsumerMessageSource extends IntegrationObjectSupport implements MessageSource<Map<String, Map<Integer, List<Object>>>> {

	private final KafkaConsumerContext kafkaConsumerContext;

	public KafkaHighLevelConsumerMessageSource(final KafkaConsumerContext kafkaConsumerContext) {
		this.kafkaConsumerContext = kafkaConsumerContext;
	}

	@Override
	public Message<Map<String, Map<Integer, List<Object>>>> receive() {
		return kafkaConsumerContext.receive();
	}

	@Override
	public String getComponentType() {
		return "kafka:inbound-channel-adapter";
	}
}
