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

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherMethodInvokingTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void replyChannelExplicitOverwriteTrue() {
		MessageChannel inputChannel = context.getBean("input", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		inputChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("test");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
		assertThat(result.getHeaders().get("foo")).isEqualTo(123);
		assertThat(result.getHeaders().get("bar")).isEqualTo("ABC");
		assertThat(result.getHeaders().get("other")).isEqualTo("zzz");
	}

	public static class TestBean {

		public String echo(String text) {
			return text.toUpperCase();
		}

		public Map<String, Object> enrich() {
			Map<String, Object> headers = new HashMap<>();
			headers.put("foo", 123);
			headers.put("bar", "ABC");
			return headers;
		}

	}

}
