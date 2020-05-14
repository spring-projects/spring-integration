/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Option;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.ftp.gateway.FtpOutboundGateway;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

/**
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class FtpOutboundGatewayParserTests {

	@Autowired
	AbstractEndpoint gateway1;

	@Autowired
	AbstractEndpoint gateway2;

	@Autowired
	AbstractEndpoint gateway3;

	@Autowired
	AbstractEndpoint gateway4;

	@Autowired
	AbstractEndpoint withBeanExpression;

	@Autowired
	FileNameGenerator generator;

	private static volatile int adviceCalled;

	@Test
	public void testGateway1() {
		FtpOutboundGateway gateway = TestUtils.getPropertyValue(gateway1,
				"handler", FtpOutboundGateway.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.remoteFileSeparator")).isEqualTo("X");
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "localDirectoryExpression.literalValue"))
				.isEqualTo("local-test-dir");
		assertThat(TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(gateway, "filter")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "command")).isEqualTo(Command.LS);

		@SuppressWarnings("unchecked")
		Set<Option> options = TestUtils.getPropertyValue(gateway, "options", Set.class);
		assertThat(options.contains(Option.NAME_ONLY)).isTrue();
		assertThat(options.contains(Option.NOSORT)).isTrue();

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout", Long.class);
		assertThat(sendTimeout).isEqualTo(Long.valueOf(777));
		assertThat(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(gateway, "mputFilter")).isInstanceOf(ExpressionFileListFilter.class);
		assertThat(TestUtils.getPropertyValue(gateway, "fileExistsMode")).isEqualTo(FileExistsMode.APPEND);
	}

	@Test
	public void testGateway2() throws Exception {
		FtpOutboundGateway gateway = TestUtils.getPropertyValue(gateway2,
				"handler", FtpOutboundGateway.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.remoteFileSeparator")).isEqualTo("X");
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory"))
				.isInstanceOf(CachingSessionFactory.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.existsMode"))
				.isEqualTo(FtpRemoteFileTemplate.ExistsMode.NLST);
		assertThat(TestUtils.getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "localDirectoryExpression.literalValue"))
				.isEqualTo("local-test-dir");
		assertThat(TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(gateway, "command")).isEqualTo(Command.GET);
		@SuppressWarnings("unchecked")
		Set<String> options = TestUtils.getPropertyValue(gateway, "options", Set.class);
		assertThat(options.contains(Option.PRESERVE_TIMESTAMP)).isTrue();
		gateway.handleMessage(new GenericMessage<String>("foo"));
		assertThat(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class)).isFalse();
		assertThat(adviceCalled).isEqualTo(1);

		//INT-3129
		assertThat(TestUtils.getPropertyValue(gateway, "localFilenameGeneratorExpression")).isNotNull();
		final AtomicReference<Method> genMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(FtpOutboundGateway.class, method -> {
			method.setAccessible(true);
			genMethod.set(method);
		}, method -> "generateLocalFileName".equals(method.getName()));
		assertThat(genMethod.get().invoke(gateway, new GenericMessage<String>(""), "foo")).isEqualTo("FOO.afoo");
		assertThat(TestUtils.getPropertyValue(gateway, "mputFilter")).isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	public void testGatewayMv() {
		FtpOutboundGateway gateway = TestUtils.getPropertyValue(gateway3,
				"handler", FtpOutboundGateway.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "command")).isEqualTo(Command.MV);
		assertThat(TestUtils.getPropertyValue(gateway, "renameProcessor.expression.expression")).isEqualTo("'foo'");
	}

	@Test
	public void testGatewayMPut() {
		FtpOutboundGateway gateway = TestUtils.getPropertyValue(gateway4,
				"handler", FtpOutboundGateway.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.getPropertyValue(gateway, "command")).isEqualTo(Command.MPUT);
		assertThat(TestUtils.getPropertyValue(gateway, "renameProcessor.expression.expression")).isEqualTo("'foo'");
		assertThat(TestUtils.getPropertyValue(gateway, "mputFilter")).isInstanceOf(RegexPatternFileListFilter.class);
		assertThat(TestUtils.getPropertyValue(gateway, "remoteFileTemplate.fileNameGenerator")).isSameAs(generator);
		assertThat(TestUtils
				.getPropertyValue(gateway, "remoteFileTemplate.directoryExpressionProcessor.expression",
						Expression.class)
				.getExpressionString()).isEqualTo("/foo");
		assertThat(TestUtils
				.getPropertyValue(gateway, "remoteFileTemplate.temporaryDirectoryExpressionProcessor.expression",
						Expression.class)
				.getExpressionString()).isEqualTo("/bar");
	}

	@Test
	public void testWithBeanExpression() {
		FtpOutboundGateway gateway = TestUtils.getPropertyValue(withBeanExpression,
				"handler", FtpOutboundGateway.class);
		ExpressionEvaluatingMessageProcessor<?> processor = TestUtils.getPropertyValue(gateway, "fileNameProcessor",
				ExpressionEvaluatingMessageProcessor.class);
		assertThat(processor).isNotNull();
		assertThat(processor.processMessage(MessageBuilder.withPayload("bar").build())).isEqualTo("foo");
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}
}
