/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.handler.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.w3c.dom.Element;

/**
 * @author Dan Oxlade
 */
public class GemfireOutboundChannelAdapterParserTests {

	private GemfireOutboundChannelAdapterParser underTest = new GemfireOutboundChannelAdapterParser();

	private volatile static int adviceCalled;

	@Test(expected = BeanDefinitionParsingException.class)
	public void regionIsARequiredAttribute() throws Exception {
		String xml = "<outbound-channel-adapter />";
		Element element = loadXMLFrom(xml).getDocumentElement();
		underTest.parseConsumer(element, createFakeParserContext());
	}

	@Test
	public void withAdvice() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context.xml", this.getClass());
		MessageChannel channel = ctx.getBean("input", MessageChannel.class);
		channel.send(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Throwable {
			adviceCalled++;
			return null;
		}

	}
}
