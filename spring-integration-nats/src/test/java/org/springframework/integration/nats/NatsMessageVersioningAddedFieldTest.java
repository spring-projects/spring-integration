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
import static org.junit.Assert.assertNotNull;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import java.io.IOException;
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
import org.springframework.integration.nats.config.GeneralContextConfig;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.config.Stub1ContextConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dto.TestDTOStubV1;
import org.springframework.integration.nats.dto.TestDTOStubV2;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the flow using Producer and Consumer bean configuration without DSL
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
      Stub1ContextConfig.class,
      NatsMessageVersioningAddedFieldTest.BusConfig.class
    })
public class NatsMessageVersioningAddedFieldTest extends AbstractNatsIntegrationTestSupport {

  @Autowired private Connection natsConnection;

  @Autowired private BusConfig.ControlBusGateway controlBus;

  @Autowired
  @Qualifier("consumerChannel")
  private PollableChannel consumerChannel;

  @Autowired
  @Qualifier("messageConvertorOfDTOStub")
  private MessageConverter<TestDTOStubV1> messageConvertorOfDTOStub;

  @PostConstruct
  public void streamSetup() throws IOException, JetStreamApiException {
    AbstractNatsIntegrationTestSupport.createStreamConfig(
        this.natsConnection, GeneralContextConfig.TEST_STREAM, GeneralContextConfig.TEST_SUBJECT);
  }

  /**
   * Tests the positive flow using NatsTemplate and NatsMessageDrivenChannelAdapter bean
   * configuration without DSL
   *
   * <p>Test Scenario: Publish messages to subject using NatsTemplate ResultExpected: Messages
   * consumed via adapter should be available in the consumer channel
   *
   * <p>Beans are configured below in {@link Stub1ContextConfig#positiveFlow()}
   *
   * @throws IOException covers various communication issues with the NATS server such as timeout or
   *     interruption
   * @throws JetStreamApiException the request had an error related to the data
   */
  @Test
  public void testMessageVersioningFlow() throws IOException, JetStreamApiException {
    // start adapter using messaging gateway
    this.controlBus.send("@testAdapter.start()");
    // Message Producer -> publish messages to subject using NatsTemplate
    caseAddedNewUnknowFieldInMessage();
    // stop adapter using messaging gateway
    this.controlBus.send("@testAdapter.stop()");
  }

  /**
   * Test shows backward compatibility between sender and consumer in case when sender sends newer
   * version of DTO object, when on other hand the consumer still works with older version of DTO
   * object. The default converter is capable in general to ignore unknown properties during
   * deserialization time.
   *
   * @throws IOException
   * @throws JetStreamApiException
   */
  public void caseAddedNewUnknowFieldInMessage() throws IOException, JetStreamApiException {
    // Message Producer -> publish messages to subject using NatsTemplate using newer version 2 of
    // DTO
    final NatsTemplate natsTemplate =
        new NatsTemplate(
            this.natsConnection, GeneralContextConfig.TEST_SUBJECT, messageConvertorOfDTOStub);

    for (int i = 0; i < 5; i++) {
      // sending DTO of Version 2
      final PublishAck ack =
          natsTemplate.send(new TestDTOStubV2("Hello" + i, "I am new unknown property"));
      assertNotNull(ack);
      assertEquals(GeneralContextConfig.TEST_STREAM, ack.getStream());
    }

    // Message Consumer Assertion -> Messages consumed via adapter should be
    // available in the consumer channel
    // consumer still works with older version DTO. The test shows that deserialization will not
    // fail
    // in spite of the fact JSON contains unknown properties.
    for (int i = 0; i < 5; i++) {
      final Message<?> message = this.consumerChannel.receive(20000);
      assertNotNull(message);
      // receiving and converting to the version 1 without impact. The new field of version 2 is
      // ignored
      final TestDTOStubV1 payload = (TestDTOStubV1) message.getPayload();
      assertEquals("Hello" + i, payload.getProperty());
    }
  }

  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
  @Repository
  public static class BusConfig {
    @Bean
    public IntegrationFlow controlBus() {
      return IntegrationFlowDefinition::controlBus;
    }

    /** Gateway interface used to start and stop adapter(spring integration) components. */
    @MessagingGateway(defaultRequestChannel = "controlBus.input")
    private interface ControlBusGateway {

      void send(String command);
    }
  }
}
