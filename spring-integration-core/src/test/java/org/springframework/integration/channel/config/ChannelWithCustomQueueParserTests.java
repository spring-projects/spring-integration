/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.channel.config;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testcases for detailed namespace support for &lt;queue/> element under
 * &lt;channel/>
 *
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @see ChannelWithCustomQueueParserTests
 */
@SpringJUnitConfig
@DirtiesContext
public class ChannelWithCustomQueueParserTests {

	@Qualifier("customQueueChannel")
	@Autowired
	QueueChannel customQueueChannel;

	@Test
	public void parseConfig() {
		assertThat(customQueueChannel).isNotNull();
	}

	@Test
	public void queueTypeSet() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(customQueueChannel);
		Object queue = accessor.getPropertyValue("queue");
		assertThat(queue).isNotNull();
		assertThat(queue).isInstanceOf(ArrayBlockingQueue.class);
		assertThat(((BlockingQueue<?>) queue).remainingCapacity()).isEqualTo(2);
	}

}
