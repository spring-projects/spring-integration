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
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MessagePublishingInterceptorUsageTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel channel;

	@Test
	public void demoMessagePublishingInterceptor() {
		String name = this.testBean.setName("John", "Doe");
		assertThat(name).isNotNull();
		Message<?> message = this.channel.receive(1000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("John Doe");
		assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
	}

	public static class TestBean {

		public String setName(String fname, String lname) {
			return fname + " " + lname;
		}

	}

}
