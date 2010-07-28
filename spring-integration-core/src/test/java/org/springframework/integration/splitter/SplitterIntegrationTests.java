/*
 * Copyright 2002-2008 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Splitter;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Alexander Peters
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class SplitterIntegrationTests {

	@Autowired
	@Qualifier("inAnnotated")
	MessageChannel inAnnotated;

	@Autowired
	@Qualifier("inMethodInvoking")
	MessageChannel inMethodInvoking;

	@Autowired
	@Qualifier("inDefault")
	MessageChannel inDefault;
	
	@Autowired
	MethodInvokingSplitter splitter;

	private String sentence = "The quick brown fox jumped over the lazy dog";

	private List<String> words = Arrays.asList(sentence.split("\\s"));

	@Autowired
	Receiver receiver;

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
		public List<String> split(String sentence) {
			return Arrays.asList(sentence.split("\\s"));
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
	public void channelResolver_isNotNull() throws Exception {
		splitter.setOutputChannel(null);
		Message<String> message = MessageBuilder.withPayload("fooBar")
				.setReplyChannelName("out").build();
		inMethodInvoking.send(message);
	}

}
