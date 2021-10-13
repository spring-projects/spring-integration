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

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.URLName;
import javax.mail.search.SearchTerm;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.SearchTermStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ImapIdleChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private ImapIdleChannelAdapter autoChannelAdapter;

	@Test
	public void simpleAdapter() {
		Object adapter = context.getBean("simpleAdapter");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		// mail.imap(s).peek properties
		assertThat(properties).hasSize(2);
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead")).isEqualTo(Boolean.TRUE);
		assertThat(adapterAccessor.getPropertyValue("errorChannel")).isNull();
		assertThat(adapterAccessor.getPropertyValue("adviceChain")).isNull();
		assertThat(receiverAccessor.getPropertyValue("embeddedPartsAsBytes")).isEqualTo(Boolean.FALSE);
		assertThat(receiverAccessor.getPropertyValue("headerMapper")).isNotNull();
		assertThat(receiverAccessor.getPropertyValue("simpleContent")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("autoCloseFolder")).isEqualTo(false);
		assertThat(receiverAccessor.getPropertyValue("cancelIdleInterval")).isEqualTo(202000L);
	}

	@Test
	public void simpleAdapterWithErrorChannel() {
		Object adapter = context.getBean("simpleAdapterWithErrorChannel");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		// mail.imap(s).peek properties
		assertThat(properties).hasSize(2);
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead")).isEqualTo(Boolean.TRUE);
		assertThat(adapterAccessor.getPropertyValue("errorChannel")).isSameAs(context.getBean("errorChannel"));
		assertThat(receiverAccessor.getPropertyValue("embeddedPartsAsBytes")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("headerMapper")).isNull();
		assertThat(receiverAccessor.getPropertyValue("simpleContent")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void simpleAdapterWithMarkMessagesAsRead() {
		Object adapter = this.context.getBean("simpleAdapterMarkAsRead");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = this.context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		// mail.imap(s).peek properties
		assertThat(properties).hasSize(2);
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("userFlag")).isEqualTo("flagged");
	}

	@Test
	public void simpleAdapterWithMarkMessagesAsReadFalse() {
		Object adapter = context.getBean("simpleAdapterMarkAsReadFalse");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		// mail.imap(s).peek properties
		assertThat(properties).hasSize(2);
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void customAdapter() {
		Object adapter = context.getBean("customAdapter");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertThat(properties.getProperty("foo")).isEqualTo("bar");
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.FALSE);
		SearchTermStrategy stStrategy = context.getBean("searchTermStrategy", SearchTermStrategy.class);
		assertThat(TestUtils.getPropertyValue(adapter, "mailReceiver.searchTermStrategy")).isEqualTo(stStrategy);
	}

	@Test
	public void testAutoChannel() {
		assertThat(TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel")).isSameAs(autoChannel);
	}

	@Test
	public void transactionalAdapter() {
		Object adapter = context.getBean("transactionalAdapter");
		assertThat(adapter).isInstanceOf(ImapIdleChannelAdapter.class);
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertThat(adapterAccessor.getPropertyValue("outputChannel")).isSameAs(channel);
		assertThat(adapterAccessor.getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertThat(receiver).isInstanceOf(ImapMailReceiver.class);
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertThat(url).isEqualTo(new URLName("imap:foo"));
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		// mail.imap(s).peek properties
		assertThat(properties).hasSize(2);
		assertThat(receiverAccessor.getPropertyValue("shouldDeleteMessages")).isEqualTo(Boolean.TRUE);
		assertThat(receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead")).isEqualTo(Boolean.TRUE);
		assertThat(adapterAccessor.getPropertyValue("errorChannel")).isNull();
		assertThat(adapterAccessor.getPropertyValue("sendingTaskExecutor")).isEqualTo(context.getBean("executor"));
		assertThat(adapterAccessor.getPropertyValue("adviceChain")).isNotNull();
	}

	public static class TestSearchTermStrategy implements SearchTermStrategy {

		@Override
		public SearchTerm generateSearchTerm(Flags supportedFlags, Folder folder) {
			return null;
		}

	}

}
