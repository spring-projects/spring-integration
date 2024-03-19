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

import static org.junit.Assert.assertTrue;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Options;
import io.nats.client.impl.NatsImpl;
import io.nats.spring.boot.autoconfigure.NatsConnectionProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.dsl.Nats;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
@ContextConfiguration(classes = {NatsTestConfig.class, NatsOutputQueueTest.ContextConfig.class})
public class NatsOutputQueueTest extends AbstractNatsIntegrationTestSupport {

  private static final Log LOG = LogFactory.getLog(NatsOutputQueueTest.class);

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired
  @Qualifier("producerChannelOutputQueueFullFlow")
  private DirectChannel producerChannelOutputQueueFullFlow;

  /**
   * This is to test the behavior of NATS clients output message queue {@link
   * io.nats.client.impl.MessageQueue}
   *
   * <p>Test scenario: To produce more messages than the configured output queue capacity {@link
   * ContextConfig#natsTestConnection(NatsConnectionProperties)} Result expected: The NATS client
   * should throw java.lang.IllegalStateException: Output queue is full
   *
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testOutputQueueOverflow() throws InterruptedException {
    // stopping nats server
    stopNatsServer();
    // sleep 10 seconds to let the nats server shutdown
    Thread.sleep(10000);
    LOG.info("NATS server stopped.");
    // trying to produce more messages(200) than output queue capacity(100)
    try {
      for (int i = 1; i <= 200; i++) {
        this.producerChannelOutputQueueFullFlow.send(
            MessageBuilder.withPayload("Hello " + i).build());
      }
      Assert.fail();
    } catch (Exception ex) {
      // This is the expected behavior of NATS client when internal output queue overflows
      assertTrue(ex.getMessage().contains("Output queue is full"));
    }
  }

  /**
   * Configuration class to setup Beans required to initialize NATS Message producers using NATS dsl
   * classes
   */
  @Configuration
  @EnableIntegration
  @IntegrationComponentScan
  public static class ContextConfig {

    @Value("${org.springframework.integration.nats.credentials}")
    private Resource natsCredentials;

    @Autowired
    private @Qualifier("natsTestConnection") Connection natsTestConnection;

    @Bean
    public Connection natsTestConnection(
        @NonNull final NatsConnectionProperties natsEncryptedConnectionProperties)
        throws GeneralSecurityException, IOException, InterruptedException {
      try {
        Options.Builder builder = natsEncryptedConnectionProperties.toOptionsBuilder();
        builder.authHandler(NatsImpl.credentials(natsCredentials.getFilename()));
        builder.maxMessagesInOutgoingQueue(100);
        return io.nats.client.Nats.connect(builder.build());
      } catch (final GeneralSecurityException | IOException | InterruptedException e) {
        LOG.error("error connecting to nats", e);
        throw e;
      }
    }

    @Bean
    public NatsTemplate natsTemplate() {
      return new NatsTemplate(this.natsTestConnection, TEST_SUBJECT, messageConvertor());
    }

    @Bean
    public MessageChannel producerChannelOutputQueueFullFlow() {
      return MessageChannels.direct().getObject();
    }

    @Bean
    public IntegrationFlow outboundOutputQueueFullFlow() {
      return IntegrationFlow.from(producerChannelOutputQueueFullFlow())
          .log(LoggingHandler.Level.INFO)
          .handle(Nats.outboundAsyncProducingHandler(natsTemplate()))
          .get();
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }

    @PostConstruct
    public void streamSetup() throws IOException, JetStreamApiException {
      createStreamConfig(this.natsTestConnection, TEST_STREAM, TEST_SUBJECT);
    }
  }
}
