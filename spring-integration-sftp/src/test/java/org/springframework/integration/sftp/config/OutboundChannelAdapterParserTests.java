/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author David Turanski
 * @author Gunnar Hillert
 */
public class OutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testOutboundChannelAdapterWithId() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapter");
		assertThat(consumer instanceof EventDrivenConsumer).isTrue();
		PublishSubscribeChannel channel = context.getBean("inputChannel", PublishSubscribeChannel.class);
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isEqualTo(channel);
		assertThat(((EventDrivenConsumer) consumer).getComponentName()).isEqualTo("sftpOutboundAdapter");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler",
				FileTransferringMessageHandler.class);
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.remoteFileSeparator");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(remoteFileSeparator).isEqualTo(".");
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.temporaryFileSuffix", String.class))
				.isEqualTo(".bar");
		Expression remoteDirectoryExpression = (Expression) TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.directoryExpressionProcessor.expression");
		assertThat(remoteDirectoryExpression).isNotNull();
		assertThat(remoteDirectoryExpression instanceof LiteralExpression).isTrue();
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.temporaryDirectoryExpressionProcessor"))
				.isNotNull();
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator"))
				.isEqualTo(context.getBean("fileNameGenerator"));
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.charset")).isEqualTo("UTF-8");
		CachingSessionFactory<?> sessionFactory = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.sessionFactory", CachingSessionFactory.class);
		DefaultSftpSessionFactory clientFactory = TestUtils.getPropertyValue(sessionFactory, "sessionFactory",
				DefaultSftpSessionFactory.class);
		assertThat(TestUtils.getPropertyValue(clientFactory, "host")).isEqualTo("localhost");
		assertThat(TestUtils.getPropertyValue(clientFactory, "port")).isEqualTo(2222);
		assertThat(TestUtils.getPropertyValue(handler, "order")).isEqualTo(23);
		//verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils
				.getPropertyValue(
						TestUtils.getPropertyValue(channel, "dispatcher"),
						"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertThat(iterator.next())
				.isSameAs(TestUtils.getPropertyValue(context.getBean("sftpOutboundAdapterWithExpression"), "handler"));
		assertThat(iterator.next()).isSameAs(handler);
		assertThat(TestUtils.getPropertyValue(handler, "chmod")).isEqualTo(384);
		context.close();
	}

	@Test
	public void testOutboundChannelAdapterWithWithRemoteDirectoryAndFileExpression() {
		ConfigurableApplicationContext context =
			new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapterWithExpression");
		assertThat(consumer instanceof EventDrivenConsumer).isTrue();
		assertThat(TestUtils.getPropertyValue(consumer, "inputChannel")).isEqualTo(context.getBean("inputChannel"));
		assertThat(((EventDrivenConsumer) consumer).getComponentName()).isEqualTo("sftpOutboundAdapterWithExpression");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		SpelExpression remoteDirectoryExpression = (SpelExpression) TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.directoryExpressionProcessor.expression");
		assertThat(remoteDirectoryExpression).isNotNull();
		assertThat(remoteDirectoryExpression.getExpressionString()).isEqualTo("'foo' + '/' + 'bar'");
		FileNameGenerator generator = (FileNameGenerator) TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator");
		Expression fileNameGeneratorExpression = TestUtils.getPropertyValue(generator, "expression", Expression.class);
		assertThat(fileNameGeneratorExpression).isNotNull();
		assertThat(fileNameGeneratorExpression.getExpressionString()).isEqualTo("payload.getName() + '-foo'");
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.charset")).isEqualTo("UTF-8");
		assertThat(TestUtils.getPropertyValue(handler, "remoteFileTemplate.temporaryDirectoryExpressionProcessor"))
				.isNull();
		context.close();
	}

	@Test
	public void testOutboundChannelAdapterWithNoTemporaryFileName() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("sftpOutboundAdapterWithNoTemporaryFileName");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler", FileTransferringMessageHandler.class);
		assertThat((Boolean) TestUtils.getPropertyValue(handler, "remoteFileTemplate.useTemporaryFileName")).isFalse();
		context.close();
	}

	@Test
	public void advised() {
		ConfigurableApplicationContext context =
				new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context.xml", this.getClass());
		Object consumer = context.getBean("advised");
		MessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
		context.close();
	}

	@Test
	public void testFailWithRemoteDirAndExpression() {
		try {
			new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context-fail.xml", this.getClass())
					.close();
			fail("Exception expected");
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getMessage()).contains("Only one of 'remote-directory'");
		}

	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testFailWithFileExpressionAndFileGenerator() {
		new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context-fail-fileFileGen.xml",
				this.getClass()).close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}
}
