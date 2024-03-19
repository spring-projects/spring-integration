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

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.nats.NatsMessageProducingHandler;
import org.springframework.integration.nats.NatsTemplate;
import org.springframework.lang.NonNull;

/** A {@link MessageHandlerSpec} implementation for the {@link NatsMessageProducingHandler}. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsMessageProducingHandlerSpec
    extends MessageHandlerSpec<NatsMessageProducingHandlerSpec, AbstractMessageHandler> {

  public static NatsMessageProducingHandlerSpec of(@NonNull final NatsTemplate natsTemplate) {
    NatsMessageProducingHandlerSpec spec = new NatsMessageProducingHandlerSpec();
    spec.init(natsTemplate);
    return spec;
  }

  void init(@NonNull final NatsTemplate natsTemplate) {
    this.target = new NatsMessageProducingHandler(natsTemplate);
  }

  @Override
  protected NatsMessageProducingHandlerSpec id(final String idToSet) {
    this.target.setBeanName(idToSet);
    return super.id(idToSet);
  }
}
