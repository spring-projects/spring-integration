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

package org.springframework.integration.smb.config;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Command;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway.Option;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.smb.outbound.SmbOutboundGateway;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests follow original logic for testing SFTP module,
 * adapted for SMB module.
 *
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gregory Bragg
 * @author Glenn Renfro
 *
 * @since 6.0
 */
@SpringJUnitConfig
@DirtiesContext
public class SmbOutboundGatewayParserTests {

	@Autowired
	AbstractEndpoint gateway1;

	@Autowired
	AbstractEndpoint gateway2;

	@Autowired
	AbstractEndpoint gateway3;

	@Autowired
	AbstractEndpoint gateway4;

	@Autowired
	AbstractEndpoint advised;

	@Autowired
	AbstractEndpoint noExpressionLS;

	@Autowired
	AbstractEndpoint noExpressionPUT;

	@Autowired
	AbstractEndpoint noExpressionGET;

	@Autowired
	FileNameGenerator generator;

	private static volatile int adviceCalled;

	@Test
	public void testGateway1() {
		SmbOutboundGateway gateway = TestUtils.getPropertyValue(gateway1, "handler");
		assertThat(TestUtils.<String>getPropertyValue(gateway, "remoteFileTemplate.remoteFileSeparator"))
				.isEqualTo("X");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.<String>getPropertyValue(gateway, "localDirectoryExpression.literalValue"))
				.isEqualTo("local-test-dir");
		assertThat((Boolean) TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "requiresReply")).isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "filter")).isNotNull();
		assertThat(TestUtils.<Command>getPropertyValue(gateway, "command")).isEqualTo(Command.LS);
		@SuppressWarnings("unchecked")
		Set<Option> options = TestUtils.getPropertyValue(gateway, "options");
		assertThat(options.contains(Option.NAME_ONLY)).isTrue();
		assertThat(options.contains(Option.NOSORT)).isTrue();

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout");
		assertThat(sendTimeout).isEqualTo(Long.valueOf(777));
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mputFilter"))
				.isInstanceOf(RegexPatternFileListFilter.class);
	}

	@Test
	public void testGateway2() throws Exception {
		SmbOutboundGateway gateway = TestUtils.getPropertyValue(gateway2, "handler");
		assertThat(TestUtils.<String>getPropertyValue(gateway, "remoteFileTemplate.remoteFileSeparator")).isEqualTo("X");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "remoteFileTemplate.sessionFactory"))
				.isNotNull();
		assertThat(TestUtils
				.getPropertyValue(gateway, "remoteFileTemplate.sessionFactory") instanceof CachingSessionFactory)
				.isTrue();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.<String>getPropertyValue(gateway, "localDirectoryExpression.literalValue"))
				.isEqualTo("local-test-dir");
		assertThat((Boolean) TestUtils.getPropertyValue(gateway, "autoCreateLocalDirectory")).isFalse();
		assertThat(TestUtils.<Command>getPropertyValue(gateway, "command")).isEqualTo(Command.GET);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "requiresReply")).isFalse();
		@SuppressWarnings("unchecked")
		Set<String> options = TestUtils.getPropertyValue(gateway, "options");
		assertThat(options.contains(Option.PRESERVE_TIMESTAMP)).isTrue();

		//INT-3129
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "localFilenameGeneratorExpression")).isNotNull();
		final AtomicReference<Method> genMethod = new AtomicReference<>();
		ReflectionUtils.doWithMethods(SmbOutboundGateway.class, method -> {
			method.setAccessible(true);
			genMethod.set(method);
		}, method -> "generateLocalFileName".equals(method.getName()));
		assertThat(genMethod.get().invoke(gateway, new GenericMessage<String>(""), "foo")).isEqualTo("FOO.afoo");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "mputFilter"))
				.isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	public void testGatewayMv() {
		SmbOutboundGateway gateway = TestUtils.getPropertyValue(gateway3, "handler");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "remoteFileTemplate.sessionFactory"))
				.isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.<Command>getPropertyValue(gateway, "command")).isEqualTo(Command.MV);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "renameProcessor.expression.expression"))
				.isEqualTo("'foo'");
	}

	@Test
	public void testGatewayMPut() {
		SmbOutboundGateway gateway = TestUtils.getPropertyValue(gateway4, "handler");
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "remoteFileTemplate.sessionFactory")).isNotNull();
		assertThat(TestUtils.<Object>getPropertyValue(gateway, "outputChannel")).isNotNull();
		assertThat(TestUtils.<Command>getPropertyValue(gateway, "command")).isEqualTo(Command.MPUT);
		assertThat(TestUtils.<String>getPropertyValue(gateway, "renameProcessor.expression.expression")).isEqualTo("'foo'");
		assertThat(TestUtils.<RegexPatternFileListFilter>getPropertyValue(gateway, "mputFilter")).isInstanceOf(RegexPatternFileListFilter.class);
		assertThat(TestUtils.<FileNameGenerator>getPropertyValue(gateway, "remoteFileTemplate.fileNameGenerator")).isSameAs(generator);
		assertThat(TestUtils
				.<Expression>getPropertyValue(gateway,
						"remoteFileTemplate.directoryExpressionProcessor.expression")
				.getExpressionString()).isEqualTo("/foo");
		assertThat(TestUtils
				.<Expression>getPropertyValue(gateway,
						"remoteFileTemplate.temporaryDirectoryExpressionProcessor.expression")
				.getExpressionString()).isEqualTo("/bar");
	}

	@Test
	public void advised() {
		SmbOutboundGateway gateway = TestUtils.getPropertyValue(advised, "handler");
		gateway.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	void noExpression() {
		assertThat(TestUtils.<Object>getPropertyValue(this.noExpressionLS, "handler.fileNameProcessor")).isNull();
		assertThat(TestUtils.<Object>getPropertyValue(this.noExpressionPUT, "handler.fileNameProcessor")).isNull();
		assertThat(TestUtils.<String>getPropertyValue(this.noExpressionGET,
				"handler.fileNameProcessor.expression.expression")).isEqualTo("payload");
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
