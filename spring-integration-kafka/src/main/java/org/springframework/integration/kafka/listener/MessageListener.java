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

import org.springframework.integration.kafka.core.KafkaMessage;

/**
 * Listener for handling incoming Kafka messages
 *
 * @author Marius Bogoevici
 */
public interface MessageListener {

	/**
	 * Executes when a Kafka message is received
	 * @param message the Kafka message to be processed
	 */
	void onMessage(KafkaMessage message);

}
