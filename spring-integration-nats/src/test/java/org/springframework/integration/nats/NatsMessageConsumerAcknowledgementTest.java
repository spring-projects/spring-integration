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
import io.nats.client.impl.NatsMessage;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test the Acknowledgement Timeout Scenarios 1. Application restart(consumer restart)
 * during message processing 2. Message processing takes longer than the specified ACK timeout
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
    classes = {NatsTestConfig.class, NatsMessageConsumerAcknowledgementTest.ContextConfig.class})
public class NatsMessageConsumerAcknowledgementTest extends AbstractNatsIntegrationTestSupport {

  public static final int MAX_REDELIVER = 3;
  private static final Log LOG = LogFactory.getLog(NatsMessageConsumerAcknowledgementTest.class);
  private static final String TEST_SUBJECT = "test-subject";
  private static final String TEST_STREAM = "test-stream";
  private static final String TEST_SUBJECT_CONSUMER = "test-subject-consumer";
  public static Map<String, NatsJetStreamMetaData> messagesReceivedDataWithTimeStamp =
      new LinkedHashMap<>();

  public static Map<String, NatsJetStreamMetaData> messagesReceived = new LinkedHashMap<>();

  @Autowired private Connection natsConnection;

  @Autowired private NatsMessageConsumerAcknowledgementTest.ControlBusGateway controlBus;

  @Autowired
  @Qualifier("messageConvertorString")
  private MessageConverter<String> messageConvertorString;

