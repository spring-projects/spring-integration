/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.mail.config;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import javax.mail.URLName;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.integration.mail.Pop3MailReceiver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class PollingMailSourceParserTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void imapAdapter() {
		Object adapter = context.getBean("imapAdapter");
		assertEquals(SourcePollingChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object channel = context.getBean("channel");
		assertEquals(channel, adapterAccessor.getPropertyValue("outputChannel"));
		Object source = adapterAccessor.getPropertyValue("source");
		assertEquals(MailReceivingMessageSource.class, source.getClass());
		Object receiver = new DirectFieldAccessor(source).getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals("bar", properties.getProperty("foo"));
	}

	@Test
	public void pop3Adapter() {
		Object adapter = context.getBean("pop3Adapter");
		assertEquals(SourcePollingChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object channel = context.getBean("channel");
		assertEquals(channel, adapterAccessor.getPropertyValue("outputChannel"));
		Object source = adapterAccessor.getPropertyValue("source");
		assertEquals(MailReceivingMessageSource.class, source.getClass());
		Object receiver = new DirectFieldAccessor(source).getPropertyValue("mailReceiver");
		assertEquals(Pop3MailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("pop3:bar"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals("bar", properties.getProperty("foo"));
	}

}
