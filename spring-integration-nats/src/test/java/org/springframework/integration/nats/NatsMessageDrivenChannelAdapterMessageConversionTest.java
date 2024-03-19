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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import javax.annotation.PostConstruct;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.integration.nats.exception.MessageConversionException;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test negative scenarios - Message conversion exception and processing exceptions
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
      NatsMessageDrivenChannelAdapterMessageConversionTest.ContextConfig.class
    })
public class NatsMessageDrivenChannelAdapterMessageConversionTest
    extends AbstractNatsIntegrationTestSupport {

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired private Connection natsConnection;

  @Autowired private ControlBusGateway controlBus;

  @Autowired
  @Qualifier("messageConvertorString")
  private MessageConverter<String> messageConvertorString;

  @Autowired
  @Qualifier("consumerChannel")
  private PollableChannel consumerChannel;

  @Autowired
  @Qualifier("errorChannel")
  private PollableChannel errorChannel;

  /**
   * Tests negative scenario using outbound(message producer) and inbound (message consumer)
   * adapters. Downstream message handling exceptions should never interrupt the consumer polling.
   * Exceptions should be logged and polling should continue.
   *
   * <p>Test scenario: 1.Send mocked message which throws MessageConversionException in adapter
   * 2.Send mocked message which throws Transformer Exception in channel during message processing
   *
   * <p>Result expected: These exceptions should be logged and sent to error Channel of the adapter
   * and consumer polling should continue
   *
   * <p>Bean Context for this test defined below in {@link ContextConfig#negativeFlow()}
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testMessageConversionErrorFlow() throws IOException, JetStreamApiException {
    // start adapter by sending input to Message gateway
    this.controlBus.send("@testErrorAdapter.start()");

    // Message producer to send valid message with correct converter String
    final NatsTemplate natsTemplateString =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, this.messageConvertorString);
    // Producer to send invalid message with incorrect converter TestStub
    final MessageConverter<TestStub> testStubConvertor = new MessageConverter<>(TestStub.class);
    final NatsTemplate natsTemplateTestStub =
        new NatsTemplate(this.natsConnection, TEST_SUBJECT, testStubConvertor);

    // publish valid messages to subject via natsTemplateString
    for (int i = 0; i < 2; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      assertEquals(TEST_STREAM, ack.getStream());
    }
    // Publish invalid message via natsTemplate
    natsTemplateTestStub.send(new TestStub("error"));

    // publish some more valid messages to subject via natsTemplateString
    for (int i = 2; i < 4; i++) {
      final PublishAck ack = natsTemplateString.send("Hello" + i);
      assertNotNull(ack);
      assertEquals(TEST_STREAM, ack.getStream());
    }

    // Message consumer -> check for valid messages in consumer channel
    for (int i = 0; i < 4; i++) {
      final Message<?> message = this.consumerChannel.receive(20000);
      assertNotNull(message);
      assertEquals("Hello" + i, message.getPayload());
    }

    // Message consumer -> check for information about invalid messages in
    // error channel
    Message<?> errorMessage = this.errorChannel.receive(20000);
    assertNotNull(errorMessage);
    assertEquals(MessageConversionException.class, errorMessage.getPayload().getClass());
    final MessageConversionException conversionException =
        (MessageConversionException) errorMessage.getPayload();
    assertTrue(conversionException.getMessage().contains("Error converting to java.lang.String"));

    // Message Producer: Publish new messages to topic via
    // natsTemplateString
    // check whether consumer is running even after message conversion error
    natsTemplateString.send("Hello" + "still running1");
    natsTemplateString.send("Hello" + "still running2");

    // Message consumer -> check for valid messages in consumer channel
    Message<?> message = this.consumerChannel.receive(20000);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("Hello" + "still running1");
    message = this.consumerChannel.receive(20000);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("Hello" + "still running2");

    // Scenario: Exception in transformer method

    // Message Producer: Publish new error prone messages to topic via
    // natsTemplateString
    // On this particular message transformer will throw Exception
    // See negativeFlow bean below
    final PublishAck ack = natsTemplateString.send("message_tranformation_error");
    assertNotNull(ack);
    assertEquals(TEST_STREAM, ack.getStream());

    // Message consumer -> check for information about invalid messages in
    // error channel
    errorMessage = this.errorChannel.receive(20000);
    assertNotNull(errorMessage);
    assertEquals(errorMessage.getPayload().getClass(), MessageTransformationException.class);
    final MessageTransformationException transformException =
        (MessageTransformationException) errorMessage.getPayload();
    assertTrue(transformException.getMessage().contains("Failed to transform Message in bean"));
    assertEquals(
        "intentional Runtime Exception in transformer",
        transformException.getCause().getCause().getMessage());

    // Message Producer: Publish new messages to topic via
    // natsTemplateString
    // check whether consumer is running even after message conversion error
    natsTemplateString.send("Hello" + "still running3");
    natsTemplateString.send("Hello" + "still running4");

    // Message consumer -> check for valid messages in consumer channel
    message = this.consumerChannel.receive(20000);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("Hello" + "still running3");
    message = this.consumerChannel.receive(20000);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("Hello" + "still running4");
  }

  /** Gateway interface used to start and stop adapter(spring integration) components. */
  @MessagingGateway(defaultRequestChannel = "controlBus.input")
  private interface ControlBusGateway {

    void send(String command);
  }

  /**
   * Configuration class to setup Beans required to initialize NATS Message producers and consumers
   * for negative scenatrio testing
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
    public IntegrationFlow negativeFlow() {
      return IntegrationFlow.from(
              Nats.messageDrivenChannelAdapter(
                      Nats.container(
                              testConsumerFactoryForNegativeFlow(), NatsMessageDeliveryMode.PULL)
                          .id("testErrorContainer")
                          .concurrency(1),
                      messageConvertorString()) //
                  .id("testErrorAdapter") //
                  .autoStartup(false) //
                  .errorChannel(errorChannel())) //
          .log(Level.INFO) //
          .<String, String>transform(
              p -> {
                if (p.contains("message_tranformation_error")) {
                  throw new RuntimeException("intentional Runtime Exception in transformer");
                }
                return p;
              }) //
          .log(Level.INFO) //
          .channel("consumerChannel") //
          .get();
    }

    @Bean
    public NatsConsumerFactory testConsumerFactoryForNegativeFlow() {
      final ConsumerProperties consumerProperties =
          new ConsumerProperties(
              TEST_STREAM, TEST_SUBJECT, "test-subject-consumer", "test-subject-group");
      consumerProperties.setConsumerMaxWait(Duration.ofSeconds(1));
      return new NatsConsumerFactory(this.natsConnection, consumerProperties);
    }

    @Bean
    public MessageConverter<String> messageConvertorString() {
      return new MessageConverter<>(String.class);
    }

    @Bean
    public MessageChannel consumerChannel() {
      return MessageChannels.queue().getObject();
    }

    @Bean
    public MessageChannel errorChannel() {
      return MessageChannels.queue().getObject();
    }
  }

  /** Test class to produce invalid messages in different format for message conversion test */
  class TestStub implements Serializable {

    private static final long serialVersionUID = -684979447075L;

    private final String property;

    public TestStub(final String propertyValue) {
      this.property = propertyValue;
    }

    public String getProperty() {
      return this.property;
    }
  }
}
