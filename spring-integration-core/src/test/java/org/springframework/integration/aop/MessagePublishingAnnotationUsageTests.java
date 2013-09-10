/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.aop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Publisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class MessagePublishingAnnotationUsageTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel channel;


	@Test
	public void headerWithExplicitName() {
		String name = testBean.defaultPayload("John", "Doe");
		assertNotNull(name);
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("John Doe", message.getPayload());
		assertEquals("Doe", message.getHeaders().get("last"));
	}

	@Test
	public void headerWithImplicitName() {
		String name = testBean.defaultPayloadButExplicitAnnotation("John", "Doe");
		assertNotNull(name);
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("John Doe", message.getPayload());
		assertEquals("Doe", message.getHeaders().get("lname"));
	}

	@Test
	public void payloadAsArgument() {
		String name = testBean.argumentAsPayload("John", "Doe");
		assertNotNull(name);
		assertEquals("John Doe", name);
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals("John", message.getPayload());
		assertEquals("Doe", message.getHeaders().get("lname"));
	}


	public static class TestBean {

		@Publisher(channel="messagePublishingAnnotationUsageTestChannel")
		public String defaultPayload(String fname, @Header("last") String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel="messagePublishingAnnotationUsageTestChannel")
		@Payload
		public String defaultPayloadButExplicitAnnotation(String fname, @Header String lname) {
			return fname + " " + lname;
		}

		@Publisher(channel="messagePublishingAnnotationUsageTestChannel")
		public String argumentAsPayload(@Payload String fname, @Header String lname) {
			return fname + " " + lname;
		}
	}

}
