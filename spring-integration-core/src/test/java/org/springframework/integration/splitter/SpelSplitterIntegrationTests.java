/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
public class SpelSplitterIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private MessageChannel dups;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired
	private MessageChannel iteratorInput;

	@Autowired
	private MessageChannel spelIteratorInput;

	@Autowired
	private PollableChannel output;

	@Test
	public void dups() {
		Message<?> message = MessageBuilder.withPayload("one one").build();
		this.dups.send(message);
		Message<?> one = output.receive(0);
		Message<?> two = output.receive(0);
		assertThat(one).isNotNull();
		assertThat(two).isNotNull();
		assertThat(one.getPayload()).isEqualTo("one");
		assertThat(two.getPayload()).isEqualTo("one");
	}

	@Test
	public void simple() {
		Message<?> message = MessageBuilder.withPayload(new TestBean()).setHeader("foo", "foo").build();
		this.simpleInput.send(message);
		Message<?> one = output.receive(0);
		Message<?> two = output.receive(0);
		Message<?> three = output.receive(0);
		Message<?> four = output.receive(0);
		assertThat(one.getPayload()).isEqualTo(1);
		assertThat(one.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(two.getPayload()).isEqualTo(2);
		assertThat(two.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(three.getPayload()).isEqualTo(3);
		assertThat(three.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(four.getPayload()).isEqualTo(4);
		assertThat(four.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(output.receive(0)).isNull();
	}

	@Test
	public void beanResolving() {
		Message<?> message = MessageBuilder.withPayload("a,b,c,d").build();
		this.beanResolvingInput.send(message);
		Message<?> a = output.receive(0);
		Message<?> b = output.receive(0);
		Message<?> c = output.receive(0);
		Message<?> d = output.receive(0);
		assertThat(a.getPayload()).isEqualTo("a");
		assertThat(b.getPayload()).isEqualTo("b");
		assertThat(c.getPayload()).isEqualTo("c");
		assertThat(d.getPayload()).isEqualTo("d");
		assertThat(output.receive(0)).isNull();
	}

	@Test
	public void iteratorSplitter() {
		this.iteratorInput.send(new GenericMessage<>("a,b,c,d"));
		Message<?> a = output.receive(0);
		Message<?> b = output.receive(0);
		Message<?> c = output.receive(0);
		Message<?> d = output.receive(0);
		assertThat(a.getPayload()).isEqualTo("a");
		assertThat(new IntegrationMessageHeaderAccessor(a).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(a).getSequenceSize()).isEqualTo(0);
		assertThat(b.getPayload()).isEqualTo("b");
		assertThat(new IntegrationMessageHeaderAccessor(b).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(b).getSequenceSize()).isEqualTo(0);
		assertThat(c.getPayload()).isEqualTo("c");
		assertThat(new IntegrationMessageHeaderAccessor(c).getSequenceNumber()).isEqualTo(3);
		assertThat(new IntegrationMessageHeaderAccessor(c).getSequenceSize()).isEqualTo(0);
		assertThat(d.getPayload()).isEqualTo("d");
		assertThat(new IntegrationMessageHeaderAccessor(d).getSequenceNumber()).isEqualTo(4);
		assertThat(new IntegrationMessageHeaderAccessor(d).getSequenceSize()).isEqualTo(0);
		assertThat(output.receive(0)).isNull();
	}

	@Test
	public void spelIteratorSplitter() {
		this.spelIteratorInput.send(new GenericMessage<>("a,b,c,d"));
		Message<?> a = output.receive(0);
		Message<?> b = output.receive(0);
		Message<?> c = output.receive(0);
		Message<?> d = output.receive(0);
		assertThat(a.getPayload()).isEqualTo("a");
		assertThat(new IntegrationMessageHeaderAccessor(a).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(a).getSequenceSize()).isEqualTo(0);
		assertThat(b.getPayload()).isEqualTo("b");
		assertThat(new IntegrationMessageHeaderAccessor(b).getSequenceNumber()).isEqualTo(2);
		assertThat(new IntegrationMessageHeaderAccessor(b).getSequenceSize()).isEqualTo(0);
		assertThat(c.getPayload()).isEqualTo("c");
		assertThat(new IntegrationMessageHeaderAccessor(c).getSequenceNumber()).isEqualTo(3);
		assertThat(new IntegrationMessageHeaderAccessor(c).getSequenceSize()).isEqualTo(0);
		assertThat(d.getPayload()).isEqualTo("d");
		assertThat(new IntegrationMessageHeaderAccessor(d).getSequenceNumber()).isEqualTo(4);
		assertThat(new IntegrationMessageHeaderAccessor(d).getSequenceSize()).isEqualTo(0);
		assertThat(output.receive(0)).isNull();
	}


	static class TestBean {

		private final List<Integer> numbers = new ArrayList<Integer>();

		TestBean() {
			for (int i = 1; i <= 10; i++) {
				this.numbers.add(i);
			}
		}

		public List<Integer> getNumbers() {
			return this.numbers;
		}

		public String[] split(String s) {
			return s.split(",");
		}

		public Iterator<String> splitIterator(String s) {
			return Arrays.asList(s.split(",")).iterator();
		}

	}

}
