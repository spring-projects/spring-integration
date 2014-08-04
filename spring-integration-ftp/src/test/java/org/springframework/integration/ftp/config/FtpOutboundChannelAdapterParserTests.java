/*
 * Copyright 2002-2014 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FtpOutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Autowired
	private EventDrivenConsumer simpleAdapter;

	@Autowired
	private EventDrivenConsumer advisedAdapter;

	@Autowired
	private EventDrivenConsumer withBeanExpressions;

	@Autowired
	private EventDrivenConsumer ftpOutbound;

	@Autowired
	private EventDrivenConsumer ftpOutbound2;

	@Autowired
	private EventDrivenConsumer ftpOutbound3;

	@Autowired
	private PublishSubscribeChannel ftpChannel;

	@Autowired
	private FileNameGenerator fileNameGenerator;

	@Test
	public void testFtpOutboundChannelAdapterComplete() throws Exception{
		assertEquals(ftpChannel, TestUtils.getPropertyValue(ftpOutbound, "inputChannel"));
		assertEquals("ftpOutbound", ftpOutbound.getComponentName());
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(ftpOutbound, "handler", FileTransferringMessageHandler.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(handler, "remoteFileTemplate.remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals(".foo", TestUtils.getPropertyValue(handler, "remoteFileTemplate.temporaryFileSuffix", String.class));
		assertEquals("", remoteFileSeparator);
		assertEquals(this.fileNameGenerator, TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(handler, "remoteFileTemplate.charset"));
		assertNotNull(TestUtils.getPropertyValue(handler, "remoteFileTemplate.directoryExpressionProcessor"));
		assertNotNull(TestUtils.getPropertyValue(handler, "remoteFileTemplate.temporaryDirectoryExpressionProcessor"));
		Object sfProperty = TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory");
		assertEquals(DefaultFtpSessionFactory.class, sfProperty.getClass());
		DefaultFtpSessionFactory sessionFactory = (DefaultFtpSessionFactory) sfProperty;
		assertEquals("localhost", TestUtils.getPropertyValue(sessionFactory, "host"));
		assertEquals(22, TestUtils.getPropertyValue(sessionFactory, "port"));
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
		//verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(ftpChannel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(TestUtils.getPropertyValue(this.ftpOutbound2, "handler"), iterator.next());
		assertSame(handler, iterator.next());
		assertEquals(FileExistsMode.APPEND, TestUtils.getPropertyValue(ftpOutbound, "handler.mode"));
	}

	@SuppressWarnings("resource")
	@Test(expected=BeanCreationException.class)
	public void testFailWithEmptyRfsAndAcdTrue() throws Exception{
		new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-fail.xml", this.getClass());
	}

	@Test
	public void cachingByDefault() {
		Object sfProperty = TestUtils.getPropertyValue(simpleAdapter, "handler.remoteFileTemplate.sessionFactory");
		assertEquals(CachingSessionFactory.class, sfProperty.getClass());
		Object innerSfProperty = TestUtils.getPropertyValue(sfProperty, "sessionFactory");
		assertEquals(DefaultFtpSessionFactory.class, innerSfProperty.getClass());
		assertEquals(FileExistsMode.REPLACE, TestUtils.getPropertyValue(simpleAdapter, "handler.mode"));
	}

	@Test
	public void adviceChain() {
		MessageHandler handler = TestUtils.getPropertyValue(advisedAdapter, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@Test
	public void testTemporaryFileSuffix() {
		FileTransferringMessageHandler<?> handler =
				(FileTransferringMessageHandler<?>)TestUtils.getPropertyValue(ftpOutbound3, "handler");
		assertFalse((Boolean)TestUtils.getPropertyValue(handler,"remoteFileTemplate.useTemporaryFileName"));
	}

	@Test
	public void testBeanExpressions() throws Exception{
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(withBeanExpressions, "handler", FileTransferringMessageHandler.class);
		ExpressionEvaluatingMessageProcessor<?> dirExpProc = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.directoryExpressionProcessor", ExpressionEvaluatingMessageProcessor.class);
		assertNotNull(dirExpProc);
		Message<String> message = MessageBuilder.withPayload("qux").build();
		assertEquals("foo", dirExpProc.processMessage(message));
		ExpressionEvaluatingMessageProcessor<?> tempDirExpProc = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.temporaryDirectoryExpressionProcessor", ExpressionEvaluatingMessageProcessor.class);
		assertNotNull(tempDirExpProc);
		assertEquals("bar", tempDirExpProc.processMessage(message));
		DefaultFileNameGenerator generator = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.fileNameGenerator", DefaultFileNameGenerator.class);
		assertNotNull(generator);
		assertEquals("baz", generator.generateFileName(message));
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
