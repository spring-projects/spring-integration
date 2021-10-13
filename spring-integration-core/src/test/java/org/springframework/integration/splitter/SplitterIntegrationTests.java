/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.splitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Iwein Fuld
 * @author Alexander Peters
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
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

	@BeforeEach
	public void clearWords() {
		receiver.receivedWords.clear();
	}

	@MessageEndpoint
	public static class Receiver {

		private final List<String> receivedWords = new ArrayList<>();

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
	public void annotated() throws Exception {
		inAnnotated.send(new GenericMessage<>(sentence));
		assertThat(this.receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(this.receiver.receivedWords)).isTrue();
	}

	@Test
	public void methodInvoking() {
		inMethodInvoking.send(new GenericMessage<>(sentence));
		assertThat(receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(this.receiver.receivedWords)).isTrue();
	}

	@Test
	public void defaultSplitter() {
		inDefault.send(new GenericMessage<>(words));
		assertThat(receiver.receivedWords.containsAll(words)).isTrue();
		assertThat(words.containsAll(receiver.receivedWords)).isTrue();
	}

	@Test
	public void delimiterSplitter() {
		inDelimiters.send(new GenericMessage<>("one,two, three; four/five"));
		assertThat(receiver.receivedWords.containsAll(Arrays.asList("one", "two", "three", "four", "five"))).isTrue();
	}

	@Test
	public void delimitersNotAllowedWithRef() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidRef.xml",
								SplitterIntegrationTests.class))
				.withRootCauseExactlyInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("'delimiters' property is only available");
	}

	@Test
	public void delimitersNotAllowedWithInnerBean() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidInnerBean.xml",
								SplitterIntegrationTests.class))
				.withRootCauseExactlyInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("'delimiters' property is only available");
	}

	@Test
	public void delimitersNotAllowedWithExpression() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("SplitterIntegrationTests-invalidExpression.xml",
								SplitterIntegrationTests.class))
				.withRootCauseExactlyInstanceOf(IllegalArgumentException.class)
				.withMessageContaining("'delimiters' property is only available");
	}

	@Test
	public void channelResolver_isNotNull() {
		splitter.setOutputChannel(null);
		Message<String> message = MessageBuilder.withPayload("fooBar")
				.setReplyChannelName("out").build();
		inMethodInvoking.send(message);
	}

}
