/*
 * Copyright 2015-2024 the original author or authors.
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

package org.springframework.integration.support.management;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.config.EnableIntegrationManagement;
import org.springframework.integration.config.IntegrationManagementConfigurer;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.ControlBusMessageProcessor;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class IntegrationManagementConfigurerTests {

	@Test
	public void testDefaults() {
		DirectChannel channel = new DirectChannel();
		AbstractMessageHandler handler = new RecipientListRouter();
		AbstractMessageSource<?> source = new AbstractMessageSource<>() {

			@Override
			public String getComponentType() {
				return null;
			}

			@Override
			protected Object doReceive() {
				return null;
			}
		};
		assertThat(channel.isLoggingEnabled()).isTrue();
		assertThat(handler.isLoggingEnabled()).isTrue();
		assertThat(source.isLoggingEnabled()).isTrue();
		ApplicationContext ctx = mock(ApplicationContext.class);
		Map<String, IntegrationManagement> beans = new HashMap<>();
		beans.put("foo", channel);
		beans.put("bar", handler);
		beans.put("baz", source);
		when(ctx.getBeansOfType(IntegrationManagement.class)).thenReturn(beans);
		IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
		configurer.setBeanName(IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME);
		configurer.setApplicationContext(ctx);
		configurer.setDefaultLoggingEnabled(false);
		configurer.afterSingletonsInstantiated();
		assertThat(channel.isLoggingEnabled()).isFalse();
		assertThat(handler.isLoggingEnabled()).isFalse();
		assertThat(source.isLoggingEnabled()).isFalse();
	}

	@Test
	public void testEmptyAnnotation() {
		try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigEmptyAnnotation.class)) {
			AbstractMessageChannel channel = ctx.getBean("channel", AbstractMessageChannel.class);
			assertThat(channel.isLoggingEnabled()).isTrue();
			channel = ctx.getBean("loggingOffChannel", AbstractMessageChannel.class);
			assertThat(channel.isLoggingEnabled()).isFalse();
		}
	}

	@Test
	public void controlBusIntegration() {
		try (ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(ControlBusEagerConfig.class)) {
			ControlBusCommandRegistry controlBusCommandRegistry =
					ctx.getBean(IntegrationContextUtils.CONTROL_BUS_COMMAND_REGISTRY_BEAN_NAME,
							ControlBusCommandRegistry.class);

			Map<String, Map<ControlBusCommandRegistry.CommandMethod, String>> commands =
					controlBusCommandRegistry.getCommands();

			assertThat(commands).containsKeys("errorChannel", "nullChannel", "taskScheduler", "channel",
					"_org.springframework.integration.errorLogger",
					"_org.springframework.integration.errorLogger.handler");

			Map<ControlBusCommandRegistry.CommandMethod, String> commandMethodStringMap = commands.get("channel");

			List<String> controlBusMethodForChannelBean =
					commandMethodStringMap.keySet()
							.stream()
							.map(ControlBusCommandRegistry.CommandMethod::getMethodName)
							.toList();

			assertThat(controlBusMethodForChannelBean)
					.containsOnly("setLoggingEnabled", "isLoggingEnabled", "setShouldTrack");

			Expression isLoggingEnabledCommand =
					controlBusCommandRegistry.getExpressionForCommand("nullChannel.isLoggingEnabled");

			StandardEvaluationContext evaluationContext = IntegrationContextUtils.getEvaluationContext(ctx);

			assertThat(isLoggingEnabledCommand.getValue(evaluationContext, boolean.class)).isFalse();

			Expression setLoggingEnabledCommand =
					controlBusCommandRegistry.getExpressionForCommand("nullChannel.setLoggingEnabled", boolean.class);

			setLoggingEnabledCommand.getValue(evaluationContext, new Object[] {true});

			assertThat(isLoggingEnabledCommand.getValue(evaluationContext, boolean.class)).isTrue();

			ControlBusMessageProcessor controlBusMessageProcessor = ctx.getBean(ControlBusMessageProcessor.class);

			assertThatIllegalArgumentException()
					.isThrownBy(() ->
							controlBusMessageProcessor.processMessage(new GenericMessage<>("nonSuchBean.command")))
					.withMessage("There is no registered bean for requested command: nonSuchBean");

			assertThatIllegalArgumentException()
					.isThrownBy(() ->
							controlBusMessageProcessor.processMessage(new GenericMessage<>("channel.noSuchCommand")))
					.withMessageStartingWith("No method 'noSuchCommand' found in bean 'bean 'channel'");

			controlBusMessageProcessor.processMessage(
					MessageBuilder.withPayload("nullChannel.setLoggingEnabled")
							.setHeader(IntegrationMessageHeaderAccessor.CONTROL_BUS_ARGUMENTS, List.of(false))
							.build());

			assertThat((Boolean) controlBusMessageProcessor.processMessage(
					new GenericMessage<>("nullChannel.isLoggingEnabled")))
					.isFalse();
		}
	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement
	public static class ConfigEmptyAnnotation {

		@Bean
		public MessageChannel channel() {
			return new DirectChannel();
		}

		@Bean
		public MessageChannel loggingOffChannel() {
			DirectChannel directChannel = new DirectChannel();
			directChannel.setLoggingEnabled(false);
			return directChannel;
		}

	}

	@Configuration
	@EnableIntegration
	@EnableIntegrationManagement(loadControlBusCommands = "true", defaultLoggingEnabled = "false")
	public static class ControlBusEagerConfig {

		@Bean
		MessageChannel channel() {
			return new DirectChannel();
		}

		@Bean
		ControlBusMessageProcessor controlBusMessageProcessor(ControlBusCommandRegistry controlBusCommandRegistry) {
			return new ControlBusMessageProcessor(controlBusCommandRegistry);
		}

	}

}
