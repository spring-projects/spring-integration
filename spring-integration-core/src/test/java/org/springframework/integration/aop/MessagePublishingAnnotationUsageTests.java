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

package org.springframework.integration.aop;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MessagePublishingAnnotationUsageTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel channel;

	@Test
	public void headerWithExplicitName() {
		String name = this.testBean.defaultPayload("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("last")).isEqualTo("Doe");
	}

	@Test
	public void headerWithImplicitName() {
		String name = this.testBean.defaultPayloadButExplicitAnnotation("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("lname")).isEqualTo("Doe");
	}

	@Test
	public void payloadAsArgument() {
		String name = this.testBean.argumentAsPayload("John", "Doe");
		assertThat(name).isNotNull();
		assertThat(name).isEqualTo("John Doe");
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John");
		assertThat(message.getHeaders().get("lname")).isEqualTo("Doe");
	}

	public static class TestBean {

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		public String defaultPayload(String fname, @Header("last") String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		@Payload
		public String defaultPayloadButExplicitAnnotation(String fname, @Header String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel = "messagePublishingAnnotationUsageTestChannel")
		public String argumentAsPayload(@Payload String fname, @Header String lname) {
			return fname + " " + lname;
		}

	}

}
