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
package org.springframework.integration.nats.dsl;

import io.nats.client.Connection;
import org.springframework.integration.nats.AbstractNatsMessageListenerContainer;
import org.springframework.integration.nats.NatsConsumerFactory;
import org.springframework.integration.nats.NatsMessageDeliveryMode;
import org.springframework.integration.nats.NatsTemplate;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.lang.NonNull;

/** Factory class for NATS components. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public final class Nats {

  private Nats() {
    super();
  }

  /**
   * Creates an initial spec for {@link NatsMessageListenerContainerSpec} with provided
   * consumerFactory and natsMessageDeliveryMode
   *
   * @param consumerFactory the consumerFactory {@link NatsConsumerFactory}
   * @param natsMessageDeliveryMode the message delivery mode {@link NatsMessageDeliveryMode}
   * @return
   */
  public static NatsMessageListenerContainerSpec container(
      @NonNull final NatsConsumerFactory consumerFactory,
      @NonNull final NatsMessageDeliveryMode natsMessageDeliveryMode) {
    return new NatsMessageListenerContainerSpec(consumerFactory, natsMessageDeliveryMode);
  }

  /**
   * Creates an initial spec for {@link NatsMessageListenerContainerSpec} with provided container
   * spec
   *
   * @param containerSpec the container spec {@link NatsMessageListenerContainerSpec}
   * @return the spec
   *     NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
   */
  public static NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
      messageDrivenChannelAdapter(final NatsMessageListenerContainerSpec containerSpec) {
    return new NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer(
        containerSpec);
  }

  /**
   * Creates an initial spec for {@link NatsMessageListenerContainerSpec} with provided container
   * spec and messageConvertor
   *
   * @param containerSpec the container spec {@link NatsMessageListenerContainerSpec}
   * @param messageConverter the message convertor {@link MessageConverter}
   * @return the spec
   *     NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
   */
  public static NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
      messageDrivenChannelAdapter(
          final NatsMessageListenerContainerSpec containerSpec,
          final MessageConverter<?> messageConverter) {
    return new NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer(
        containerSpec, messageConverter);
  }

  /**
   * Creates an initial spec for {@link NatsMessageListenerContainerSpec} with provided
   * consumerFactory and natsMessageDeliveryMode
   *
   * @param consumerFactory the consumerFactory {@link NatsConsumerFactory}
   * @param natsMessageDeliveryMode the message delivery mode {@link NatsMessageDeliveryMode}
   * @return the spec
   *     NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
   */
  public static NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
      messageDrivenChannelAdapter(
          @NonNull final NatsConsumerFactory consumerFactory,
          @NonNull final NatsMessageDeliveryMode natsMessageDeliveryMode) {
    return new NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer(
        container(consumerFactory, natsMessageDeliveryMode));
  }

  /**
   * Creates an initial spec for {@link NatsMessageDrivenChannelAdapterSpec} with provided
   * listenerContainer and messageConvertor
   *
   * @param consumerFactory the consumerFactory {@link NatsConsumerFactory}
   * @param natsMessageDeliveryMode the message delivery mode {@link NatsMessageDeliveryMode}
   * @param messageConverter the message convertor {@link MessageConverter}
   * @return the spec
   *     NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
   */
  public static NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer
      messageDrivenChannelAdapter(
          @NonNull final NatsConsumerFactory consumerFactory,
          @NonNull final NatsMessageDeliveryMode natsMessageDeliveryMode,
          final MessageConverter<?> messageConverter) {
    return new NatsMessageDrivenChannelAdapterSpec.NatsMessageDrivenChannelAdpaterListenerContainer(
        container(consumerFactory, natsMessageDeliveryMode), messageConverter);
  }

  /**
   * Creates an initial spec for {@link NatsMessageDrivenChannelAdapterSpec} with provided
   * listenerContainer and messageConvertor
   *
   * @param listenerContainer the listenerContainer {@link AbstractNatsMessageListenerContainer}
   * @param messageConverter the messageConverter {@link MessageConverter}
   * @return the spec
   */
  public static NatsMessageDrivenChannelAdapterSpec<?> messageDrivenChannelAdapter(
      @NonNull final AbstractNatsMessageListenerContainer listenerContainer,
      @NonNull final MessageConverter<?> messageConverter) {
    return new NatsMessageDrivenChannelAdapterSpec<>(listenerContainer, messageConverter);
  }

  /**
   * Creates an initial spec for {@link NatsMessageDrivenChannelAdapterSpec} with provided
   * listenerContainer
   *
   * @param listenerContainer the listenerContainer {@link AbstractNatsMessageListenerContainer}
   * @return the spec
   */
  public static NatsMessageDrivenChannelAdapterSpec<?> messageDrivenChannelAdapter(
      @NonNull final AbstractNatsMessageListenerContainer listenerContainer) {
    return new NatsMessageDrivenChannelAdapterSpec<>(listenerContainer);
  }

  /**
   * Creates an initial spec for {@link NatsMessageProducingHandlerSpec} with provided natsTemplate
   *
   * @param natsTemplate the NatsTemplate {@link NatsTemplate}
   * @return the spec
   */
  public static NatsMessageProducingHandlerSpec outboundAdapter(
      @NonNull final NatsTemplate natsTemplate) {
    return NatsMessageProducingHandlerSpec.of(natsTemplate);
  }

  /**
   * Creates an initial spec for {@link NatsMessageAsyncProducingHandlerSpec} with provided
   * natsTemplate
   *
   * <p>This method creates asynchronous message producing handler.
   *
   * @param natsTemplate the NatsTemplate {@link NatsTemplate}
   * @return the spec
   */
  public static NatsMessageAsyncProducingHandlerSpec outboundAsyncProducingHandler(
      @NonNull final NatsTemplate natsTemplate) {
    return NatsMessageAsyncProducingHandlerSpec.ofAsync(natsTemplate);
  }

  /**
   * Creates an initial spec for {@link NatsOutboundGatewaySpec} with provided natsTemplate
   *
   * <p>This method creates reply producing message handler/outbound Gateway.
   *
   * @param natsTemplate the NatsTemplate {@link NatsTemplate}
   * @param messageConverter messageConvertor to extract the reply payload
   * @return the spec
   */
  public static NatsOutboundGatewaySpec outboundGateway(
      @NonNull final NatsTemplate natsTemplate, @NonNull final MessageConverter messageConverter) {
    return NatsOutboundGatewaySpec.of(natsTemplate, messageConverter);
  }

  /**
   * Creates an initial spec for {@link NatsOutboundGatewaySpec} with provided natsConnection,
   * subject and messageConvertor
   *
   * <p>This method creates reply producing message handler/outbound Gateway.
   *
   * @param natsConnection
   * @param subject
   * @param messageConvertor
   * @return
   */
  public static NatsOutboundGatewaySpec outboundGateway(
      Connection natsConnection, String subject, MessageConverter<String> messageConvertor) {
    return NatsOutboundGatewaySpec.of(natsConnection, subject, messageConvertor);
  }

  /**
   * Creates an initial spec for {@link NatsInboundGatewaySpec} with provided container,
   * natsTemplate and message convertor
   *
   * @param listenerContainer the listenerContainer {@link AbstractNatsMessageListenerContainer}
   * @param messageConverter the messageConverter {@link MessageConverter}
   * @param natsTemplate the NatsTemplate {@link NatsTemplate}
   * @return
   */
  public static NatsInboundGatewaySpec inboundGateway(
      @NonNull final AbstractNatsMessageListenerContainer listenerContainer,
      @NonNull final MessageConverter<?> messageConverter,
      @NonNull final NatsTemplate natsTemplate) {
    return NatsInboundGatewaySpec.of(listenerContainer, messageConverter, natsTemplate);
  }

  /**
   * Creates an initial spec for {@link NatsInboundGatewaySpec} with provided containerSpec,
   * natsTemplate and message convertor
   *
   * @param containerSpec the container spec {@link NatsMessageListenerContainerSpec}
   * @param messageConverter the messageConverter {@link MessageConverter}
   * @param natsTemplate the NatsTemplate {@link NatsTemplate}
   * @return
   */
  public static NatsInboundGatewaySpec inboundGateway(
      final NatsMessageListenerContainerSpec containerSpec,
      @NonNull final MessageConverter<?> messageConverter,
      @NonNull final NatsTemplate natsTemplate) {
    return new NatsInboundGatewaySpec.NatsInboundGatewayListenerContainerSpec(
        messageConverter, natsTemplate, containerSpec);
  }

  /**
   * Creates an initial spec for {@link NatsInboundGatewaySpec} with provided natsConnection,
   * subject and message convertor
   *
   * <p>Required container bean, consumerProperties and Template bean are created automatically
   *
   * @param natsConnection
   * @param subject
   * @param messageConvertor
   * @return
   */
  public static NatsInboundGatewaySpec inboundGateway(
      Connection natsConnection, String subject, MessageConverter<String> messageConvertor) {
    return NatsInboundGatewaySpec.of(natsConnection, subject, messageConvertor);
  }
}
