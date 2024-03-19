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

import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.nats.NatsConcurrentListenerContainer;
import org.springframework.integration.nats.NatsConsumerFactory;
import org.springframework.integration.nats.NatsMessageDeliveryMode;
import org.springframework.lang.NonNull;

/**
 * A helper class in the Builder pattern style to delegate options to the {@link
 * NatsConcurrentListenerContainer}.
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
public class NatsMessageListenerContainerSpec
    extends IntegrationComponentSpec<
        NatsMessageListenerContainerSpec, NatsConcurrentListenerContainer> {

  public NatsMessageListenerContainerSpec(@NonNull final NatsConsumerFactory natsConsumerFactory) {
    this.target =
        new NatsConcurrentListenerContainer(natsConsumerFactory, NatsMessageDeliveryMode.PULL);
  }

  public NatsMessageListenerContainerSpec(
      @NonNull final NatsConsumerFactory natsConsumerFactory,
      @NonNull final NatsMessageDeliveryMode natsMessageDeliveryMode) {
    this.target = new NatsConcurrentListenerContainer(natsConsumerFactory, natsMessageDeliveryMode);
  }

  @Override
  public NatsMessageListenerContainerSpec id(final String idToSet) {
    this.target.setBeanName(idToSet);
    return super.id(idToSet);
  }

  public NatsMessageListenerContainerSpec concurrency(final int concurrency) {
    this.target.setConcurrency(concurrency);
    return this;
  }
}
