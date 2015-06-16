/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.integration.ip.tcp;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class TcpSendingNoSocketTests {

	@Autowired
	private MessageChannel shouldFail;

	@Autowired
	private MessageChannel advised;

	@Test
	public void exceptionExpected() {
		try {
			shouldFail.send(new GenericMessage<String>("foo"));
			fail("Exception expected");
		}
		catch (MessageHandlingException e) {
			assertThat(e.getMessage(), startsWith("Unable to find outbound socket"));
		}
	}

	@Test
	public void exceptionTrapped() {
		advised.send(new GenericMessage<String>("foo"));
	}
}
