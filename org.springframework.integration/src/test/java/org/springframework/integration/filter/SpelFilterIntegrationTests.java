/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SpelFilterIntegrationTests {

	@Autowired @Qualifier("input")
	private MessageChannel input;

	@Autowired @Qualifier("positives")
	private PollableChannel positives;

	@Autowired @Qualifier("discards")
	private PollableChannel discards;


	@Test
	public void filter() {
		this.input.send(new GenericMessage<Integer>(1));
		this.input.send(new GenericMessage<Integer>(0));
		this.input.send(new GenericMessage<Integer>(99));
		this.input.send(new GenericMessage<Integer>(-99));
		assertEquals(new Integer(1), positives.receive(0).getPayload());
		assertEquals(new Integer(99), positives.receive(0).getPayload());
		assertEquals(new Integer(0), discards.receive(0).getPayload());
		assertEquals(new Integer(-99), discards.receive(0).getPayload());
		assertNull(positives.receive(0));
		assertNull(discards.receive(0));
	}

}
