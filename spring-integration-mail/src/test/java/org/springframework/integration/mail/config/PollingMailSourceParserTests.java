/*
 * Copyright 2002-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import javax.mail.URLName;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class PollingMailSourceParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void imapAdapter() {
		Object adapter = context.getBean("imapAdapter");
		assertThat(adapter.getClass()).isEqualTo(SourcePollingChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(channel);
		Object source = adapterAccessor.getPropertyValue("source");
		assertThat(source.getClass()).isEqualTo(MailReceivingMessageSource.class);
		Object receiver = new DirectFieldAccessor(source).getPropertyValue("mailReceiver");
		assertThat(receiver.getClass()).isEqualTo(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
	}

	@Test
	public void pop3Adapter() {
		Object adapter = context.getBean("pop3Adapter");
		assertThat(adapter.getClass()).isEqualTo(SourcePollingChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isEqualTo(channel);
		Object source = adapterAccessor.getPropertyValue("source");
		assertThat(source.getClass()).isEqualTo(MailReceivingMessageSource.class);
		Object receiver = new DirectFieldAccessor(source).getPropertyValue("mailReceiver");
		assertThat(receiver.getClass()).isEqualTo(Pop3MailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("pop3:bar"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
	}

}
