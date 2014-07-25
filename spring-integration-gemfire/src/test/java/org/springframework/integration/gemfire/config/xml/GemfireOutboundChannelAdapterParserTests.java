/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.gemfire.config.xml;

import static org.junit.Assert.assertEquals;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.createFakeParserContext;
import static org.springframework.integration.gemfire.config.xml.ParserTestUtil.loadXMLFrom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Oxlade
 * @author Liujiong
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GemfireOutboundChannelAdapterParserTests {

	private GemfireOutboundChannelAdapterParser underTest = new GemfireOutboundChannelAdapterParser();

	private volatile static int adviceCalled;

	@Autowired
	@Qualifier("adapter")
	ConsumerEndpointFactoryBean adapter1;

	@Autowired
	ApplicationContext ctx;

	@Test(expected = BeanDefinitionParsingException.class)
	public void regionIsARequiredAttribute() throws Exception {
		String xml = "<outbound-channel-adapter />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.parseConsumer(element, createFakeParserContext());
	}

	@Test
	public void withAdvice() {
		adapter1.start();
		MessageChannel channel = ctx.getBean("input", MessageChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testPhase() {
		assertEquals(2, adapter1.getPhase());
	}

	@Test
	public void testAutoStart() {
		assertEquals(false, adapter1.isAutoStartup());
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
