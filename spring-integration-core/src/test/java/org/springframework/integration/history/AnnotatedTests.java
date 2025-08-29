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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

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

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class AnnotatedTests {

	@Test
	public void testHistoryWithAnnotatedComponents() throws Exception {
		ClassPathXmlApplicationContext ac = new ClassPathXmlApplicationContext("annotated-config.xml", this.getClass());
		CompletableFuture<Message<?>> eventMessage = new CompletableFuture<>();
		ApplicationListener<ApplicationEvent> listener =
				event -> {
					eventMessage.complete((Message<?>) event.getSource());

				};
		ac.addApplicationListener(listener);

		MessageChannel channel = ac.getBean("inputChannel", MessageChannel.class);
		EventDrivenConsumer consumer = ac.getBean("myAdapter", EventDrivenConsumer.class);
		MessageHandler handler = (MessageHandler) TestUtils.getPropertyValue(consumer, "handler");
		Field handlerField = consumer.getClass().getDeclaredField("handler");
		handlerField.setAccessible(true);
		handlerField.set(consumer, handler);
		channel.send(new GenericMessage<>("hello"));

		assertThat(eventMessage).succeedsWithin(Duration.ofSeconds(10))
				.extracting(MessageHistory::read)
				.extracting(history -> history.get(1))
				.extracting("name", "type").containsExactly("myAdapter", "method-outbound-channel-adapter");

		ac.removeApplicationListener(listener);
		ac.close();
	}

}
