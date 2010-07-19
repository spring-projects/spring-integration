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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class OperationInvokingChannelAdapterParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private TestBean testBean;

	@After
	public void resetLists() {
		testBean.messages.clear();
	}


	@Test
	public void adapterWithDefaults() throws Exception {
		assertEquals(0, testBean.messages.size());
		input.send(new StringMessage("test1"));
		input.send(new StringMessage("test2"));
		input.send(new StringMessage("test3"));
		assertEquals(3, testBean.messages.size());
	}
	
	@Test
	// Headers should be ignored
	public void adapterWitJmxHeaders() throws Exception {
		assertEquals(0, testBean.messages.size());
		input.send(this.createMessage("1"));
		input.send(this.createMessage("2"));
		input.send(this.createMessage("3"));
		assertEquals(3, testBean.messages.size());
	}
	
	private Message<?> createMessage(String payload){
		return MessageBuilder.withPayload(payload)
			.setHeader(JmxHeaders.OBJECT_NAME, "org.springframework.integration.jmx.config:type=TestBean,name=foo")
			.setHeader(JmxHeaders.OPERATION_NAME, "blah").build();
	}
}
