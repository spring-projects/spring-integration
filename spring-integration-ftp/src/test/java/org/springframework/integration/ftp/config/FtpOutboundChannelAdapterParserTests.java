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

package org.springframework.integration.ftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.handler.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class FtpOutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testFtpOutboundChannelAdapterComplete() throws Exception{
		ApplicationContext ac =
			new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = ac.getBean("ftpOutbound");
		assertTrue(consumer instanceof EventDrivenConsumer);
		PublishSubscribeChannel channel = ac.getBean("ftpChannel", PublishSubscribeChannel.class);
		assertEquals(channel, TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("ftpOutbound", ((EventDrivenConsumer)consumer).getComponentName());
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(handler, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals(".foo", TestUtils.getPropertyValue(handler, "temporaryFileSuffix", String.class));
		assertEquals("", remoteFileSeparator);
		assertEquals(ac.getBean("fileNameGenerator"), TestUtils.getPropertyValue(handler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "directoryExpressionProcessor"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectoryExpressionProcessor"));
		assertNotNull(TestUtils.getPropertyValue(handler, "temporaryDirectory"));
		Object sfProperty = TestUtils.getPropertyValue(handler, "sessionFactory");
		assertEquals(DefaultFtpSessionFactory.class, sfProperty.getClass());
		DefaultFtpSessionFactory sessionFactory = (DefaultFtpSessionFactory) sfProperty;
		assertEquals("localhost", TestUtils.getPropertyValue(sessionFactory, "host"));
		assertEquals(22, TestUtils.getPropertyValue(sessionFactory, "port"));
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
		//verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(channel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(TestUtils.getPropertyValue(ac.getBean("ftpOutbound2"), "handler"), iterator.next());
		assertSame(handler, iterator.next());
	}

	@Test(expected=BeanCreationException.class)
	public void testFailWithEmptyRfsAndAcdTrue() throws Exception{
		new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-fail.xml", this.getClass());
	}

	@Test
	public void cachingByDefault() {
		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"FtpOutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object adapter = ac.getBean("simpleAdapter");
		Object sfProperty = TestUtils.getPropertyValue(adapter, "handler.sessionFactory");
		assertEquals(CachingSessionFactory.class, sfProperty.getClass());
		Object innerSfProperty = TestUtils.getPropertyValue(sfProperty, "sessionFactory");
		assertEquals(DefaultFtpSessionFactory.class, innerSfProperty.getClass());
	}

	@Test
	public void adviceChain() {
		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"FtpOutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object adapter = ac.getBean("advisedAdapter");
		MessageHandler handler = TestUtils.getPropertyValue(adapter, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testTemporaryFileSuffix() {
		ApplicationContext ac =
				new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-context.xml", this.getClass());
			FileTransferringMessageHandler<?> handler =
					(FileTransferringMessageHandler<?>)TestUtils.getPropertyValue(ac.getBean("ftpOutbound3"), "handler");
			assertFalse((Boolean)TestUtils.getPropertyValue(handler,"useTemporaryFileName"));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Throwable {
			adviceCalled++;
			return null;
		}

	}
}
