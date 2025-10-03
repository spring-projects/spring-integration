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

package org.springframework.integration.mail.config;

import java.util.Properties;
import java.util.stream.Stream;

import jakarta.mail.URLName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.inbound.ImapMailReceiver;
import org.springframework.integration.mail.inbound.MailReceiver;
import org.springframework.integration.mail.inbound.MailReceivingMessageSource;
import org.springframework.integration.mail.inbound.Pop3MailReceiver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Ma Jiandong
 */
@SpringJUnitConfig
public class PollingMailSourceParserTests {

	@Autowired
	private ApplicationContext context;

	static Stream<Arguments> methodArguments() {
		return Stream.of(
				Arguments.arguments("imapAdapter", ImapMailReceiver.class, "imap:foo"),
				Arguments.arguments("pop3Adapter", Pop3MailReceiver.class, "pop3:bar")
		);
	}

	@ParameterizedTest
	@MethodSource("methodArguments")
	public void inboundChannelAdaptorTest(String adapterName, Class<MailReceiver> mailReceiverClass, String storeUri) {
		Object adapter = context.getBean(adapterName);
		assertThat(adapter.getClass()).isEqualTo(SourcePollingChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(channel);
		Object source = adapterAccessor.getPropertyValue("source");
		assertThat(source.getClass()).isEqualTo(MailReceivingMessageSource.class);
		Object receiver = new DirectFieldAccessor(source).getPropertyValue("mailReceiver");
		assertThat(receiver.getClass()).isEqualTo(mailReceiverClass);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName(storeUri));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
	}

}
