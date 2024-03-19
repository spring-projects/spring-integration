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
package org.springframework.integration.nats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import java.io.IOException;
import java.time.Duration;
import javax.annotation.PostConstruct;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.exception.NatsException;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the flow using Producer and Consumer bean configuration without DSL
 *
 * <p>Integration test cases to test NATS spring components communication with docker/devlocal NAT
 * server.
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
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
    classes = {NatsTestConfig.class, NatsMessageDrivenChannelAdapterTest.ContextConfig.class})
public class NatsMessageDrivenChannelAdapterTest extends AbstractNatsIntegrationTestSupport {

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired private Connection natsConnection;

  @Autowired private ControlBusGateway controlBus;

  @Autowired
  @Qualifier("messageConvertor")
  private MessageConverter<String> messageConvertor;

  @Autowired
  @Qualifier("consumerChannel")
  private PollableChannel consumerChannel;

  @Autowired
  @Qualifier("consumerStubChannel")
  private PollableChannel consumerStubChannel;

  @Autowired
  @Qualifier("consumerErrorChannel")
  private PollableChannel consumerErrorChannel;

  @Autowired private ApplicationContext appContext;

  /**
   * Tests the positive flow using NatsTemplate and NatsMessageDrivenChannelAdapter bean
   * configuration without DSL
   *
   * <p>Test Scenario: Publish messages to subject using NatsTemplate ResultExpected: Messages
   * consumed via adapter should be available in the consumer channel
   *
   * <p>Beans are configured below in {@link ContextConfig#positiveFlow()}
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testCompletePositiveFlow() throws IOException, JetStreamApiException {
    // start adapter using messaging gateway
    this.controlBus.send("@testAdapter.start()");
    // Message Producer -> publish messages to subject using NatsTemplate
    final NatsTemplate natsTemplate =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, this.messageConvertor);
    for (int i = 0; i < 5; i++) {
      final PublishAck ack = natsTemplate.send("Hello" + i);
      assertNotNull(ack);
      assertEquals(TEST_STREAM, ack.getStream());
    }

    // Message Consumer Assertion -> Messages consumed via adapter should be
    // available in the consumer channel
    for (int i = 0; i < 5; i++) {
      final Message<?> message = this.consumerChannel.receive(20000);
      assertNotNull(message);
      assertEquals("Hello" + i, message.getPayload());
    }
    // stop adapter using messaging gateway
    this.controlBus.send("@testAdapter.stop()");
  }

  /**
   * Method to test behavior of adapter when configured with non-existing stream or subject.
   *
   * <p>Test Scenario: Check what happens if consumer will subscribe for not non-existing Subject
   *
   * <p>Expected Result: Check if exception is thrown and consumed and logged by the error channel
   * configured in inbound adapter
   *
   * <p>Test Beans are configured below in {@link
   * ContextConfig#negativeFlowSubscriptionNotAvailable()} ()}
   */
  @Test
  public void testSubscriptionNotAvailableOnNoStreamOrSubjectFound() {
    // Bean references of adapter and container to check its running state
    final NatsMessageDrivenChannelAdapter adapter =
        (NatsMessageDrivenChannelAdapter) this.appContext.getBean("testErrorAdapterSub");
    final AbstractNatsMessageListenerContainer container =
        (AbstractNatsMessageListenerContainer) this.appContext.getBean("testErrorContainerSub");
    // Assert that adapter and container is not running before start
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
    // Check if NATS exception is thrown with expected message
    final NatsException exception =
        assertThrows(
            NatsException.class,
            () -> {
              this.controlBus.send("@testErrorAdapterSub.start()");
            });
    assertEquals(IllegalStateException.class, exception.getCause().getClass());
    assertTrue(
        exception
            .getMessage()
            .contains("Subscription is not available to start the container for subject:"));
    final IllegalStateException nestedException = (IllegalStateException) exception.getCause();
    assertTrue(
        nestedException.getMessage().contains("[SUB-90007] No matching streams for subject."));
    this.controlBus.send("@testErrorAdapterSub.stop()");
  }

  /** Gateway interface used to start and stop adapter(spring integration) components. */
  @MessagingGateway(defaultRequestChannel = "controlBus.input")
  private interface ControlBusGateway {

    void send(String command);
  }

  /**
   * Configuration class to setup Beans required to initialize NATS Message producers and consumers
   * using NATS integration classes
   */
  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
  @Repository
  public static class ContextConfig {

    @Autowired private Connection natsConnection;

    @PostConstruct
    public void streamSetup() throws IOException, JetStreamApiException {
      createStreamConfig(this.natsConnection, TEST_STREAM, TEST_SUBJECT);
    }

    @Bean
    public IntegrationFlow controlBus() {
      return IntegrationFlowDefinition::controlBus;
    }

    @Bean
    public IntegrationFlow positiveFlow() {
      return IntegrationFlow.from(testAdapter()) //
          .log(Level.INFO)
          .get();
    }

    @Bean
    public NatsMessageDrivenChannelAdapter testAdapter() {
      final NatsMessageDrivenChannelAdapter adapter =
          new NatsMessageDrivenChannelAdapter(testContainer(), messageConvertor());
      adapter.setBeanName("testAdapter");
      adapter.setOutputChannel(consumerChannel());
      adapter.setErrorChannel(consumerErrorChannel());
      adapter.setAutoStartup(false);
      return adapter;
    }

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

    @Bean
    public IntegrationFlow negativeFlowSubscriptionNotAvailable() {
      return IntegrationFlow.from(
              Nats.messageDrivenChannelAdapter(
                      Nats.container(
                              testConsumerFactoryForNegativeFlowSubscriptionNotAvailable(),
                              NatsMessageDeliveryMode.PULL)
                          .id("testErrorContainerSub")
                          .concurrency(1),
                      messageConvertor()) //
                  .id("testErrorAdapterSub") //
                  .autoStartup(false)) //
          .log(Level.INFO) //
          .get();
    }

    @NonNull
    @Bean
    public NatsConsumerFactory testConsumerFactoryForNegativeFlowSubscriptionNotAvailable() {
      final ConsumerProperties consumerProperties =
          new ConsumerProperties(
              "invalidStream",
              "invalidSubject",
              "invalidSubject-error-consumer",
              "invalidSubject-error-group");
      return new NatsConsumerFactory(this.natsConnection, consumerProperties);
    }

    @NonNull
    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    @NonNull
    @Bean
    public MessageChannel consumerChannel() {
      return MessageChannels.queue().getObject();
    }

    @NonNull
    @Bean
    public MessageChannel consumerStubChannel() {
      return MessageChannels.queue().getObject();
    }

    @Bean
    public MessageChannel consumerErrorChannel() {
      return MessageChannels.queue().getObject();
    }
  }
}
