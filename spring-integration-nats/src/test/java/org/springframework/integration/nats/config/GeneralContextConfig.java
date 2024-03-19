/*
 * Copyright 2016-2024 the original author or authors.
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
/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
package org.springframework.integration.nats.config;

import io.nats.client.Connection;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.nats.ConsumerProperties;
import org.springframework.integration.nats.NatsConsumerFactory;
import org.springframework.integration.nats.NatsMessageDeliveryMode;
import org.springframework.integration.nats.NatsMessageDrivenChannelAdapter;
import org.springframework.integration.nats.NatsMessageListenerContainer;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageChannel;

public abstract class GeneralContextConfig {
  public static final String TEST_SUBJECT = "test-subject";
  public static final String TEST_STREAM = "test-stream";
  @Autowired private Connection natsConnection;

  @Bean
  public IntegrationFlow positiveFlow() {
    return IntegrationFlow.from(testAdapter()) //
        .log(LoggingHandler.Level.INFO)
        .get();
  }

  public abstract NatsMessageDrivenChannelAdapter testAdapter();

  @NonNull
  @Bean
  public NatsMessageListenerContainer testContainer() {
    return new NatsMessageListenerContainer(
        testConsumerFactory(), NatsMessageDeliveryMode.PUSH_ASYNC);
  }

  @NonNull
  @Bean
  public NatsConsumerFactory testConsumerFactory() {
    final ConsumerProperties consumerProperties =
        new ConsumerProperties(
            TEST_STREAM, TEST_SUBJECT, "test-subject-consumer", "test-subject-group");
    consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
    return new NatsConsumerFactory(this.natsConnection, consumerProperties);
  }

  @NonNull
  @Bean
  public MessageChannel consumerChannel() {
    return MessageChannels.queue().getObject();
  }

  @Bean
  public MessageChannel consumerErrorChannel() {
    return MessageChannels.queue().getObject();
  }
}
