/*
 * Copyright 2012-2018 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.smb.AbstractBaseTests;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;

/**
 * @author Markus Spann
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 */
public class SmbOutboundChannelAdapterParserTests extends AbstractBaseTests {

	@Test
	public void testSmbOutboundChannelAdapterComplete() {
		ApplicationContext ac = getApplicationContext();

		Object consumer = ac.getBean("smbOutboundChannelAdapter");
		assertTrue(consumer instanceof EventDrivenConsumer);

		PublishSubscribeChannel channel = ac.getBean("smbPubSubChannel", PublishSubscribeChannel.class);
		assertEquals(channel, TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("smbOutboundChannelAdapter", ((EventDrivenConsumer) consumer).getComponentName());

		Object messageHandler = TestUtils.getPropertyValue(consumer, "handler");
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals(".working.tmp", TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.temporaryFileSuffix", String.class));
		assertEquals(".", remoteFileSeparator);
		assertEquals(ac.getBean("fileNameGenerator"), TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.charset", Charset.class).name());

		Object sessionFactoryProp = TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.sessionFactory");
		assertEquals(SmbSessionFactory.class, sessionFactoryProp.getClass());

		SmbSessionFactory smbSessionFactory = (SmbSessionFactory) sessionFactoryProp;
		assertEquals("localhost", TestUtils.getPropertyValue(smbSessionFactory, "host"));
		assertEquals(0, TestUtils.getPropertyValue(smbSessionFactory, "port"));
		assertEquals(23, TestUtils.getPropertyValue(messageHandler, "order"));

		// verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(TestUtils.getPropertyValue(ac.getBean("smbOutboundChannelAdapter2"), "handler"),
				iterator.next());
		assertSame(messageHandler, iterator.next());
	}

	@Test
	public void noCachingByDefault() {
		ApplicationContext ac = new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), this.getClass());
		Object adapter = ac.getBean("simpleAdapter");
		Object sfProperty = TestUtils.getPropertyValue(adapter, "handler.remoteFileTemplate.sessionFactory");
		assertEquals(SmbSessionFactory.class, sfProperty.getClass());
	}

}