  /**
   * Method to test if Message is redelivered when the ack wait timeout is reached on consumer
   * restart.
   *
   * <p>Bean context for outbound flow {@link ContextConfig#consumerInboundFlow()}
   *
   * <p>Test scenario: Consumer is restarted in between message processing Result expected: Message
   * should be redelivered until ack is done
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testMessageRedeliveryForAckTimeoutOnConsumerRestart() throws Exception {
    clearStatistic();

    // Message producer to send valid message with correct converter String
    final NatsTemplate natsTemplateString =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, this.messageConvertorString);

    // Publish specific message via natsTemplate - this message will be redelivered
    natsTemplateString.send("do-not-acknowledge");

    wait(2000, "Await before stopping the adapter to let the consumer start pulling messages");
    // consumer stopped
    this.controlBus.send("@testErrorAdapter.stop()");
    wait(5000, "Await before sending messages to unavailable NATS Server.");
    // consumer started and send some new messages
    this.controlBus.send("@testErrorAdapter.start()");

    // publish valid messages to subject via natsTemplateString
    for (int i = 0; i < 5; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      Assertions.assertEquals(TEST_STREAM, ack.getStream());
    }

    // Wait till message redelivery happens
    wait(20000, "Await for messages arrivals after the application starts consuming messages.");

    // Check metadata of the NATS to verify the delivered count of nats messages

    LOG.info("-----------------------------------------------");
    LOG.info("----VERIFY NORMAL MESSAGES - Delivered once----");
    LOG.info("-----------------------------------------------");
    for (String s : messagesReceived.keySet()) {
      NatsJetStreamMetaData natsJetStreamMetaData = messagesReceived.get(s);
      if (!s.startsWith("do-not-acknowledge")) {
        // assert that for all other messages, they are delivered once
        LOG.info("Message received: " + s + " => " + natsJetStreamMetaData);
        assertEquals(1, natsJetStreamMetaData.deliveredCount());
      }
    }

    LOG.info("-----------------------------------------------");
    LOG.info("---VERIFY REDELIVERED MESSAGES - Delivered twice");
    LOG.info("-----------------------------------------------");
    for (String s : messagesReceived.keySet()) {
      NatsJetStreamMetaData natsJetStreamMetaData = messagesReceived.get(s);
      if (s.startsWith("do-not-acknowledge")) {
        // assert that "do-not-acknowledge" message is redelivered
        LOG.info("Message received: " + s + " => " + natsJetStreamMetaData);
        assertEquals(2, natsJetStreamMetaData.deliveredCount());
      }
    }
  }

  /**
   * Method to test if Message is redelivered when the ack wait timeout is reached on delayed/stuck
   * thread while processing message
   *
   * <p>Bean context for outbound flow {@link ContextConfig#consumerInboundFlow()}
   *
   * <p>Test scenario: Message processing takes longer than the specified ACK timeout ACK_WAIT is
   * configured as 2 seconds and message processing takes 10 seconds Result expected: Message will
   * be redelivered based on the ack_wait timeout and max redeliver count
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testMessageRedeliveryForAckTimeoutOnDelayedProcessing() throws Exception {
    clearStatistic();

    // Message producer to send valid message with correct converter String
    final NatsTemplate natsTemplateString =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, this.messageConvertorString);

    // publish some other messages to subject via natsTemplateString
    for (int i = 0; i < 5; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      Assertions.assertEquals(TEST_STREAM, ack.getStream());
    }

    // Publish specific message via natsTemplate - this message will be take time to process
    natsTemplateString.send("do-not-acknowledge");

    // Wait till message redelivery happens
    wait(20000, "Await for messages arrivals after the application starts consuming messages.");

    // Check metadata of the NATS to verify the delivered count of nats messages
    // All messages will be delivered only once since the single consumer will
    // wait for the messages processing to complete before pulling for next batch of messages

    LOG.info("-----------------------------------------------");
    LOG.info("----VERIFY NORMAL MESSAGES - Delivered once----");
    LOG.info("-----------------------------------------------");
    for (String s : messagesReceived.keySet()) {
      NatsJetStreamMetaData natsJetStreamMetaData = messagesReceived.get(s);
      // assert that for all other messages, they are delivered once
      LOG.info("Message received: " + s + " => " + natsJetStreamMetaData);
      assertEquals(1, natsJetStreamMetaData.deliveredCount());
    }

    LOG.info("-----------------------------------------------");
    LOG.info("--VERIFY REDELIVERED MESSAGES - Timestamp------");
    LOG.info("-----------------------------------------------");
    for (String s : messagesReceivedDataWithTimeStamp.keySet()) {
      NatsJetStreamMetaData natsJetStreamMetaData = messagesReceivedDataWithTimeStamp.get(s);
      if (s.startsWith("do-not-acknowledge")) {
        // assert that "do-not-acknowledge" message is redelivered
        LOG.info("Message received: " + s + " => " + natsJetStreamMetaData);
      }
    }
  }

  private void clearStatistic() {
    messagesReceived.clear();
    messagesReceivedDataWithTimeStamp.clear();
  }

  private void wait(int millis, String name) throws InterruptedException {
    Timer t = new Timer();
    t.schedule(new SecondsCounter(name), 0, 1000);
    Thread.sleep(millis);
    t.cancel();
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
      // Consumer Config to limit the redelivery and ackWait duration
      ConsumerConfiguration consumerConfiguration =
          ConsumerConfiguration.builder()
              .ackWait(Duration.ofSeconds(2))
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
    public IntegrationFlow consumerInboundFlow() {
      return IntegrationFlow.from(
              Nats.messageDrivenChannelAdapter(
                      Nats.container(
                              testConsumerFactoryForNegativeFlow(), NatsMessageDeliveryMode.PULL)
                          .id("testErrorContainer")
                          .concurrency(1)) //
                  .id("testErrorAdapter")) //
          .channel("consumerChannel")
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
     * Message consumer which does not ack only on
     * specific message payload
     * */
    @ServiceActivator(inputChannel = "consumerChannel")
    public void processMessage(@Payload final Message<?> message)
        throws IOException, InterruptedException {
      NatsMessage natsMessage = (NatsMessage) message.getPayload();
      final String processedMessage = messageConvertorString().fromMessage(natsMessage);
      NatsJetStreamMetaData metaData =
          (NatsJetStreamMetaData) message.getHeaders().get("nats_metadata");
      String key = processedMessage + "__" + Instant.now();
      messagesReceived.put(processedMessage, metaData);
      messagesReceivedDataWithTimeStamp.put(key, metaData);
      LOG.info("Message received in service Activator: " + key);
      if ("do-not-acknowledge".equalsIgnoreCase(processedMessage)) {
        Thread.sleep(20000);
      }
    }

    @Bean
    public MessageConverter<String> messageConvertorString() {
      return new MessageConverter<>(String.class);
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
