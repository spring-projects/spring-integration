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

package org.springframework.integration.aggregator.scenarios;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artme Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatorReplyChannelTests {

	@Autowired
	private volatile MessageChannel input;

	@Autowired
	private volatile PollableChannel output;

	private final List<String> list = new ArrayList<>();

	@Before
	public void setupList() {
		this.list.add("foo");
		this.list.add("bar");
	}

	@Test
	public void replyChannelHeader() {
		verifyReply(MessageBuilder.withPayload(list).setReplyChannel(output).build());
	}

	@Test // INT-1095
	public void replyChannelNameHeader() {
		verifyReply(MessageBuilder.withPayload(list).setReplyChannelName("output").build());
	}

	private void verifyReply(Message<?> message) {
		assertThat(this.output.receive(0)).isNull();
		this.input.send(message);
		Message<?> result = this.output.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload() instanceof List).isTrue();
		List<?> resultList = (List<?>) result.getPayload();
		assertThat(resultList.size()).isEqualTo(2);
		assertThat(resultList.contains("foo")).isTrue();
		assertThat(resultList.contains("bar")).isTrue();
	}

}
