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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import java.io.IOException;
import javax.annotation.PostConstruct;
import org.aopalliance.aop.Advice;
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
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.exception.MessageConversionException;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Class to test Message Producer (Outbound flow) using NatsMessageProducingHandler(Synchronous)
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
@ContextConfiguration(classes = {NatsTestConfig.class, NatsOutboundAdapterTest.ContextConfig.class})
public class NatsOutboundAdapterTest extends AbstractNatsIntegrationTestSupport {

  private static final Log LOG = LogFactory.getLog(NatsOutboundAdapterTest.class);

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired private Connection natsConnection;

  @Autowired
  @Qualifier("producerChannelPositiveFlow")
  private DirectChannel producerChannelPositiveFlow;

  @Autowired
  @Qualifier("producerChannelNegativeFlow")
  private DirectChannel producerChannelNegativeFlow;

  @Autowired
  @Qualifier("producerChannelMessageConversionError")
  private DirectChannel producerChannelMessageConversionError;

  @Autowired private ApplicationContext appContext;

  /**
   * Tests positive flow of message producer(outbound)
   *
   * <p>Test Scenario: Send list of messages to the NATS server Expected: outbound flow should run
   * without any exceptions
   *
   * <p>Bean Context for this test defined below in Producer Flow Bean: {@link
   * ContextConfig#outboundPositiveFlow()}
   */
  @Test
  public void testOutboundPositiveFlowUsingSpringIntegration() {
    // testInitialSetup to check if the natsConnection is available
    LOG.info(this.natsConnection.getConnectedUrl());
    // Test if messages are sent to NATs server
    for (int i = 0; i < 5; i++) {
      assertTrue(
          this.producerChannelPositiveFlow.send(MessageBuilder.withPayload("Hello").build()));
    }
  }

  /**
   * Tests Negative flow of message producer(outbound).
   *
   * <p>Scenario: Send messages to 'subject' which is not configured( not available) Expected:
   * Exception is thrown when message is sent to invalid subject(topic)
   *
   * <p>Bean Context for this test defined below in Producer Flow Bean: {@link
   * ContextConfig#outboundNegativeFlowInvalidSubject()}
   */
  @Test
  public void testOutboundNegativeFlow() {
    // testInitialSetup to check if the natsConnection is available
    LOG.info(this.natsConnection.getConnectedUrl());
    // Test if exception is thrown when message is sent to invalid subject(topic)
    MessageDeliveryException messageDeliveryException =
        assertThrows(
            MessageDeliveryException.class,
            () ->
                this.producerChannelNegativeFlow.send(MessageBuilder.withPayload("Hello").build()));
    assertEquals(IOException.class, messageDeliveryException.getCause().getClass());
    assertTrue(
        messageDeliveryException
            .getMessage()
            .contains("Exception occurred while sending message to invalid_subject"));
  }

  /**
   * Tests Negative flow of message producer(outbound).
   *
   * <p>Scenario: Send messages which results in Message Conversion failures Expected: exception is
   * thrown when message conversion failure happens
   *
   * <p>Bean Context for this test defined below in Producer Flow Bean: {@link
   * ContextConfig#outboundNegativeFlowMessageConversionError()}
   */
  @Test
  public void testOutboundNegativeFlowMessageConversionError() {
    // testInitialSetup to check if the natsConnection is available
    LOG.info(this.natsConnection.getConnectedUrl());
    // Test if exception is thrown when message conversion failure happens
    MessageHandlingException messageHandlingException =
        assertThrows(
            MessageHandlingException.class,
            () ->
                this.producerChannelMessageConversionError.send(
                    MessageBuilder.withPayload(new TestStub("errorMessage")).build()));
    assertEquals(MessageConversionException.class, messageHandlingException.getCause().getClass());
    assertTrue(
        messageHandlingException
            .getMessage()
            .contains(
                "Error converting org.springframework.integration.nats.NatsOutboundAdapterTest$TestStub to byte array."));
  }

  /**
   * Configuration class to setup Beans required to initialize NATS Message producers using NATS dsl
   * classes
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

    // Postive Flow
    @Bean
    public MessageChannel producerChannelPositiveFlow() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    public NatsTemplate natsTemplate() {
      return new NatsTemplate(this.natsConnection, TEST_SUBJECT, messageConvertor());
    }

    @Bean
    public IntegrationFlow outboundPositiveFlow() {
      return IntegrationFlow.from(producerChannelPositiveFlow())
          .log(LoggingHandler.Level.INFO)
          .handle(Nats.outboundAdapter(natsTemplate()))
          .get();
    }

    @Bean
    public Advice expressionAdvice() {
      RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
      advice.setRecoveryCallback(new ErrorMessageSendingRecoverer(errorChannel()));
      return advice;
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    // Neagtive Flow - Invalid Subject
    @Bean
    public IntegrationFlow outboundNegativeFlowInvalidSubject() {
      return IntegrationFlow.from(producerChannelNegativeFlow())
          .log(LoggingHandler.Level.INFO)
          .handle(Nats.outboundAdapter(natsTemplateWithInvalidSubject()))
          .get();
    }

    @Bean
    public NatsTemplate natsTemplateWithInvalidSubject() {
      return new NatsTemplate(this.natsConnection, "invalid_subject", messageConvertor());
    }

    @Bean
    public MessageChannel producerChannelNegativeFlow() {
      return MessageChannels.direct().getObject();
    }

    // Negative Flow - MessageConversion Error
    @Bean
    public IntegrationFlow outboundNegativeFlowMessageConversionError() {
      return IntegrationFlow.from(producerChannelMessageConversionError())
          .log(LoggingHandler.Level.INFO)
          .handle(Nats.outboundAdapter(natsTemplateMessageConversionError()))
          .get();
    }

    @Bean
    public NatsTemplate natsTemplateMessageConversionError() {
      return new NatsTemplate(this.natsConnection, TEST_STREAM, testStubMessageConvertor());
    }

    @Bean
    public MessageConverter<TestStub> testStubMessageConvertor() {
      return new MessageConverter<>(TestStub.class);
    }

    @Bean
    public MessageChannel producerChannelMessageConversionError() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    public MessageChannel errorChannel() {
      return MessageChannels.direct().getObject();
    }
  }

  /**
   * Test class without getter method and Serializable to produce JSONProcessingException in
   * MessageConversion Flow.
   */
  class TestStub {

    private final String property;

    public TestStub(final String propertyValue) {
      this.property = propertyValue;
    }
  }
}
