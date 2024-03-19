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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import java.io.IOException;
import java.time.Duration;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler.Level;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test positive working flow using NATS DSL configuration
 *
 * <p>Integration test cases to test NATS spring components communication with docker/devlocal NATS
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
@ContextConfiguration(classes = {NatsTestConfig.class, NatsDSLConfigTest.ContextConfig.class})
public class NatsDSLConfigTest extends AbstractNatsIntegrationTestSupport {

  private static final Log LOG = LogFactory.getLog(NatsDSLConfigTest.class);

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired private Connection natsConnection;

  @Autowired
  @Qualifier("messageConvertor")
  private MessageConverter<String> messageConvertor;

  @Autowired
  @Qualifier("consumerChannel")
  private PollableChannel consumerChannel;

  @Autowired
  @Qualifier("producerChannel")
  private DirectChannel producerChannel;

  @Autowired private ApplicationContext appContext;

  // Messaging gateway to start/stop adapter on demand
  @Autowired private ControlBusGateway controlBus;

  /**
   * Tests complete postive flow using outbound(message producer) and inbound (message consumer)
   * adapters. Bean Context for this test defined below in Producer Flow Bean: {@link
   * ContextConfig#outboundFlow()}
   *
   * <p>Consumer Flow Bean: {@link ContextConfig#inboundFlow()}
   */
  @Test
  public void testCompleteFlowUsingSpringIntegration() {
    // testInitialSetup to check if the natsConnection is available
    LOG.info(this.natsConnection.getConnectedUrl());
    // Bean references to check their running state
    final NatsMessageDrivenChannelAdapter adapter =
        (NatsMessageDrivenChannelAdapter) this.appContext.getBean("testAdapterPushMode");
    final NatsConcurrentListenerContainer concurrentContainer =
        (NatsConcurrentListenerContainer) this.appContext.getBean("testContainerPushMode");
    // Check if Adapter and Container is not running when not started
    assertFalse(adapter.isRunning());
    assertFalse(concurrentContainer.isRunning());
    // start adapter using messaging gateway
    this.controlBus.send("@testAdapterPushMode.start()");
    // Test Adapter and Container running after start
    assertTrue(adapter.isRunning());
    assertTrue(concurrentContainer.isRunning());
    // since concurrency is one, we know only one container is present
    final NatsMessageListenerContainer container = concurrentContainer.getContainers().get(0);
    assertTrue(container.isRunning());

    // Message Producer -> Publishes 5 messages to NATS server via
    // producerChannel
    for (int i = 0; i < 5; i++) {
      final boolean messageSent =
          this.producerChannel.send(MessageBuilder.withPayload("Hello" + i).build());
      assertTrue(messageSent);
    }

    // Message Consumer -> Consumed message from NATS server should be
    // available in Adapter's output channel(consumerChannel)
    for (int i = 0; i < 5; i++) {
      final Message<?> message = this.consumerChannel.receive(20000);
      assertThat(message).isNotNull();
      assertThat(message.getPayload()).isEqualTo("Hello" + i);
    }

    // Stop adapter
    this.controlBus.send("@testAdapterPushMode.stop()");
    // Check if Adapter and Container is stopped
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
  }

  /** Gateway interface used to start and stop adapter(spring integration) components. */
  @MessagingGateway(defaultRequestChannel = "controlBus.input")
  private interface ControlBusGateway {

    void send(String command);
  }

  /**
   * Configuration class to setup Beans required to initialize NATS Message producers and consumers
   * using NATS dsl classes
   */
  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
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
    public NatsTemplate natsTemplate() {
      return new NatsTemplate(this.natsConnection, TEST_SUBJECT, messageConvertor());
    }

    @Bean
    public IntegrationFlow outboundFlow() {
      return IntegrationFlow.from(producerChannel())
          .log(Level.INFO)
          .handle(Nats.outboundAdapter(natsTemplate()))
          .get();
    }

    @Bean
    public IntegrationFlow inboundFlow() {
      return IntegrationFlow.from(
              Nats.messageDrivenChannelAdapter(
                      Nats.container(testConsumerFactory(), NatsMessageDeliveryMode.PULL) //
                          .id("testContainerPushMode") //
                          .concurrency(1) //
                      ,
                      messageConvertor())
                  .id("testAdapterPushMode") //
                  .autoStartup(false)
                  .outputChannel(consumerChannel())) //
          .log(Level.INFO) //
          .get();
    }

    @Bean
    public NatsConsumerFactory testConsumerFactory() {
      final ConsumerProperties consumerProperties =
          new ConsumerProperties(
              TEST_STREAM, TEST_SUBJECT, "test-subject-consumer", "test-subject-group");
      consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
      return new NatsConsumerFactory(this.natsConnection, consumerProperties);
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    @Bean
    public MessageChannel consumerChannel() {
      return MessageChannels.queue().getObject();
    }

    @Bean
    public MessageChannel producerChannel() {
      return MessageChannels.direct().getObject();
    }
  }
}
