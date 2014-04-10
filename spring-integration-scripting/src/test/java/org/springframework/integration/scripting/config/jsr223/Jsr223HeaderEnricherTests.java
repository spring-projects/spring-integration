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

package org.springframework.integration.scripting.config.jsr223;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gunnar Hillert
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223HeaderEnricherTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private QueueChannel outputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private QueueChannel outputB;

	@Test
	public void referencedScript() throws Exception{
		inputA.send(new GenericMessage<String>("Hello"));
		assertEquals("jruby", outputA.receive(20000).getHeaders().get("TEST_HEADER"));
	}

	@Test
	public void inlineScript() throws Exception{
		inputB.send(new GenericMessage<String>("Hello"));
		assertEquals("js", outputB.receive(20000).getHeaders().get("TEST_HEADER"));
	}
}
