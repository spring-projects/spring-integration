/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jms.request_reply;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.gateway.RequestReplyExchanger;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.config.ActiveMqTestUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
/**
 * @author Oleg Zhurakousky
 */
public class RequestReplyScenariosWithCorrelationKeyProvidedTests {

	@Test
	public void messageCorrelationBasedCustomCorrelationKey() throws Exception{
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGateway", RequestReplyExchanger.class);

		gateway.exchange(MessageBuilder.withPayload("foo").build());
		context.close();
	}

	@Test
	public void messageCorrelationBasedCustomCorrelationKeyAsJMSCorrelationID() throws Exception{
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGatewayB", RequestReplyExchanger.class);

		gateway.exchange(MessageBuilder.withPayload("foo").build());
		context.close();
	}

	@Test
	public void messageCorrelationBasedCustomCorrelationKeyDelayedReplies() throws Exception{
		ActiveMqTestUtils.prepare();

		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("explicit-correlation-key.xml", this.getClass());
		RequestReplyExchanger gateway = context.getBean("explicitCorrelationKeyGatewayC", RequestReplyExchanger.class);


		for (int i = 0; i < 3; i++) {
			try {
				gateway.exchange(MessageBuilder.withPayload("hello").build());
			} catch (Exception e) {
				// ignore
			}
		}

		JmsOutboundGateway outGateway = TestUtils.getPropertyValue(context.getBean("outGateway"), "handler", JmsOutboundGateway.class);
		outGateway.setReceiveTimeout(5000);
		assertEquals("foo", gateway.exchange(MessageBuilder.withPayload("foo").build()).getPayload());
		context.close();
	}


	public static class DelayedService {
		public String echo(String s) throws Exception{
			Thread.sleep(200);
			return s;
		}
	}
}
