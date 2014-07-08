/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.splitter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class SplitterIntegrationTests {

	@Autowired
	MessageChannel inAnnotated;

	@Autowired
	MessageChannel inMethodInvoking;

	@Autowired
	MessageChannel inDefault;

	@Autowired
	MessageChannel inDelimiters;

	@Autowired
	@Qualifier("splitter.handler")
	MethodInvokingSplitter splitter;

	private String sentence = "The quick brown fox jumped over the lazy dog";

	private List<String> words = Arrays.asList(sentence.split("\\s"));

	@Autowired
	Receiver receiver;

	@Before
	public void clearWords() {
		receiver.receivedWords.clear();
	}

	@MessageEndpoint
	public static class Receiver {
		private List<String> receivedWords = new ArrayList<String>();

		@ServiceActivator(inputChannel = "out")
		public void deliveredWords(String string) {
			this.receivedWords.add(string);
		}
	}

	@MessageEndpoint
	public static class TestSplitter {

		@Splitter(inputChannel = "inAnnotated", outputChannel = "out")
		public Iterator<String> split(String sentence) {
			return Arrays.asList(sentence.split("\\s")).iterator();
		}
	}


	@Test
	public void configOk() throws Exception {
		// just checking the parsing
	}

	@Test
	public void annotated() throws Exception {
		inAnnotated.send(new GenericMessage<String>(sentence));
		assertTrue(this.receiver.receivedWords.containsAll(words));
		assertTrue(words.containsAll(this.receiver.receivedWords));
	}

	@Test
	public void methodInvoking() throws Exception {
		inMethodInvoking.send(new GenericMessage<String>(sentence));
		assertTrue(receiver.receivedWords.containsAll(words));
		assertTrue(words.containsAll(this.receiver.receivedWords));
	}

	@Test
	public void defaultSplitter() throws Exception {
		inDefault.send(new GenericMessage<List<String>>(words));
		assertTrue(receiver.receivedWords.containsAll(words));
		assertTrue(words.containsAll(receiver.receivedWords));
	}

	@Test
	public void delimiterSplitter() throws Exception {
		inDelimiters.send(new GenericMessage<String>("one,two, three; four/five"));
		assertTrue(receiver.receivedWords.containsAll(Arrays.asList("one", "two", "three", "four", "five")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithRef() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidRef.xml", SplitterIntegrationTests.class);
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertNotNull(cause);
			assertTrue(cause instanceof IllegalArgumentException);
			assertTrue(cause.getMessage().contains("'delimiters' property is only available"));
			throw cause;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithInnerBean() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidInnerBean.xml", SplitterIntegrationTests.class);
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertNotNull(cause);
			assertTrue(cause instanceof IllegalArgumentException);
			assertTrue(cause.getMessage().contains("'delimiters' property is only available"));
			throw cause;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithExpression() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidExpression.xml", SplitterIntegrationTests.class);
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertNotNull(cause);
			assertTrue(cause instanceof IllegalArgumentException);
			assertTrue(cause.getMessage().contains("'delimiters' property is only available"));
			throw cause;
		}
	}

	@Test
	public void channelResolver_isNotNull() throws Exception {
		splitter.setOutputChannel(null);
		Message<String> message = MessageBuilder.withPayload("fooBar")
				.setReplyChannelName("out").build();
		inMethodInvoking.send(message);
	}

}
