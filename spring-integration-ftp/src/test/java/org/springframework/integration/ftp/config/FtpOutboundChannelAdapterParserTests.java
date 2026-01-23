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

package org.springframework.integration.ftp.config;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
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
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
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
	private PollingConsumer ftpOutbound3;

	@Autowired
	private PublishSubscribeChannel ftpChannel;

	@Autowired
	private FileNameGenerator fileNameGenerator;

	@Test
	public void testFtpOutboundChannelAdapterComplete() {
		assertThat(TestUtils.<PublishSubscribeChannel>getPropertyValue(ftpOutbound, "inputChannel"))
				.isEqualTo(ftpChannel);
		assertThat(ftpOutbound.getComponentName()).isEqualTo("ftpOutbound");
		FileTransferringMessageHandler<?> handler =
				TestUtils.getPropertyValue(ftpOutbound, "handler");
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.remoteFileSeparator");
		assertThat(remoteFileSeparator).isNotNull();
		assertThat(TestUtils.<String>getPropertyValue(handler, "remoteFileTemplate.temporaryFileSuffix"))
				.isEqualTo(".foo");
		assertThat(remoteFileSeparator).isEqualTo("");
		assertThat(TestUtils.<FileNameGenerator>getPropertyValue(handler,
				"remoteFileTemplate.fileNameGenerator")).isEqualTo(this.fileNameGenerator);
		assertThat(TestUtils.<Object>getPropertyValue(handler, "remoteFileTemplate.charset"))
				.isEqualTo(StandardCharsets.UTF_8);
		assertThat(TestUtils.<Object>getPropertyValue(handler,
				"remoteFileTemplate.directoryExpressionProcessor")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(handler,
				"remoteFileTemplate.temporaryDirectoryExpressionProcessor")).isNotNull();
		assertThat(TestUtils.<FtpRemoteFileTemplate.ExistsMode>getPropertyValue(handler,
				"remoteFileTemplate.existsMode")).isEqualTo(FtpRemoteFileTemplate.ExistsMode.NLST);
		Object sfProperty = TestUtils.getPropertyValue(handler, "remoteFileTemplate.sessionFactory");
		assertThat(sfProperty.getClass()).isEqualTo(DefaultFtpSessionFactory.class);
		DefaultFtpSessionFactory sessionFactory = (DefaultFtpSessionFactory) sfProperty;
		assertThat(TestUtils.<String>getPropertyValue(sessionFactory, "host")).isEqualTo("localhost");
		assertThat(TestUtils.<Integer>getPropertyValue(sessionFactory, "port")).isEqualTo(22);
		assertThat(TestUtils.<Integer>getPropertyValue(handler, "order")).isEqualTo(23);
		//verify subscription order
		Object dispatcher = TestUtils.getPropertyValue(ftpChannel, "dispatcher");
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils.getPropertyValue(dispatcher, "handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertThat(iterator.next()).isSameAs(TestUtils.getPropertyValue(this.ftpOutbound2, "handler"));
		assertThat(iterator.next()).isSameAs(handler);
		assertThat(TestUtils.<FileExistsMode>getPropertyValue(ftpOutbound, "handler.mode"))
				.isEqualTo(FileExistsMode.APPEND);
	}

	@Test
	public void testFailWithEmptyRfsAndAcdTrue() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("FtpOutboundChannelAdapterParserTests-fail.xml",
								getClass()));
	}

	@Test
	public void cachingByDefault() {
		Object sfProperty = TestUtils.getPropertyValue(simpleAdapter, "handler.remoteFileTemplate.sessionFactory");
		assertThat(sfProperty.getClass()).isEqualTo(CachingSessionFactory.class);
		Object innerSfProperty = TestUtils.getPropertyValue(sfProperty, "sessionFactory");
		assertThat(innerSfProperty.getClass()).isEqualTo(DefaultFtpSessionFactory.class);
		assertThat(TestUtils.<FileExistsMode>getPropertyValue(simpleAdapter, "handler.mode"))
				.isEqualTo(FileExistsMode.REPLACE);
	}

	@Test
	public void adviceChain() {
		MessageHandler handler = TestUtils.getPropertyValue(advisedAdapter, "handler");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testTemporaryFileSuffix() {
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(ftpOutbound3, "handler");
		assertThat(TestUtils.<Boolean>getPropertyValue(handler, "remoteFileTemplate.useTemporaryFileName"))
				.isFalse();
	}

	@Test
	public void testBeanExpressions() {
		FileTransferringMessageHandler<?> handler = TestUtils.getPropertyValue(withBeanExpressions, "handler");
		ExpressionEvaluatingMessageProcessor<?> dirExpProc = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.directoryExpressionProcessor");
		assertThat(dirExpProc).isNotNull();
		Message<String> message = MessageBuilder.withPayload("qux").build();
		assertThat(dirExpProc.processMessage(message)).isEqualTo("foo");
		ExpressionEvaluatingMessageProcessor<?> tempDirExpProc = TestUtils.getPropertyValue(handler,
				"remoteFileTemplate.temporaryDirectoryExpressionProcessor");
		assertThat(tempDirExpProc).isNotNull();
		assertThat(tempDirExpProc.processMessage(message)).isEqualTo("bar");
		DefaultFileNameGenerator generator = TestUtils.getPropertyValue(handler, "remoteFileTemplate.fileNameGenerator");
		assertThat(generator).isNotNull();
		assertThat(generator.generateFileName(message)).isEqualTo("baz");
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
