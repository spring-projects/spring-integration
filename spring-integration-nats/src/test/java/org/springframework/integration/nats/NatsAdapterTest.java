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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nats.client.impl.NatsMessage;
import java.io.Serializable;
import org.junit.Test;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.nats.NatsMessageDrivenChannelAdapter.NatsMessageHandler;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.exception.MessageConversionException;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.PollableChannel;

/** Unit testing of NatsMessageDrivenChannelAdapter */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsAdapterTest {

  /**
   * Testing of basic functionality of NatsMessageDrivenChannelAdapter start, stop and running state
   * of adapter
   */
  @Test
  public void testAdapterBasicFunctionality() {
    final NatsMessageListenerContainer container = mock(NatsMessageListenerContainer.class);
    final NatsMessageDrivenChannelAdapter adapter = new NatsMessageDrivenChannelAdapter(container);
    // Assert that adapter and container is not running before start
    assertFalse(adapter.isRunning());
    verify(container, times(1)).setAutoStartup(false);
    adapter.onInit();
    adapter.start();
    verify(container, times(1)).setMessageHandler(any());
    verify(container, times(1)).start();
    // Assert that adapter and container is running after start
    assertTrue(adapter.isRunning());
    // assertTrue(container.isRunning());
    adapter.stop();
    verify(container, times(1)).stop();
    // Assert that adapter and container is not running after stop
    assertFalse(adapter.isRunning());
  }

  /** Test successful execution on onMessage method in {@link NatsMessageHandler} */
  @Test
  public void testAdapterOnMessageSuccessFlow() {
    // Configure adapter bean
    final NatsMessageListenerContainer container = mock(NatsMessageListenerContainer.class);
    final MessageConverter<String> messageConverter = new MessageConverter<>(String.class);
    final NatsMessageDrivenChannelAdapter adapter =
        new NatsMessageDrivenChannelAdapter(container, messageConverter);
    final NatsMessageHandler<String> messageHandler =
        adapter.new NatsMessageHandler(messageConverter);
    // Configure consumer channel(output channel) for adapter
    final PollableChannel consumerChannel = MessageChannels.queue().getObject();
    adapter.setOutputChannel(consumerChannel);
    final NatsMessage natsMessage = mock(NatsMessage.class);
    // Configure natsMessage as it will be received from NATS server
    when(natsMessage.getData()).thenReturn(messageConverter.toMessage("Testing"));
    when(natsMessage.isJetStream()).thenReturn(true);
    // Invoke onMessage method with natsMessage
    messageHandler.onMessage(natsMessage);
    // Assert that message is received in consumer channel
    final org.springframework.messaging.Message<?> message = consumerChannel.receive(20000);
    assertThat(message).isNotNull();
    assertThat(message.getPayload()).isEqualTo("Testing");
  }

  /**
   * Test exception handling flow of onMessage method in {@link NatsMessageHandler} This tests the
   * scenario when message conversion fails when sending message to the adapter's output channel
   */
  @Test
  public void testAdapterMessageHandlingOnMessageConversionException() {
    // Configure adapter bean
    final NatsMessageListenerContainer container = mock(NatsMessageListenerContainer.class);
    final MessageConverter<String> messageConverterString = new MessageConverter<>(String.class);
    final MessageConverter<String> messageConverterTestStub = new MessageConverter<>(String.class);
    final NatsMessageDrivenChannelAdapter adapter =
        new NatsMessageDrivenChannelAdapter(container, messageConverterString);
    // Message Handler is configured to use String based message convertor
    final NatsMessageHandler<String> messageHandler =
        adapter.new NatsMessageHandler(messageConverterString);
    // Configure error channel for adapter to capture error info
    final PollableChannel errorChannel = MessageChannels.queue().getObject();
    adapter.setErrorChannel(errorChannel);
    final NatsMessage natsMessage = mock(NatsMessage.class);
    // Received nats message on onMessage method is converted using Test
    // stub based convertor
    when(natsMessage.getData())
        .thenReturn(
            messageConverterTestStub.toMessage(new TestStub("Error in Message Conversion")));
    when(natsMessage.isJetStream()).thenReturn(true);
    // Invoke onMessage method
    messageHandler.onMessage(natsMessage);
    // Error channel should receive the error info about message conversion
    // exception
    final org.springframework.messaging.Message<?> errorMessage = errorChannel.receive(20000);
    assertNotNull(errorMessage);
    assertEquals(MessageConversionException.class, errorMessage.getPayload().getClass());
    final MessageConversionException conversionException =
        (MessageConversionException) errorMessage.getPayload();
    assertTrue(conversionException.getMessage().contains("Error converting to java.lang.String"));
  }

  /**
   * Test Adapter for handling of exception thrown while processing message in transformer or
   * service activator
   */
  @Test
  public void testAdapterMessageHandlingOnProcessingException() {
    // Configure adapter bean
    final NatsMessageListenerContainer container = mock(NatsMessageListenerContainer.class);
    final MessageConverter<String> messageConverterString = new MessageConverter<>(String.class);
    final NatsMessageDrivenChannelAdapter adapter =
        new NatsMessageDrivenChannelAdapter(container, messageConverterString);
    final NatsMessageHandler<String> messageHandler =
        adapter.new NatsMessageHandler(messageConverterString);
    // Configure consumer and error channel for adapter to capture message
    // and error info
    final DirectChannel consumerChannel = MessageChannels.direct("consumerChannelId").getObject();
    final PollableChannel errorChannel = MessageChannels.queue().getObject();
    adapter.setOutputChannel(consumerChannel);
    adapter.setErrorChannel(errorChannel);
    // configure consumer channel processing handler to throw exception
    final boolean messageProcessing =
        consumerChannel.subscribe(
            (msg) -> {
              final String payload = (String) msg.getPayload();
              if ("transformError".equalsIgnoreCase(payload)) {
                throw new IllegalStateException("transform_error_message");
              }
            });
    assertTrue(messageProcessing);
    // configure error prone NATS message
    final NatsMessage natsMessage = mock(NatsMessage.class);
    when(natsMessage.getData()).thenReturn(messageConverterString.toMessage("transformError"));
    when(natsMessage.isJetStream()).thenReturn(true);
    // Invoke onMessage method with error prone message
    messageHandler.onMessage(natsMessage);
    // Error channel should receive the error info about processing and
    // delivery exception
    final org.springframework.messaging.Message<?> errorMessage = errorChannel.receive(20000);
    assertNotNull(errorMessage);
    assertEquals(MessageDeliveryException.class, errorMessage.getPayload().getClass());
    final MessageDeliveryException exception = (MessageDeliveryException) errorMessage.getPayload();
    assertTrue(exception.getMessage().contains("transform_error_message"));
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
