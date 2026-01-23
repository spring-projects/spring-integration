/*
 * Copyright 2012-present the original author or authors.
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.smb.AbstractBaseTests;
import org.springframework.integration.smb.session.SmbSessionFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Markus Spann
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Prafull Kumar Soni
 * @author Gregory Bragg
 * @author Glenn Renfro
 */
public class SmbOutboundChannelAdapterParserTests extends AbstractBaseTests {

	@Test
	public void testSmbOutboundChannelAdapterComplete() {
		try (ClassPathXmlApplicationContext ac = getApplicationContext()) {
			Object consumer = ac.getBean("smbOutboundChannelAdapter");
			assertThat(consumer).isInstanceOf(EventDrivenConsumer.class);

			PublishSubscribeChannel channel = ac.getBean("smbPubSubChannel", PublishSubscribeChannel.class);
			assertThat(channel).isEqualTo(TestUtils.getPropertyValue(consumer, "inputChannel"));
			assertThat("smbOutboundChannelAdapter").isEqualTo(((EventDrivenConsumer) consumer).getComponentName());

			Object messageHandler = TestUtils.getPropertyValue(consumer, "handler");
			String remoteFileSeparator =
					TestUtils.<String>getPropertyValue(messageHandler, "remoteFileTemplate.remoteFileSeparator");
			assertThat(remoteFileSeparator).isNotNull();
			assertThat(".working.tmp")
					.isEqualTo(TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.temporaryFileSuffix"));
			assertThat(".").isEqualTo(remoteFileSeparator);
			assertThat(ac.getBean("fileNameGenerator"))
					.isEqualTo(TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.fileNameGenerator"));
			assertThat(StandardCharsets.UTF_8)
					.isEqualTo(TestUtils.<Charset>getPropertyValue(messageHandler, "remoteFileTemplate.charset"));

			Object sessionFactoryProp = TestUtils.getPropertyValue(messageHandler, "remoteFileTemplate.sessionFactory");
			assertThat(SmbSessionFactory.class).isEqualTo(sessionFactoryProp.getClass());

			SmbSessionFactory smbSessionFactory = (SmbSessionFactory) sessionFactoryProp;
			assertThat("localhost").isEqualTo(TestUtils.getPropertyValue(smbSessionFactory, "host"));
			assertThat(0).isEqualTo(TestUtils.getPropertyValue(smbSessionFactory, "port"));
			assertThat(23).isEqualTo(TestUtils.getPropertyValue(messageHandler, "order"));

			// verify subscription order
			@SuppressWarnings("unchecked")
			Set<MessageHandler> handlers =
					TestUtils.getPropertyValue(TestUtils.getPropertyValue(channel, "dispatcher"), "handlers");
			Iterator<MessageHandler> iterator = handlers.iterator();
			assertThat(TestUtils.<Object>getPropertyValue(ac.getBean("smbOutboundChannelAdapter2"), "handler"))
					.isSameAs(iterator.next());
			assertThat(messageHandler).isSameAs(iterator.next());
		}
	}

	@Test
	public void noCachingByDefault() {
		try (ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext(
				getApplicationContextXmlFile(), this.getClass())) {

			Object adapter = ac.getBean("simpleAdapter");
			Object sfProperty = TestUtils.getPropertyValue(adapter, "handler.remoteFileTemplate.sessionFactory");
			assertThat(SmbSessionFactory.class).isEqualTo(sfProperty.getClass());
		}
	}

}
