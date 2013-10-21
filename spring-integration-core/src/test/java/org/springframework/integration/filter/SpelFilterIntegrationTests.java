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

package org.springframework.integration.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelFilterIntegrationTests {

	@Autowired
	private MessageChannel simpleInput;

	@Autowired
	private PollableChannel positives;

	@Autowired
	private PollableChannel negatives;

	@Autowired
	private MessageChannel beanResolvingInput;

	@Autowired
	private PollableChannel evens;

	@Autowired
	private PollableChannel odds;


	@Test
	public void simpleExpressionBasedFilter() {
		this.simpleInput.send(new GenericMessage<Integer>(1));
		this.simpleInput.send(new GenericMessage<Integer>(0));
		this.simpleInput.send(new GenericMessage<Integer>(99));
		this.simpleInput.send(new GenericMessage<Integer>(-99));
		assertEquals(new Integer(1), positives.receive(0).getPayload());
		assertEquals(new Integer(99), positives.receive(0).getPayload());
		assertEquals(new Integer(0), negatives.receive(0).getPayload());
		assertEquals(new Integer(-99), negatives.receive(0).getPayload());
		assertNull(positives.receive(0));
		assertNull(negatives.receive(0));
	}

	@Test
	public void beanResolvingExpressionBasedFilter() {
		this.beanResolvingInput.send(new GenericMessage<Integer>(1));
		this.beanResolvingInput.send(new GenericMessage<Integer>(2));
		this.beanResolvingInput.send(new GenericMessage<Integer>(9));
		this.beanResolvingInput.send(new GenericMessage<Integer>(22));
		assertEquals(new Integer(1), odds.receive(0).getPayload());
		assertEquals(new Integer(9), odds.receive(0).getPayload());
		assertEquals(new Integer(2), evens.receive(0).getPayload());
		assertEquals(new Integer(22), evens.receive(0).getPayload());
		assertNull(odds.receive(0));
		assertNull(evens.receive(0));
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		public boolean isEven(int number) {
			return number % 2 == 0;
		}
	}

}
