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

package org.springframework.integration.aop;

import org.junit.Assert;

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
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AnnotationConfigRegistrationTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private QueueChannel annotationConfigRegistrationTest;

	@Autowired
	private QueueChannel defaultChannel;


	@Test // INT-1200
	public void verifyInterception() {
		String name = testBean.setName("John", "Doe", 123);
		Assert.assertNotNull(name);
		Message<?> message = annotationConfigRegistrationTest.receive(0);
		Assert.assertNotNull(message);
		Assert.assertEquals("John DoeDoe", message.getPayload());
		Assert.assertEquals(123, message.getHeaders().get("x"));
	}

	@Test
	public void defaultChannel() {
		String result = testBean.exclaim("hello");
		Assert.assertNotNull(result);
		Assert.assertEquals("HELLO!!!", result);
		Message<?> message = defaultChannel.receive(0);
		Assert.assertNotNull(message);
		Assert.assertEquals("HELLO!!!", message.getPayload());
	}


	public static class TestBean {

		@Publisher(channel="annotationConfigRegistrationTest")
		@Payload("#return + #args.lname")
		public String setName(String fname, String lname, @Header("x") int num) {
			return fname + " " + lname;
		}

		@Publisher
		public String exclaim(String s) {
			return s.toUpperCase() + "!!!";
		}
	}

}
