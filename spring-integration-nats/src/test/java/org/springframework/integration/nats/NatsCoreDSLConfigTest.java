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
import static org.junit.Assert.assertTrue;

import io.nats.client.Connection;
import java.util.concurrent.Executors;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
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
 * Class to test Inbound and Outbound Gateways using NATS DSL configuration
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
@ContextConfiguration(
    classes = {
      NatsTestConfig.class,
      NatsCoreDSLConfigTest.OutboundContextConfig.class,
      NatsCoreDSLConfigTest.InboundContextConfig.class
    })
public class NatsCoreDSLConfigTest extends AbstractNatsIntegrationTestSupport {

  private static final Log LOG = LogFactory.getLog(NatsCoreDSLConfigTest.class);

  private static final String TEST_SUBJECT = "test.subject";

  private static final long TIME_OUT_DURATION =
      600; // Update to 6000 to verify negative timeout scenarios

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

  /**
   * Tests complete positive flow using outbound and inbound gateways . Bean Context for this test
   * defined below in Outbound Gateway Flow Bean: {@link OutboundContextConfig#outboundFlowTMS()}
   *
   * <p>Inbound Gateway Flow Bean: {@link InboundContextConfig#inboundFlowBus()} ()}
   *
   * <p>To test Negative flow 1. Update TIME_OUT_DURATION to 6000 (increases BUS (inbound Gateway)
   * handling time) and run tests again 2. Update replyTimeout parameters in {@link
   * OutboundContextConfig#outboundFlowTMS()} and {@link InboundContextConfig#inboundFlowBus()} ()}
   */
  @Test
  public void testCompleteFlowUsingSpringIntegration() {
    LOG.info(this.natsConnection.getConnectedUrl());
    try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    final int runTest_N_times = 1;
    for (int i = 0; i < runTest_N_times; i++) {
      LOG.info("Test Complete Flow Using Spring Integration execution num=" + i);
      runTestCompleteFlowUsingSpringIntegration();
    }
  }

  private void runTestCompleteFlowUsingSpringIntegration() {
    // Message Producer -> Publishes 2 messages to NATS server via
    // producerChannel
    for (int i = 0; i < 2; i++) {
      final boolean messageSent =
          this.producerChannel.send(MessageBuilder.withPayload("Hello" + i).build());
      assertTrue(messageSent);
    }

    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    // Message Consumer -> Consumed message from NATS server should be
    // available in Adapter's output channel(consumerChannel)
    for (int i = 0; i < 2; i++) {
      final Message<?> message = this.consumerChannel.receive(2000);
      if (message != null) {
        LOG.info("Rec Message: " + message.getPayload());
        assertThat(message).isNotNull();
      }
    }
  }

  /** Configuration class to set up Beans required to configure Outbound Gateway */
  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
  public static class OutboundContextConfig {

    @Autowired private Connection natsConnection;

    /**
     * TMS - Outbound flow - request reply
     *
     * @return
     */
    @Bean
    public IntegrationFlow outboundFlowTMS() {
      return IntegrationFlow.from(producerChannel()) // requestChannel from TMS
          .log(LoggingHandler.Level.INFO) //
          .handle(
              Nats.outboundGateway(natsConnection, TEST_SUBJECT, this.messageConvertor()) //
                  .errorChannelName("errorTMSChannel") // error channel
                  .replyChannel(consumerChannel()) // channel to receive response
                  .replyTimeout(10000)) // replyTimeout in milliseconds
          .get();
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    @Bean
    public MessageChannel producerChannel() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    public MessageChannel consumerChannel() {
      return MessageChannels.queue().getObject();
    }

    @ServiceActivator(inputChannel = "errorTMSChannel", outputChannel = "consumerChannel")
    public Message processErrorMessage(Message<?> m) {
      LOG.info("error TMS handling: " + m.getPayload());
      LOG.info("error TMS handling headers: " + m.getHeaders());
      Message msg =
          MessageBuilder.withPayload(m.getPayload() + "**TMS---ERROR**")
              .copyHeaders(m.getHeaders())
              .build();
      return msg;
    }
  }

  /** Configuration class to set up Beans required to configure Inbound Gateway */
  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
  public static class InboundContextConfig {

    @Autowired private Connection natsConnection;

    /**
     * Inbound Gateway - listens on the nats subject and sends the message to the reply channel
     *
     * @return
     */
    @Bean
    public IntegrationFlow inboundFlowBus() {
      return IntegrationFlow.from(
              Nats.inboundGateway(natsConnection, TEST_SUBJECT, messageConvertor()) //
                  .requestChannel(
                      consumerChannelBus()) // -> BUS handling - switch and endpoint call mock
                  .replyChannel(
                      "responseChannel") // BUS replies to this channel - gateway sends message back
                  // to reply subject
                  .errorChannel("errorBusChannel") // error channel
                  .replyTimeout(5000) // replyTimeout in milliseconds
              )
          .get();
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    // Consumer channel process should be running in a
    // separate thread to let the reply Timeout to take effect.
    @Bean
    public MessageChannel consumerChannelBus() {
      return MessageChannels.executor("test", Executors.newFixedThreadPool(2)).getObject();
      // return MessageChannels.direct().get();
    }

    @Bean
    public MessageChannel responseChannel() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    public MessageChannel errorBusChannel() {
      return MessageChannels.direct().getObject();
    }

    @ServiceActivator(inputChannel = "consumerChannelBus")
    public Message processMessage(Message<?> m) {

      LOG.info("bus handling: " + m.getPayload());
      LOG.info("bus handling headers: " + m.getHeaders());
      try {
        // int y = 7/0; // disable this commented line to simulate exception behaviour in inbound
        // gateway
        Thread.sleep(NatsCoreDSLConfigTest.TIME_OUT_DURATION);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      Message msg =
          MessageBuilder.withPayload(m.getPayload() + "**BUS**")
              .copyHeaders(m.getHeaders())
              .build();
      return msg;
    }

    @ServiceActivator(inputChannel = "errorBusChannel", outputChannel = "responseChannel")
    public Message processBusErrorMessage(final Message m) {
      LOG.info("error BUS handling: " + m.getPayload());
      LOG.info("error BUS handling headers: " + m.getHeaders());
      Message msg =
          MessageBuilder.withPayload(m.getPayload() + "**BUS--ERROR**")
              .copyHeaders(m.getHeaders())
              .build();
      return msg;
    }
  }
}
