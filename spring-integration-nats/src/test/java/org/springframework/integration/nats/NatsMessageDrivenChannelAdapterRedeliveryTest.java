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
import static org.junit.Assert.assertNotNull;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsJetStreamMetaData;
import io.nats.client.support.NatsJetStreamConstants;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.nats.support.thread.BasicThreading;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test the redelivery functionality of 1. NATS server in message consumer
 * (NatsMessageDrivenChannelAdapter - Inbound Flow) 2. NATS client Producer
 * (NatsMessageAsyncProducingHandler - Outbound Flow)
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
    classes = {
      NatsTestConfig.class,
      NatsMessageDrivenChannelAdapterRedeliveryTest.ContextConfig.class
    })
public class NatsMessageDrivenChannelAdapterRedeliveryTest
    extends AbstractNatsIntegrationTestSupport {

  public static final int MAX_REDELIVER = 3;
  private static final Log LOG =
      LogFactory.getLog(NatsMessageDrivenChannelAdapterRedeliveryTest.class);
  private static final String TEST_SUBJECT = "test-subject";
  private static final String TEST_STREAM = "test-stream";
  private static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";
  public static HashMap<String, NatsJetStreamMetaData> messagesSent = new HashMap<>();

  public static HashMap<String, NatsJetStreamMetaData> messagesReceived = new HashMap<>();

  @Autowired private Connection natsConnection;

  @Autowired private ControlBusGateway controlBus;

  @Autowired
  @Qualifier("messageConvertorString")
  private MessageConverter<String> messageConvertorString;

  @Autowired
  @Qualifier("inboundSimulationRecurringPaymentsChannel")
  private MessageChannel inboundSimulationRecurringPaymentsChannel;

  /**
   * Tests negative scenario with <b>invalid message</b> using inbound (message consumer) adapters.
   * Bean context for outbound flow {@link
   * NatsMessageDrivenChannelAdapterRedeliveryTest.ContextConfig#negativeFlow()}
   *
   * <p>Test scenario: Message processing for not valid message fails with exception and ack does
   * not happen. Result expected: Message should be redelivered based on the max delivery count and
   * processing should be triggered again
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testInvalidMessageRedeliveryForAckFailure()
      throws IOException, JetStreamApiException, InterruptedException {
    clearStatistic();
    // start adapter by sending input to Message gateway
    this.controlBus.send("@testErrorAdapter.start()");

    // Message producer to send valid message with correct converter String
    final NatsTemplate natsTemplateString =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, this.messageConvertorString);

    // publish valid messages to subject via natsTemplateString
    for (int i = 0; i < 5; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      Assertions.assertEquals(TEST_STREAM, ack.getStream());
    }
    // Publish invalid message via natsTemplate - this message will be redelivered
    natsTemplateString.send("error");

    // publish valid messages to subject via natsTemplateString
    for (int i = 5; i < 10; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      Assertions.assertEquals(TEST_STREAM, ack.getStream());
    }

    // Wait till message redelivery happens
    Thread.sleep(20000);

    // Check metadata of the NATS to verify the delivered count of nats messages
    messagesReceived.forEach(
        (s, natsJetStreamMetaData) -> {
          LOG.debug("Message received: " + s + " => " + natsJetStreamMetaData.deliveredCount());
          if ("error".equalsIgnoreCase(s)) {
            // assert that "error" message is redelivered
            assertEquals(MAX_REDELIVER, natsJetStreamMetaData.deliveredCount());
          } else {
            // assert that for all other messages, they are delivered once
            assertEquals(1, natsJetStreamMetaData.deliveredCount());
          }
        });
  }

  /**
   * Tests negative scenario with unavailable NATS Server using inbound {@link
   * NatsMessageAsyncProducingHandler} Bean context for inbound flow {@link
   * NatsMessageDrivenChannelAdapterRedeliveryTest.ContextConfig#testRecurringPaymentInBoundFlow(NatsTemplate)}
   * Bean context for outbound flow {@link
   * NatsMessageDrivenChannelAdapterRedeliveryTest.ContextConfig#negativeFlow()}
   *
   * <p>Test scenario: Non-breakable message producing during unavailability of NATS server. Insure
   * all messages has been delivered even if NATS Server was not available. Result expected: Message
   * should be endless re-sent until NATS server again up and running and delivered to consumer in
   * any case
   *
   * @throws Exception covers NATS server start/stop related eceptiond and below list of exceptions
   *     IOException covers various communication issues with the NATS server such as timeout or
   *     interruption InterruptedException if any thread has interrupted the current thread. The
   *     interrupted status of the current thread is cleared when this exception is thrown.
   *     JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testAsyncMessageProducerWithRedelivery() throws Exception {
    clearStatistic();
    // test scenario for unavailability of NATS server
    // 1. everything fine and NATS server available. We are sending 5 messages
    sendBunchMessages(0, 5);
    // 2. simulating a downtime of NATS Server shut downing the docker container
    LOG.info("Stopping NATS server....");
    BasicThreading.wait(10000, "Await before stopping NATS Server.");
    stopNatsServer();
    BasicThreading.wait(10000, "Await before sending messages to unavailable NATS Server.");
    // 3. trying to send the messages to not available server to see the behaviour
    LOG.info("Trying to send to not available NATS server....");
    sendBunchMessages(5, 10);
    // 4. sleep the main publisher thread to give a chance the acknowledgment thread to consume
    // arriving acknowledgments from NATS server.
    // During this period of time there should be negative acknowledgments arriving the producer,
    // where retry logic has to happen for particular message.
    // It is about 10 sec downtime or unavailability of NATS server.
    BasicThreading.wait(10000, "Await between unavailable and available server.");
    // 5. Let say after 10 sec the server becoming available. We're simulating the behaviour by
    // starting the server.
    startNatsServer();
    LOG.info("NATS Server connection status: " + natsConnection.getStatus());
    // simulation of PROD behaviour to the given pre-configuration of NATS server in respect of
    // streams and subjects
    // recreateStream();
    // 6. After server started again we're we should see successfully redelivered
    // sleep the main publisher thread to give a chance the acknowledgment thread to consume
    // arriving acknowledgments from NATS server.
    BasicThreading.wait(10000, "Await for messages ack arrivals after the NATS has been restored.");
    // in spite of the fact that NATS Server was not available during the test we will receive all
    // messages in any case later, as soon NATS will become available
    LOG.info(
        "Sent amount of messages is "
            + messagesSent.size()
            + " and received is "
            + messagesReceived.size());
    Assert.assertEquals(messagesSent.size(), messagesReceived.size());
  }

  private void clearStatistic() {
    messagesSent.clear();
    messagesReceived.clear();
  }

  private void recreateStream() throws IOException, JetStreamApiException {
    LOG.info("Recreating stream: " + TEST_STREAM + " and subject " + TEST_SUBJECT);
    createStreamConfig(this.natsConnection, TEST_STREAM, TEST_SUBJECT);
    // Consumer Config to limit the redelivery
    ConsumerConfiguration consumerConfiguration =
        ConsumerConfiguration.builder()
            .ackWait(Duration.ofSeconds(5))
            .maxDeliver(MAX_REDELIVER)
            .durable(TEST_SUBJECT_CONSUMER)
            .build();
    createConsumer(this.natsConnection, TEST_STREAM, consumerConfiguration);
  }

  private void sendBunchMessages(int from, int to) {
    for (int i = from; i < to; i++) {
      String payload = "Hello" + i;
      this.inboundSimulationRecurringPaymentsChannel.send(
          MessageBuilder.withPayload(payload).build());
    }
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

    @Value("${org.springframework.integration.nats.producer.ack.capacity.timeout.mls}")
    private long ackQueueCapacityTimeout;

    @Value("${org.springframework.integration.nats.server.ack.timeout.mls}")
    private long ackNatsServerTimeout;

    @Value("${org.springframework.integration.nats.producer.ack.capacity.amount}")
    private int ackQueueCapacityAmount;

    @PostConstruct
    public void streamSetup() throws IOException, JetStreamApiException {
      createStreamConfig(this.natsConnection, TEST_STREAM, TEST_SUBJECT);
      // Consumer Config to limit the redelivery
      ConsumerConfiguration consumerConfiguration =
          ConsumerConfiguration.builder()
              .ackWait(Duration.ofSeconds(5))
              .maxDeliver(MAX_REDELIVER)
              .durable(TEST_SUBJECT_CONSUMER)
              .build();
      createConsumer(this.natsConnection, TEST_STREAM, consumerConfiguration);
    }

    @Bean
    public IntegrationFlow controlBus() {
      return IntegrationFlowDefinition::controlBus;
    }

    @Bean
    public IntegrationFlow negativeFlow() {
      return IntegrationFlow.from(
              Nats.messageDrivenChannelAdapter(
                      Nats.container(
                              testConsumerFactoryForNegativeFlow(), NatsMessageDeliveryMode.PULL)
                          .id("testErrorContainer")
                          .concurrency(1),
                      messageConvertorString()) //
                  .id("testErrorAdapter") //
              ) //
          .log(LoggingHandler.Level.INFO) //
          .channel("consumerChannel") //
          .get();
    }

    @Bean
    public NatsTemplate testRecurringNatsTemplate() {
      return new NatsTemplate(this.natsConnection, TEST_SUBJECT, messageConvertorString());
    }

    @Bean
    public IntegrationFlow testRecurringPaymentInBoundFlow(
        @NonNull @Qualifier("testRecurringNatsTemplate") final NatsTemplate recurringNatsTemplate) {
      return IntegrationFlow.from("inboundSimulationRecurringPaymentsChannel")
          .handle(
              Nats.outboundAsyncProducingHandler(recurringNatsTemplate)
                  .setOutputChannelName("inboundSimulationRecurringPaymentsChannel")
                  .setAckCapacity(ackQueueCapacityAmount)
                  .setAckQueueCapacityTimeout(Duration.ofMillis(ackQueueCapacityTimeout))
                  .setAckNatsServerTimeout(Duration.ofMillis(ackNatsServerTimeout)))
          .get();
    }

    @Bean
    public NatsConsumerFactory testConsumerFactoryForNegativeFlow() {
      final ConsumerProperties consumerProperties =
          new ConsumerProperties(
              TEST_STREAM, TEST_SUBJECT, TEST_SUBJECT_CONSUMER, "test-subject-group");
      consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
      return new NatsConsumerFactory(this.natsConnection, consumerProperties);
    }

    /*
     * Message consumer which throws exception only on
     * specific message payload
     * */
    @ServiceActivator(inputChannel = "consumerChannel")
    public void processMessage(@Payload final String message) {
      LOG.info("Message received in service Activator: " + message);
      if ("error".equalsIgnoreCase(message)) {
        throw new RuntimeException("Error thrown to check message redelivery");
      } else {
        LOG.info("Message successfully processed in Service Activator: " + message);
      }
    }

    @Bean
    public MessageConverter<String> messageConvertorString() {
      return new MessageConverter<>(String.class);
    }

    @Bean
    public MessageChannel errorChannel() {
      return MessageChannels.queue().getObject();
    }

    @Bean
    public MessageChannel consumerChannel() {
      DirectChannel channel = MessageChannels.direct().getObject();
      channel.addInterceptor(new SampleInterceptor());
      return channel;
    }

    @Bean
    public MessageChannel inboundSimulationRecurringPaymentsChannel() {
      DirectChannel channel = MessageChannels.direct().getObject();
      channel.addInterceptor(
          new ChannelInterceptor() {

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
              String payload = (String) message.getPayload();
              Object messageId = message.getHeaders().get(NatsJetStreamConstants.MSG_ID_HDR);
              LOG.info(
                  "preSend Message  "
                      + message
                      + " received on inboundSimulationRecurringPaymentsChannel "
                      + channel
                      + " messageId: "
                      + messageId
                      + " payload: "
                      + payload);
              NatsJetStreamMetaData metaData =
                  (NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
              messagesSent.put(payload + messageId, metaData);
              return ChannelInterceptor.super.preSend(message, channel);
            }
          });
      channel.addInterceptor(
          new WireTap(loggerChannel(), message -> (message.getPayload() != null)));
      return channel;
    }

    @Bean
    public MessageChannel loggerChannel() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    @ServiceActivator(inputChannel = "loggerChannel")
    public MessageHandler loggerChannelHandler() {
      return new MessageHandler() {
        @Override
        public void handleMessage(Message<?> message) throws MessagingException {
          LOG.info("Message producing to NATS: " + message);
        }
      };
    }
  }

  /*
   * Interceptor to listen on the messages received by "consumerChannel"
   * and add metadata to messagesReceived map for verification
   *
   */
  public static class SampleInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
      String payload = (String) message.getPayload();
      NatsJetStreamMetaData metaData =
          (NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
      messagesReceived.put(payload, metaData);
      return message;
    }
  }
}
