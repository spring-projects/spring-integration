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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.nats.converter.MessageConverter;

/** Unit test cases for the NatsTemplate class. */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsTemplateTest {

  /**
   * Unit test to validate {@link NatsTemplate#send(Object)} using mock objects and verified the
   * expected invocations.
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testSend() throws IOException, JetStreamApiException {
    final Connection connection = mock(Connection.class);
    final JetStream jetstream = mock(JetStream.class);
    final NatsTemplate natsTemplate =
        new NatsTemplate(connection, "Junit", new MessageConverter<>(String.class));
    when(connection.jetStream()).thenReturn(jetstream);
    when(jetstream.publish(any(String.class), any(byte[].class)))
        .thenReturn(mock(PublishAck.class));
    final PublishAck ack = natsTemplate.send("Test Message");
    verify(connection, times(1)).jetStream();
    verify(jetstream, times(1)).publish(any(String.class), any(byte[].class));
    Assert.assertNotNull(ack);
  }

  /**
   * Unit test to validate {@link NatsTemplate#send(Object, Headers)} using mock objects and
   * verified the expected invocations.
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testSendWithHeaders() throws IOException, JetStreamApiException {
    final Connection connection = mock(Connection.class);
    final JetStream jetstream = mock(JetStream.class);
    final MessageConverter converter = mock(MessageConverter.class);
    final NatsTemplate natsTemplate = new NatsTemplate(connection, "Junit", converter);
    when(connection.jetStream()).thenReturn(jetstream);
    when(jetstream.publish(any(Message.class))).thenReturn(mock(PublishAck.class));
    final PublishAck ack = natsTemplate.send("Test Message", mock(Headers.class));
    verify(connection, times(1)).jetStream();
    verify(jetstream, times(1)).publish(any(Message.class));
    Assert.assertNotNull(ack);
  }
}
