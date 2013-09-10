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

package org.springframework.integration.mail.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.mail.internet.MimeMessage;

import org.easymock.classextension.EasyMock;
import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.message.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.core.BeanFactoryMessageChannelDestinationResolver;

/**
 * @author Mark Fisher
 */
public class MailToStringTransformerParserTests {

	@Test
	public void topLevelTransformer() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"mailToStringTransformerParserTests.xml",  this.getClass());
		MessageChannel input = new BeanFactoryMessageChannelDestinationResolver(context).resolveDestination("input");
		PollableChannel output = (PollableChannel) new BeanFactoryMessageChannelDestinationResolver(context).resolveDestination("output");
		MimeMessage mimeMessage = EasyMock.createNiceMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.getContent()).andReturn("hello");
		EasyMock.replay(mimeMessage);
		input.send(new GenericMessage<javax.mail.Message>(mimeMessage));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("hello", result.getPayload());
		EasyMock.verify(mimeMessage);
	}

	@Test
	public void transformerWithinChain() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"mailToStringTransformerWithinChain.xml",  this.getClass());
		MessageChannel input = new BeanFactoryMessageChannelDestinationResolver(context).resolveDestination("input");
		PollableChannel output = (PollableChannel) new BeanFactoryMessageChannelDestinationResolver(context).resolveDestination("output");
		MimeMessage mimeMessage = EasyMock.createNiceMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.getContent()).andReturn("foo");
		EasyMock.replay(mimeMessage);
		input.send(new GenericMessage<javax.mail.Message>(mimeMessage));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("FOO!!!", result.getPayload());
		EasyMock.verify(mimeMessage);
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void topLevelTransformerMissingInput() {
		try {
			new ClassPathXmlApplicationContext("mailToStringTransformerWithoutInputChannel.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			assertTrue(e.getMessage().contains("input-channel"));
			throw e;
		}
	}

}
