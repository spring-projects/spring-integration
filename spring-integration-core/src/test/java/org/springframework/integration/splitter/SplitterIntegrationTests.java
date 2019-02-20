/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
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

	private final String sentence = "The quick brown fox jumped over the lazy dog";

	private final List<String> words = Arrays.asList(sentence.split("\\s"));

	@Autowired
	Receiver receiver;

	@Before
	public void clearWords() {
		receiver.receivedWords.clear();
	}

	@MessageEndpoint
	public static class Receiver {
		private final List<String> receivedWords = new ArrayList<String>();

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
		assertThat(this.receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(this.receiver.receivedWords)).isTrue();
	}

	@Test
	public void methodInvoking() throws Exception {
		inMethodInvoking.send(new GenericMessage<String>(sentence));
		assertThat(receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(this.receiver.receivedWords)).isTrue();
	}

	@Test
	public void defaultSplitter() throws Exception {
		inDefault.send(new GenericMessage<List<String>>(words));
		assertThat(receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(receiver.receivedWords)).isTrue();
	}

	@Test
	public void delimiterSplitter() throws Exception {
		inDelimiters.send(new GenericMessage<String>("one,two, three; four/five"));
		assertThat(receiver.receivedWords.containsAll(Arrays.asList("one", "two", "three", "four", "five"))).isTrue();
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithRef() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidRef.xml",
					SplitterIntegrationTests.class).close();
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertThat(cause).isNotNull();
			assertThat(cause instanceof IllegalArgumentException).isTrue();
			assertThat(cause.getMessage().contains("'delimiters' property is only available")).isTrue();
			throw cause;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithInnerBean() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidInnerBean.xml",
					SplitterIntegrationTests.class).close();
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertThat(cause).isNotNull();
			assertThat(cause instanceof IllegalArgumentException).isTrue();
			assertThat(cause.getMessage().contains("'delimiters' property is only available")).isTrue();
			throw cause;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void delimitersNotAllowedWithExpression() throws Throwable {
		try {
			new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidExpression.xml",
					SplitterIntegrationTests.class).close();
		}
		catch (BeanCreationException e) {
			Throwable cause = e.getMostSpecificCause();
			assertThat(cause).isNotNull();
			assertThat(cause instanceof IllegalArgumentException).isTrue();
			assertThat(cause.getMessage().contains("'delimiters' property is only available")).isTrue();
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
