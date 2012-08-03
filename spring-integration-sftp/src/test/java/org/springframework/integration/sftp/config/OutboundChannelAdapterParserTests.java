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

package org.springframework.integration.sftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.Message;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author David Turanski
 */
public class OutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testOutboundChannelAdapterWithId(){
		ApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapter");
		assertTrue(consumer instanceof EventDrivenConsumer);
		PublishSubscribeChannel channel = context.getBean("inputChannel", PublishSubscribeChannel.class);
		assertEquals(channel, TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("sftpOutboundAdapter", ((EventDrivenConsumer)consumer).getComponentName());
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(handler, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals(".", remoteFileSeparator);
		assertEquals(".bar", TestUtils.getPropertyValue(handler, "temporaryFileSuffix", String.class));
		Expression remoteDirectoryExpression = (Expression) TestUtils.getPropertyValue(handler, "directoryExpressionProcessor.expression");
		assertNotNull(remoteDirectoryExpression);
		assertTrue(remoteDirectoryExpression instanceof LiteralExpression);
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectoryExpressionProcessor"));
		assertEquals(context.getBean("fileNameGenerator"), TestUtils.getPropertyValue(handler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectory"));
		CachingSessionFactory<?> sessionFactory = TestUtils.getPropertyValue(handler, "sessionFactory", CachingSessionFactory.class);
		DefaultSftpSessionFactory clientFactory = TestUtils.getPropertyValue(sessionFactory, "sessionFactory", DefaultSftpSessionFactory.class);
		assertEquals("localhost", TestUtils.getPropertyValue(clientFactory, "host"));
		assertEquals(2222, TestUtils.getPropertyValue(clientFactory, "port"));
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
		//verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(channel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(TestUtils.getPropertyValue(context.getBean("sftpOutboundAdapterWithExpression"), "handler"), iterator.next());
		assertSame(handler, iterator.next());
	}

	@Test
	public void testOutboundChannelAdapterWithWithRemoteDirectoryAndFileExpression(){
		ApplicationContext context =
			new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapterWithExpression");
		assertTrue(consumer instanceof EventDrivenConsumer);
		assertEquals(context.getBean("inputChannel"), TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("sftpOutboundAdapterWithExpression", ((EventDrivenConsumer)consumer).getComponentName());
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		SpelExpression remoteDirectoryExpression = (SpelExpression) TestUtils.getPropertyValue(handler, "directoryExpressionProcessor.expression");
		assertNotNull(remoteDirectoryExpression);
		assertEquals("'foo' + '/' + 'bar'", remoteDirectoryExpression.getExpressionString());
		FileNameGenerator generator = (FileNameGenerator) TestUtils.getPropertyValue(handler, "fileNameGenerator");
		String fileNameGeneratorExpression = (String) TestUtils.getPropertyValue(generator, "expression");
		assertEquals("payload.getName() + '-foo'", fileNameGeneratorExpression);
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectory"));
		assertNull(TestUtils.getPropertyValue(handler, "temporaryDirectoryExpressionProcessor"));

	}

	@Test
	public void testOutboundChannelAdapterWithNoTemporaryFileName(){
		ApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapterWithNoTemporaryFileName");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		assertFalse((Boolean)TestUtils.getPropertyValue(handler,"useTemporaryFileName"));
	}

	@Test
	public void advised(){
		ApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("advised");
		MessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testFailWithRemoteDirAndExpression(){
		new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context-fail.xml", this.getClass());

	}

	@Test(expected=BeanDefinitionStoreException.class)
	public void testFailWithFileExpressionAndFileGenerator(){
		new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context-fail-fileFileGen.xml", this.getClass());

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
