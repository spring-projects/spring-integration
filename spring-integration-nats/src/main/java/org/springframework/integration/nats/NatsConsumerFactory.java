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
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.MessageHandler;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.Subscription;
import java.io.IOException;
import org.springframework.lang.NonNull;

/** ConsumerFactory to create subscription based on properties and delivery mode */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsConsumerFactory {

  @NonNull private final Connection natsConnection;

  @NonNull private final ConsumerProperties consumerProperties;

  public NatsConsumerFactory(
      @NonNull final Connection pNatsConnection,
      @NonNull final ConsumerProperties pConsumerProperties) {
    this.natsConnection = pNatsConnection;
    this.consumerProperties = pConsumerProperties;
  }

  /**
   * Constructs a push based synchronous subscription
   *
   * @return Subscription - push based synchronous subscription
   * @throws IOException - covers various communication issues with the NATS server such as timeout
   *     or interruption
   * @throws JetStreamApiException - the request had an error related to the data
   */
  @NonNull
  public Subscription createSyncPushSubscription() throws IOException, JetStreamApiException {
    final JetStream jetStream = this.natsConnection.jetStream();
    final PushSubscribeOptions pushOptions =
        PushSubscribeOptions.builder()
            .durable(this.consumerProperties.getConsumerName())
            .deliverGroup(this.consumerProperties.getQueueGroup())
            .build();
    final JetStreamSubscription subscription =
        jetStream.subscribe(this.consumerProperties.getSubject(), pushOptions);
    return subscription;
  }

  /**
   * Constructs a push based asynchronous subscription
   *
   * @param messageHandler the messageHandler
   * @return Subscription - push based asynchronous subscription
   * @throws IOException - covers various communication issues with the NATS server such as timeout
   *     or interruption
   * @throws JetStreamApiException - the request had an error related to the data
   */
  @NonNull
  public Subscription createAsyncPushSubscription(@NonNull final MessageHandler messageHandler)
      throws IOException, JetStreamApiException {
    final JetStream jetStream = this.natsConnection.jetStream();
    final Dispatcher dispatcher = this.natsConnection.createDispatcher();
    final PushSubscribeOptions pushOptions =
        PushSubscribeOptions.builder()
            .durable(this.consumerProperties.getConsumerName())
            .deliverGroup(this.consumerProperties.getQueueGroup())
            .build();
    final JetStreamSubscription subscription =
        jetStream.subscribe(
            this.consumerProperties.getSubject(), dispatcher, messageHandler, false, pushOptions);
    return subscription;
  }

  /**
   * Constructs a pull based synchronous subscription
   *
   * @return Subscription - pull based synchronous subscription
   * @throws IOException - covers various communication issues with the NATS server such as timeout
   *     or interruption
   * @throws JetStreamApiException - the request had an error related to the data
   */
  @NonNull
  public Subscription createSyncPullSubscription() throws IOException, JetStreamApiException {
    final JetStream js = this.natsConnection.jetStream();
    // Build our subscription options. Durable is REQUIRED for pull based
    // subscriptions
    final PullSubscribeOptions pullOptions =
        PullSubscribeOptions.builder().durable(this.consumerProperties.getConsumerName()).build();
    final JetStreamSubscription subscription =
        js.subscribe(this.consumerProperties.getSubject(), pullOptions);
    return subscription;
  }

  /**
   * Constructs a push based asynchronous subscription for core NATS subject
   *
   * @param messageHandler the messageHandler to process incoming messages
   * @return push subscription
   * @throws IOException
   */
  @NonNull
  public Subscription createAsyncCorePushSubscription(@NonNull final MessageHandler messageHandler)
      throws IOException {
    final Dispatcher dispatcher = this.natsConnection.createDispatcher();
    return dispatcher.subscribe(this.consumerProperties.getSubject(), messageHandler);
  }

  @NonNull
  public ConsumerProperties getConsumerProperties() {
    return this.consumerProperties;
  }
}
