/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.integration.config.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.log.LogAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan
 *
 * @since 4.3.8
 */
@SpringJUnitConfig
public class CustomMessagingAnnotationTests {

	@Autowired(required = false)
	@Qualifier("customMessagingAnnotationTests.Config.logger.logging.handler")
	private LoggingHandler loggingHandler;

	@Autowired
	private MessageChannel loggingChannel;

	@Test
	public void testLogAnnotation() {
		assertThat(this.loggingHandler).isNotNull();

		LogAccessor log = spy(TestUtils.getPropertyValue(this.loggingHandler, "messageLogger", LogAccessor.class));

		given(log.isWarnEnabled())
				.willReturn(true);

		new DirectFieldAccessor(this.loggingHandler)
				.setPropertyValue("messageLogger", log);

		this.loggingChannel.send(MessageBuilder.withPayload("foo")
				.setHeader("bar", "baz")
				.build());

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Supplier<? extends CharSequence>> argumentCaptor = ArgumentCaptor.forClass(Supplier.class);

		verify(log)
				.warn(argumentCaptor.capture());

		assertThat(argumentCaptor.getValue().get()).isEqualTo("foo for baz");
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean(name = IntegrationContextUtils.MESSAGING_ANNOTATION_POSTPROCESSOR_NAME)
		public static MessagingAnnotationPostProcessor messagingAnnotationPostProcessor(
				ConfigurableListableBeanFactory beanFactory) {

			MessagingAnnotationPostProcessor messagingAnnotationPostProcessor = new MessagingAnnotationPostProcessor();
			messagingAnnotationPostProcessor.
					addMessagingAnnotationPostProcessor(Logging.class, new LogAnnotationPostProcessor(beanFactory));
			return messagingAnnotationPostProcessor;
		}

		@Logging(value = "loggingChannel", level = LoggingHandler.Level.WARN)
		public String logger(Message<?> message) {
			return message.getPayload() + " for " + message.getHeaders().get("bar");
		}

	}

	@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	public @interface Logging {

		String value();


		LoggingHandler.Level level() default LoggingHandler.Level.INFO;

	}

	private static class LogAnnotationPostProcessor extends AbstractMethodAnnotationPostProcessor<Logging> {

		LogAnnotationPostProcessor(ConfigurableListableBeanFactory beanFactory) {
			super(beanFactory);
		}

		@Override
		protected String getInputChannelAttribute() {
			return "value";
		}

		@Override
		protected MessageHandler createHandler(Object bean, Method method, List<Annotation> annotations) {
			LoggingHandler.Level level = MessagingAnnotationUtils.resolveAttribute(annotations, "level",
					LoggingHandler.Level.class);
			LoggingHandler loggingHandler = new LoggingHandler(level.name());
			MethodInvokingMessageProcessor<String> processor = new MethodInvokingMessageProcessor<>(bean, method);
			processor.setBeanFactory(this.beanFactory);
			loggingHandler.setLogExpression(new FunctionExpression<>(processor::processMessage));
			return loggingHandler;
		}

	}

}
