/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.sftp.config;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class OutboundChannelAdapterParserTests {

	private static volatile int adviceCalled;

	@Autowired
	ApplicationContext context;

	@Test
	public void testOutboundChannelAdapterWithId() {
		EventDrivenConsumer consumer = this.context.getBean("sftpOutboundAdapter", EventDrivenConsumer.class);
		PublishSubscribeChannel channel = this.context.getBean("inputChannel", PublishSubscribeChannel.class);
		assertThat(TestUtils.<PublishSubscribeChannel>getPropertyValue(consumer, "inputChannel")).isEqualTo(channel);
		assertThat(consumer.getComponentName()).isEqualTo("sftpOutboundAdapter");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler");
		String remoteFileSeparator = TestUtils.getPropertyValue(handler, "remoteFileTemplate.remoteFileSeparator");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(remoteFileSeparator).isEqualTo(".");
		assertThat(TestUtils.<String>getPropertyValue(handler, "remoteFileTemplate.temporaryFileSuffix"))
				.isEqualTo(".bar");
		Expression remoteDirectoryExpression =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.directoryExpressionProcessor.expression");
		assertThat(remoteDirectoryExpression)
				.isNotNull()
				.isInstanceOf(LiteralExpression.class);
		assertThat(TestUtils.<Object>getPropertyValue(handler,
				"remoteFileTemplate.temporaryDirectoryExpressionProcessor")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator"))
				.isEqualTo(this.context.getBean("fileNameGenerator"));
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.charset"))
				.isEqualTo(StandardCharsets.UTF_8);
		CachingSessionFactory<?> sessionFactory =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory");
		DefaultSftpSessionFactory clientFactory = TestUtils.getPropertyValue(sessionFactory, "sessionFactory");
		assertThat(TestUtils.<String>getPropertyValue(clientFactory, "host")).isEqualTo("localhost");
		assertThat(TestUtils.<Integer>getPropertyValue(clientFactory, "port")).isEqualTo(2222);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(23);
		//verify subscription order
		Set<MessageHandler> handlers = TestUtils.getPropertyValue(TestUtils.getPropertyValue(channel, "dispatcher"),
				"handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertThat(iterator.next())
				.isSameAs(TestUtils.getPropertyValue(
						this.context.getBean("sftpOutboundAdapterWithExpression"), "handler"));
		assertThat(iterator.next()).isSameAs(handler);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "chmod")).isEqualTo(384);
	}

	@Test
	public void testOutboundChannelAdapterWithWithRemoteDirectoryAndFileExpression() {
		EventDrivenConsumer consumer =
				this.context.getBean("sftpOutboundAdapterWithExpression", EventDrivenConsumer.class);
		assertThat(TestUtils.<Object>getPropertyValue(consumer, "inputChannel"))
				.isEqualTo(this.context.getBean("inputChannel"));
		assertThat(consumer.getComponentName()).isEqualTo("sftpOutboundAdapterWithExpression");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler");
		SpelExpression remoteDirectoryExpression =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.directoryExpressionProcessor.expression");
		assertThat(remoteDirectoryExpression).isNotNull();
		assertThat(remoteDirectoryExpression.getExpressionString()).isEqualTo("'foo' + '/' + 'bar'");
		FileNameGenerator generator =
				TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator");
		Expression fileNameGeneratorExpression = TestUtils.getPropertyValue(generator, "expression");
		assertThat(fileNameGeneratorExpression).isNotNull();
		assertThat(fileNameGeneratorExpression.getExpressionString()).isEqualTo("payload.getName() + '-foo'");
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.charset"))
				.isEqualTo(StandardCharsets.UTF_8);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.temporaryDirectoryExpressionProcessor"))
				.isNull();
	}

	@Test
	public void testOutboundChannelAdapterWithNoTemporaryFileName() {
		Object consumer = this.context.getBean("sftpOutboundAdapterWithNoTemporaryFileName");
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(consumer, "handler");
		assertThat((Boolean) TestUtils.getPropertyValue(handler, "remoteFileTemplate.useTemporaryFileName")).isFalse();
	}

	@Test
	public void advised() {
		Object consumer = this.context.getBean("advised");
		MessageHandler handler = TestUtils.getPropertyValue(consumer, "handler");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testFailWithRemoteDirAndExpression() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("OutboundChannelAdapterParserTests-context-fail.xml",
								getClass()))
				.withMessageContaining("Only one of 'remote-directory'");
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
