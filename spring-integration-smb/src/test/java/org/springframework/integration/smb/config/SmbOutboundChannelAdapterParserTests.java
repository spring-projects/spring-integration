/**
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.smb.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.smb.AbstractBaseTest;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Markus Spann
 * @since 2.1.1
 */
public class SmbOutboundChannelAdapterParserTests extends AbstractBaseTest {

	@Test
	public void testSmbOutboundChannelAdapterComplete() throws Exception {
		ApplicationContext ac = getApplicationContext();

		Object consumer = ac.getBean("smbOutboundChannelAdapter");
		assertTrue(consumer instanceof EventDrivenConsumer);

		PublishSubscribeChannel channel = ac.getBean("smbPubSubChannel", PublishSubscribeChannel.class);
		assertEquals(channel, TestUtils.getPropertyValue(consumer, "inputChannel"));
		assertEquals("smbOutboundChannelAdapter", ((EventDrivenConsumer) consumer).getComponentName());

		Object messageHandler = TestUtils.getPropertyValue(consumer, "handler");
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(messageHandler, "remoteFileSeparator");
		assertNotNull(remoteFileSeparator);
		assertEquals(".working.tmp", TestUtils.getPropertyValue(messageHandler, "temporaryFileSuffix", String.class));
		assertEquals(".", remoteFileSeparator);
		assertEquals(ac.getBean("fileNameGenerator"), TestUtils.getPropertyValue(messageHandler, "fileNameGenerator"));
		assertEquals("UTF-8", TestUtils.getPropertyValue(messageHandler, "charset"));
		assertNotNull(TestUtils.getPropertyValue(messageHandler, "temporaryDirectory"));

		Object sessionFactoryProp = TestUtils.getPropertyValue(messageHandler, "sessionFactory");
		assertEquals(SmbSessionFactory.class, sessionFactoryProp.getClass());

		SmbSessionFactory smbSessionFactory = (SmbSessionFactory) sessionFactoryProp;
		assertEquals("localhost", TestUtils.getPropertyValue(smbSessionFactory, "host"));
		assertEquals(0, TestUtils.getPropertyValue(smbSessionFactory, "port"));
		assertEquals(23, TestUtils.getPropertyValue(messageHandler, "order"));

		// verify subscription order
		@SuppressWarnings("unchecked")
		Set<MessageHandler> handlers = (Set<MessageHandler>) TestUtils.getPropertyValue(TestUtils.getPropertyValue(channel, "dispatcher"), "handlers");
		Iterator<MessageHandler> iterator = handlers.iterator();
		assertSame(TestUtils.getPropertyValue(ac.getBean("smbOutboundChannelAdapter2"), "handler"), iterator.next());
		assertSame(messageHandler, iterator.next());
	}

	@Test
	public void cachingByDefault() {
		ApplicationContext ac = new ClassPathXmlApplicationContext(getApplicationContextXmlFile(), this.getClass());
		Object adapter = ac.getBean("simpleAdapter");
		Object sfProperty = TestUtils.getPropertyValue(adapter, "handler.sessionFactory");
		assertEquals(CachingSessionFactory.class, sfProperty.getClass());
		Object innerSfProperty = TestUtils.getPropertyValue(sfProperty, "sessionFactory");
		assertEquals(SmbSessionFactory.class, innerSfProperty.getClass());
	}

}
