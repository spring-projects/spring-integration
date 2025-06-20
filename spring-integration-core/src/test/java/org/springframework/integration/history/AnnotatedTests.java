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

package org.springframework.integration.history;

import java.lang.reflect.Field;
import java.util.Properties;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
public class AnnotatedTests {

	@Test
	public void testHistoryWithAnnotatedComponents() throws Exception {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("annotated-config.xml", this.getClass());
		ApplicationListener<ApplicationEvent> listener = new ApplicationListener<ApplicationEvent>() {

			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				MessageHistory history = MessageHistory.read((Message<?>) event.getSource());
				Properties adapterHistory = history.get(1);
				assertThat(adapterHistory.get("name")).isEqualTo("myAdapter");
				assertThat(adapterHistory.get("type")).isEqualTo("outbound-channel-adapter");
			}
		};
		listener = spy(listener);
		ac.addApplicationListener(listener);

		MessageChannel channel = ac.getBean("inputChannel", MessageChannel.class);
		EventDrivenConsumer consumer = ac.getBean("myAdapter", EventDrivenConsumer.class);
		MessageHandler handler = (MessageHandler) TestUtils.getPropertyValue(consumer, "handler");
		Field handlerField = consumer.getClass().getDeclaredField("handler");
		handlerField.setAccessible(true);
		handlerField.set(consumer, handler);
		channel.send(new GenericMessage<String>("hello"));
		verify(listener, times(1)).onApplicationEvent((ApplicationEvent) Mockito.any());
		ac.close();
	}

}
