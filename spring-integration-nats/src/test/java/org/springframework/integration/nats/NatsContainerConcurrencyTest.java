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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.impl.NatsJetStreamSubscription;
import io.nats.client.impl.NatsMessage;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

/** Unit testing of concurrent container */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsContainerConcurrencyTest {

  private static final String TEST_CONTAINER = "test-container";

  private static final String CONCURRENT_CONTAINER = "concurrent-container";

  final Message natsMessage1 =
      NatsMessage.builder()
          .data("Hello_1".getBytes())
          .subject("validSubjectForPullSubscription")
          .build();

  final Message natsMessage2 =
      NatsMessage.builder()
          .data("Hello_2".getBytes())
          .subject("validSubjectForPullSubscription")
          .build();

  /**
   * Test concurrent container with Default concurrency - 1
   *
   * <p>Checks if the parent and child containers are created, started and stopped correctly
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testBasicFunctionalityWithDefaultConcurrency()
      throws IOException, InterruptedException, JetStreamApiException {
    // Configure Mock for Jetstream context and subscription
    final NatsJetStreamSubscription pushSubscription = mock(NatsJetStreamSubscription.class);
    final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
    final ConsumerProperties consumerProperties =
        new ConsumerProperties("validStream", "validSubject", "valid-consumer", "valid-group");
    when(consumerFactory.getConsumerProperties()).thenReturn(consumerProperties);
    when(consumerFactory.createSyncPushSubscription()).thenReturn(pushSubscription);
    // Configuration of adapter and container components
    consumerProperties.setConsumerMaxWait(Duration.ofMillis(100));
    final NatsConcurrentListenerContainer container =
        new NatsConcurrentListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_SYNC);
    container.setBeanName(TEST_CONTAINER);
    final NatsMessageDrivenChannelAdapter adapter = new NatsMessageDrivenChannelAdapter(container);
    adapter.setBeanName("test-adapter");
    // Assert that adapter and container is not running before start
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
    // Initialize and start adapter
    adapter.onInit();
    adapter.start();
    // Assert that adapter and container is running after start
    assertTrue(adapter.isRunning());
    assertTrue(container.isRunning());
    // check if concurrent child container is started and running
    container.getContainers().forEach((childContainer) -> assertTrue(childContainer.isRunning()));
    // Check if one thread is created with name specified - default
    // concurrency is 1
    final Set<Thread> tSet = Thread.getAllStackTraces().keySet();
    // Naming convention: test-container-0-nats-C-1 =>
    // <beanID>-<concurrency>-nats-C-<thread-suffix-generated>
    final Optional<Thread> optionalThread =
        tSet.stream()
            .filter(t -> (TEST_CONTAINER + "-0-nats-C-1").equalsIgnoreCase(t.getName()))
            .findFirst();
    assertTrue(optionalThread.isPresent());
    final Thread thread = optionalThread.get();
    // check if the thread is alive
    assertTrue(thread.isAlive());
    // stop the adapter
    adapter.stop();
    // Assert that adapter and container is not running after stop
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
    // check if concurrent child container is stopped as well
    container.getContainers().forEach((childContainer) -> assertTrue(childContainer.isRunning()));
    // check if the thread is stopped after container stop, wait for few
    // seconds
    Thread.sleep(2000);
    assertFalse(thread.isAlive());
  }

  /**
   * Test concurrent container with concurrency - 3
   *
   * <p>Checks if the parent and child containers are created, started and stopped correctly
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testBasicFunctionalityWithConcurrency()
      throws IOException, InterruptedException, JetStreamApiException {
    // Configure Mock for Jetstream context and subscription
    final NatsJetStreamSubscription pushSubscription = mock(NatsJetStreamSubscription.class);
    final NatsConsumerFactory consumerFactory = mock(NatsConsumerFactory.class);
    final ConsumerProperties consumerProperties =
        new ConsumerProperties("validStream", "validSubject", "valid-consumer", "valid-group");
    when(consumerFactory.getConsumerProperties()).thenReturn(consumerProperties);
    when(consumerFactory.createSyncPushSubscription()).thenReturn(pushSubscription);
    // Configuration of adapter and container components
    consumerProperties.setConsumerMaxWait(Duration.ofMillis(100));
    final NatsConcurrentListenerContainer container =
        new NatsConcurrentListenerContainer(consumerFactory, NatsMessageDeliveryMode.PUSH_SYNC);
    container.setBeanName(CONCURRENT_CONTAINER);
    container.setConcurrency(3);
    final NatsMessageDrivenChannelAdapter adapter = new NatsMessageDrivenChannelAdapter(container);
    adapter.setBeanName("concur-adapter");
    // Assert that adapter and container is not running before start
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
    // Initialize and start adapter
    adapter.onInit();
    adapter.start();
    // Assert that adapter and container is running after start
    assertTrue(adapter.isRunning());
    assertTrue(container.isRunning());
    // check if concurrent containers are started and running
    container.getContainers().forEach((childContainer) -> assertTrue(childContainer.isRunning()));
    // Check if the thread is created with name specified
    final Set<Thread> tSet = Thread.getAllStackTraces().keySet();
    final List<Thread> threadsCreated =
        tSet.stream()
            .filter(t -> t.getName().contains(CONCURRENT_CONTAINER))
            .collect(Collectors.toList());
    assertEquals(3, threadsCreated.size());
    // check if the thread is alive
    threadsCreated.forEach((thread) -> assertTrue(thread.isAlive()));
    // stop the adapter
    adapter.stop();
    // Assert that adapter and container is not running after stop
    assertFalse(adapter.isRunning());
    assertFalse(container.isRunning());
    // check if concurrent child containers are stopped as well
    container.getContainers().forEach((childContainer) -> assertFalse(childContainer.isRunning()));
    // check if the thread is stopped after container stop, wait for few
    // seconds
    Thread.sleep(2000);
    threadsCreated.forEach((thread) -> assertFalse(thread.isAlive()));
  }
}
