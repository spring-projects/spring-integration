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

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.impl.NatsJetStreamMetaData;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
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
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test the redelivery functionality of message producer (NatsMessageAsyncProducingHandler
 * - Outbound Flow)
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
    classes = {NatsTestConfig.class, NatsMessageAsyncProducingHandlerTest.ContextConfig.class})
public class NatsMessageAsyncProducingHandlerTest extends AbstractNatsIntegrationTestSupport {

  public static final int MAX_REDELIVER = 3;
  private static final Log LOG = LogFactory.getLog(NatsMessageAsyncProducingHandlerTest.class);
  private static final String TEST_SUBJECT = "test-subject";
  private static final String TEST_STREAM = "test-stream";
  private static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";
  public static HashMap<String, Integer> messagesSent = new HashMap<>();
  public static HashMap<String, Integer> messagesRecCount = new HashMap<>();

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
   * Tests negative scenario with unavailable NATS Server using inbound {@link
   * NatsMessageAsyncProducingHandler} Bean context for inbound flow {@link
   * NatsMessageAsyncProducingHandlerTest.ContextConfig#testRecurringPaymentInBoundFlow(NatsTemplate)}
   * Bean context for outbound flow {@link
   * NatsMessageAsyncProducingHandlerTest.ContextConfig#negativeFlow()}
   *
   * <p>Test scenario: Non-breakable message producing during unavailability of NATS server
   * [Producer Internal queue capacity(10) = send 10 messages during server unavailability - to
   * check full queue] Insure all messages has been delivered even if NATS Server was not available.
   * Result expected: Message should be endless re-sent until NATS server again up and running and
   * delivered to consumer in any case without duplicates
   *
   * @throws Exception covers NATS server start/stop related eceptiond and below list of exceptions
   *     IOException covers various communication issues with the NATS server such as timeout or
   *     interruption InterruptedException if any thread has interrupted the current thread. The
   *     interrupted status of the current thread is cleared when this exception is thrown.
   *     JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testAsyncProducerWithFullQueueCapacity() throws Exception {
    // test scenario for unavailability of NATS server
    // 1. everything fine and NATS server available. We are sending 5 messages
    sendBunchMessages(0, 5);
    // 2. simulating a downtime of NATS Server shut downing the docker container
    LOG.info("Stopping NATS server....");
    wait(10000, "Await before stopping NATS Server.");
    stopNatsServer();
    wait(10000, "Await before sending messages to unavailable NATS Server.");
    // 3. trying to send the messages to not available server to see the behaviour, exact 10
    // messages ( full queue capacity)
    LOG.info("Trying to send to not available NATS server....");
    sendBunchMessages(5, 15);
    // 4. sleep the main publisher thread to give a chance the acknowledgment thread to consume
    // arriving acknowledgments from NATS server.
    // During this period of time there should be negative acknowledgments arriving the producer,
    // where retry logic has to happen for particular message.
    // It is about 10 sec downtime or unavailability of NATS server.
    wait(20000, "Await between unavailable and available server.");
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
    wait(10000, "Await for messages ack arrivals after the NATS has been restored.");
    // in spite of the fact that NATS Server was not available during the test we will receive all
    // messages in any case later, as soon NATS will become available
    LOG.info(
        "Sent amount of messages is "
            + messagesSent.size()
            + " and received is "
            + messagesReceived.size());

    messagesSent.keySet().stream()
        .forEach(s -> System.out.println("Payload: " + s + " Count: " + messagesSent.get(s)));
    messagesReceived.keySet().stream().forEach(System.out::println);
    Assert.assertEquals(messagesSent.size(), messagesReceived.size());
  }

  /**
   * Tests negative scenario with unavailable NATS Server using inbound {@link
   * NatsMessageAsyncProducingHandler} Bean context for inbound flow {@link
   * NatsMessageAsyncProducingHandlerTest.ContextConfig#testRecurringPaymentInBoundFlow(NatsTemplate)}
   * Bean context for outbound flow {@link
   * NatsMessageAsyncProducingHandlerTest.ContextConfig#negativeFlow()}
   *
   * <p>Test scenario: Non-breakable message producing during unavailability of NATS server
   * [Producer Internal queue capacity(10) = send messages less than this capacity (&lt; 100%)]
   * Insure all messages has been delivered even if NATS Server was not available. Result expected:
   * Message should be endless re-sent until NATS server again up and running and delivered to
   * consumer in any case without duplicates
   *
   * @throws Exception covers NATS server start/stop related eceptiond and below list of exceptions
   *     IOException covers various communication issues with the NATS server such as timeout or
   *     interruption InterruptedException if any thread has interrupted the current thread. The
   *     interrupted status of the current thread is cleared when this exception is thrown.
   *     JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testAsyncProducerWithPartiallyFullQueueCapacity() throws Exception {
    resetStatistics();
    // test scenario for unavailability of NATS server
    // 1. everything fine and NATS server available. We are sending 5 messages
    sendBunchMessages(0, 5);
    // 2. simulating a downtime of NATS Server shut downing the docker container
    LOG.info("Stopping NATS server....");
    wait(10000, "Await before stopping NATS Server.");
    stopNatsServer();
    wait(1000, "Await before sending messages to unavailable NATS Server.");
    // 3. trying to send the messages to not available server to see the behaviour, less than 10
    // messages ( partially full queue capacity)
    LOG.info("Trying to send to not available NATS server....");
    sendBunchMessages(5, 10);
    // 4. sleep the main publisher thread to give a chance the acknowledgment thread to consume
    // arriving acknowledgments from NATS server.
    // During this period of time there should be negative acknowledgments arriving the producer,
    // where retry logic has to happen for particular message.
    // It is about 10 sec downtime or unavailability of NATS server.
    wait(20000, "Await between unavailable and available server.");
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
    wait(10000, "Await for messages ack arrivals after the NATS has been restored.");
    // in spite of the fact that NATS Server was not available during the test we will receive all
    // messages in any case later, as soon NATS will become available
    LOG.info(
        "Sent amount of messages is "
            + messagesSent.size()
            + " and received is "
            + messagesReceived.size());

    messagesSent.keySet().stream()
        .forEach(s -> System.out.println("Payload: " + s + " Count: " + messagesSent.get(s)));
    messagesReceived.keySet().stream().forEach(System.out::println);
    Assert.assertEquals(messagesSent.size(), messagesReceived.size());
  }

  private void resetStatistics() {
    messagesSent.clear();
    messagesReceived.clear();
    messagesRecCount.clear();
  }

  private void wait(int millis, String name) throws InterruptedException {
    Timer t = new Timer();
    t.schedule(new SecondsCounter(name), 0, 1000);
    Thread.sleep(millis);
    t.cancel();
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

    @Value("${org.springframework.integration.nats.producer.max.ack.full.queue.capacity.amount}")
    private int maxAckFullQueueCapacityAmount;

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
                  .id("testErrorAdapter")
                  .autoStartup(true) //
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
              Nats.outboundAsyncProducingHandler(recurringNatsTemplate) //
                  .setOutputChannelName("inboundSimulationRecurringPaymentsChannel") //
                  .setAckCapacity(
                      10) // Queue capcacity reduced to verify full, half full and overflow
                  // scenarios
                  .setAckQueueCapacityTimeout(Duration.ofMillis(ackQueueCapacityTimeout)) //
                  .setAckNatsServerTimeout(Duration.ofMillis(ackNatsServerTimeout))
                  .setMaxAckFullQueueCapacityAmount(3)) //
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
              Integer sentCount = messagesSent.get(payload);
              if (sentCount == null) {
                messagesSent.put(payload, 1);
              } else {
                sentCount++;
                LOG.info(
                    "Message being resent to this channel for redelivery : "
                        + payload
                        + " count = "
                        + sentCount);
                messagesSent.put(payload, sentCount);
              }
              return ChannelInterceptor.super.preSend(message, channel);
            }
          });
      return channel;
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
      // payload suffixed with timestamp to identify duplicate messages
      messagesReceived.put(payload + "__time__" + metaData.timestamp(), metaData);
      // Map verify the number of times messages sent through this channel
      Integer recCount = messagesRecCount.get(payload);
      if (recCount == null) {
        messagesRecCount.put(payload, 1);
      } else {
        recCount++;
        LOG.info("Message sent to service Activator : " + payload + " count = " + recCount);
        messagesRecCount.put(payload, recCount);
      }
      return message;
    }
  }

  class SecondsCounter extends TimerTask {
    private final String name;
    private int countSec;

    public SecondsCounter(String name) {
      this.name = name;
      LOG.info("Task: " + name);
    }

    public void run() {
      LOG.info("Already waiting for " + countSec + " seconds.");
      countSec++;
    }
  }
}
