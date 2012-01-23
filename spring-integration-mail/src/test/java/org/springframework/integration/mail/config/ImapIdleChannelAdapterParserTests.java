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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Properties;

import javax.mail.URLName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.mail.ImapIdleChannelAdapter;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ImapIdleChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private ImapIdleChannelAdapter autoChannelAdapter;

	@Test
	public void simpleAdapter() {
		Object adapter = context.getBean("simpleAdapter");
		assertEquals(ImapIdleChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertSame(channel, adapterAccessor.getPropertyValue("outputChannel"));
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals(0, properties.size());
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldDeleteMessages"));
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead"));
		assertNull(adapterAccessor.getPropertyValue("errorChannel"));
	}
	@Test
	public void simpleAdapterWithErrorChannel() {
		Object adapter = context.getBean("simpleAdapterWithErrorChannel");
		assertEquals(ImapIdleChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertSame(channel, adapterAccessor.getPropertyValue("outputChannel"));
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals(0, properties.size());
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldDeleteMessages"));
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead"));
		assertSame(context.getBean("errorChannel"), adapterAccessor.getPropertyValue("errorChannel"));
	}
	@Test
	public void simpleAdapterWithMarkeMessagesAsRead() {
		Object adapter = context.getBean("simpleAdapterMarkAsRead");
		assertEquals(ImapIdleChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertSame(channel, adapterAccessor.getPropertyValue("outputChannel"));
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals(0, properties.size());
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldDeleteMessages"));
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead"));
	}
	
	@Test
	public void simpleAdapterWithMarkeMessagesAsReadFalse() {
		Object adapter = context.getBean("simpleAdapterMarkAsReadFalse");
		assertEquals(ImapIdleChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertSame(channel, adapterAccessor.getPropertyValue("outputChannel"));
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals(0, properties.size());
		assertEquals(Boolean.TRUE, receiverAccessor.getPropertyValue("shouldDeleteMessages"));
		assertEquals(Boolean.FALSE, receiverAccessor.getPropertyValue("shouldMarkMessagesAsRead"));
	}

	@Test
	public void customAdapter() {
		Object adapter = context.getBean("customAdapter");
		assertEquals(ImapIdleChannelAdapter.class, adapter.getClass());
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		Object channel = context.getBean("channel");
		assertSame(channel, adapterAccessor.getPropertyValue("outputChannel"));
		assertEquals(Boolean.FALSE, adapterAccessor.getPropertyValue("autoStartup"));
		Object receiver = adapterAccessor.getPropertyValue("mailReceiver");
		assertEquals(ImapMailReceiver.class, receiver.getClass());
		DirectFieldAccessor receiverAccessor = new DirectFieldAccessor(receiver);
		Object url = receiverAccessor.getPropertyValue("url");
		assertEquals(new URLName("imap:foo"), url);
		Properties properties = (Properties) receiverAccessor.getPropertyValue("javaMailProperties");
		assertEquals("bar", properties.getProperty("foo"));
		assertEquals(Boolean.FALSE, receiverAccessor.getPropertyValue("shouldDeleteMessages"));
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}
}
