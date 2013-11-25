/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.jayway.jsonpath.Criteria;
import com.jayway.jsonpath.Filter;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
@ContextConfiguration(classes = JsonPathTests.JsonPathTestsContextConfiguration.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class JsonPathTests {

	private static File JSON_FILE;

	private static String JSON;

	private static Message<String> testMessage;

	@BeforeClass
	public static void setUp() throws IOException {
		ClassPathResource jsonResource = new ClassPathResource("JsonPathTests.json", JsonPathTests.class);
		JSON_FILE = jsonResource.getFile();
		Scanner scanner = new Scanner(JSON_FILE);
		JSON = scanner.useDelimiter("\\Z").next();
		scanner.close();
		testMessage = new GenericMessage<String>(JSON);
	}

	@Autowired
	private PollableChannel output;

	@Autowired
	private volatile MessageChannel transformerInput;

	@Autowired
	private volatile MessageChannel filterInput1;

	@Autowired
	private PollableChannel discardChannel;

	@Autowired
	private volatile MessageChannel filterInput2;

	@Autowired
	private volatile MessageChannel filterInput3;

	@Autowired
	private volatile MessageChannel filterInput4;

	@Autowired
	private volatile MessageChannel splitterInput;

	@Autowired
	private PollableChannel splitterOutput;

	@Autowired
	private volatile MessageChannel routerInput;

	@Autowired
	private PollableChannel routerOutput1;

	@Autowired
	private PollableChannel routerOutput2;


	@Test
	public void testInt3139JsonPathTransformer() throws IOException {
		this.transformerInput.send(testMessage);
		Message<?> receive = this.output.receive(1000);
		assertNotNull(receive);
		assertEquals("Nigel Rees", receive.getPayload());

		this.transformerInput.send(new GenericMessage<File>(JSON_FILE));
		receive = this.output.receive(1000);
		assertNotNull(receive);

		assertEquals("Nigel Rees", receive.getPayload());
		try {
			this.transformerInput.send(new GenericMessage<Object>(new Object()));
			fail("IllegalArgumentException expected");
		}
		catch (Exception e) {
			//MessageTransformationException / MessageHandlingException / InvocationTargetException / IllegalArgumentException
			Throwable cause = e.getCause().getCause().getCause();
			assertTrue(cause instanceof IllegalArgumentException);
			assertEquals("Invalid container object", cause.getMessage());
		}
	}

	@Test
	public void testInt3139JsonPathFilter() {
		this.filterInput1.send(testMessage);
		Message<?> receive = this.output.receive(1000);
		assertNotNull(receive);
		assertEquals(JSON, receive.getPayload());

		this.filterInput2.send(testMessage);
		receive = this.output.receive(1000);
		assertNotNull(receive);

		Message<String> message = MessageBuilder.withPayload(JSON)
				.setHeader("price", 10)
				.build();
		this.filterInput3.send(message);
		receive = this.output.receive(1000);
		assertNotNull(receive);

		this.filterInput4.send(testMessage);
		receive = this.output.receive(1000);
		assertNotNull(receive);

		try {
			this.filterInput1.send(new GenericMessage<String>("{foo:{}}"));
			fail("MessageRejectedException is expected.");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageRejectedException.class));
		}
		receive = this.output.receive(0);
		assertNull(receive);

		receive = this.discardChannel.receive(1000);
		assertNotNull(receive);

	}

	@Test
	public void testInt3139JsonPathSplitter() {
	  this.splitterInput.send(testMessage);
		for(int i = 0; i < 3; i++) {
			Message<?> receive = this.splitterOutput.receive(1000);
			assertNotNull(receive);
			assertTrue(receive.getPayload() instanceof Map);
		}
	}

	@Test
	public void testInt3139JsonPathRouter() {
		Message<String> message = MessageBuilder.withPayload(JSON)
				.setHeader("jsonPath", "$.store.book[0].category")
				.build();
		this.routerInput.send(message);
		Message<?> receive = this.routerOutput1.receive(1000);
		assertNotNull(receive);
		assertEquals(JSON, receive.getPayload());
		assertNull(this.routerOutput2.receive(10));

		message = MessageBuilder.withPayload(JSON)
				.setHeader("jsonPath", "$.store.book[2].category")
				.build();
		this.routerInput.send(message);
		receive = this.routerOutput2.receive(1000);
		assertNotNull(receive);
		assertEquals(JSON, receive.getPayload());
		assertNull(this.routerOutput1.receive(10));
	}


	@Configuration
	@ImportResource("classpath:org/springframework/integration/json/JsonPathTests-context.xml")
	public static class JsonPathTestsContextConfiguration {

		@Bean
		public Filter<?> jsonPathFilter() {
			return  Filter.filter(Criteria.where("isbn").exists(true).and("category").ne("fiction"));
		}

	}

}
