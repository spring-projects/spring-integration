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
import org.springframework.integration.dsl.MessagingGatewaySpec;
import org.springframework.integration.nats.AbstractNatsMessageListenerContainer;
import org.springframework.integration.nats.ConsumerProperties;
import org.springframework.integration.nats.NatsConsumerFactory;
import org.springframework.integration.nats.NatsInboundGateway;
import org.springframework.integration.nats.NatsMessageDeliveryMode;
import org.springframework.integration.nats.NatsTemplate;
import org.springframework.integration.nats.converter.MessageConverter;

/** A {@link MessagingGatewaySpec} implementation for the {@link NatsInboundGateway}. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsInboundGatewaySpec<S extends NatsInboundGatewaySpec<S>>
    extends MessagingGatewaySpec<S, NatsInboundGateway> {

  public NatsInboundGatewaySpec(NatsInboundGateway gateway) {
    super(gateway);
  }

  public static NatsInboundGatewaySpec of(
      AbstractNatsMessageListenerContainer listenerContainer,
      MessageConverter<?> pMessageConverter,
      NatsTemplate natsTemplate) {
    NatsInboundGatewaySpec spec =
        new NatsInboundGatewaySpec(getGateway(listenerContainer, pMessageConverter, natsTemplate));
    return spec;
  }

  private static NatsInboundGateway getGateway(
      AbstractNatsMessageListenerContainer listenerContainer,
      MessageConverter<?> pMessageConverter,
      NatsTemplate natsTemplate) {
    return new NatsInboundGateway(listenerContainer, pMessageConverter, natsTemplate);
  }

  public static NatsInboundGatewaySpec of(
      Connection natsConnection, String subject, MessageConverter<String> messageConvertor) {
    return NatsInboundGatewaySpec.of(
        Nats.container(
                new NatsConsumerFactory(natsConnection, new ConsumerProperties(subject)),
                NatsMessageDeliveryMode.CORE_ASYNC)
            .getObject() //
        ,
        messageConvertor,
        new NatsTemplate(natsConnection, subject, messageConvertor));
  }

  /**
   * @param extractRequestPayload the extractRequestPayload.
   * @return the spec.
   * @see NatsInboundGateway#setExtractRequestPayload(boolean)
   */
  public S extractRequestPayload(boolean extractRequestPayload) {
    this.target.setExtractRequestPayload(extractRequestPayload);
    return _this();
  }

  /**
   * @param extractReplyPayload the extractReplyPayload.
   * @return the spec.
   * @see NatsInboundGateway#setExtractReplyPayload(boolean)
   */
  public S extractReplyPayload(boolean extractReplyPayload) {
    this.target.setExtractReplyPayload(extractReplyPayload);
    return _this();
  }

  public static class NatsInboundGatewayListenerContainerSpec
      extends NatsInboundGatewaySpec<
          NatsInboundGatewaySpec.NatsInboundGatewayListenerContainerSpec> {

    private final NatsMessageListenerContainerSpec spec;

    protected NatsInboundGatewayListenerContainerSpec(
        MessageConverter<?> pMessageConverter,
        NatsTemplate natsTemplate,
        NatsMessageListenerContainerSpec spec) {
      super(getGateway(spec.getObject(), pMessageConverter, natsTemplate));
      this.spec = spec;
      this.spec.getObject().setAutoStartup(false);
    }
  }
}
