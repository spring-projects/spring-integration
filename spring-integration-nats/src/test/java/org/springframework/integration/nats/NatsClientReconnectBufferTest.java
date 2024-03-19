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
import io.nats.client.Options;
import io.nats.client.impl.NatsImpl;
import io.nats.spring.boot.autoconfigure.NatsConnectionProperties;
import java.io.IOException;
import java.security.GeneralSecurityException;
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
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.nats.config.NatsTestConfig;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.support.AbstractNatsIntegrationTestSupport;
import org.springframework.lang.NonNull;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * During a short reconnect, the client will allow applications to publish messages, because the
 * server is offline, messages will be cached in the client. The client library will then send those
 * messages once reconnected. When the maximum reconnect buffer is reached, messages will no longer
 * be publishable by the client and an error will be returned.
 *
 * <p>More info: https://docs.nats.io/using-nats/developer/connecting/reconnect/buffer
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
    classes = {NatsTestConfig.class, NatsClientReconnectBufferTest.ContextConfig.class})
public class NatsClientReconnectBufferTest extends AbstractNatsIntegrationTestSupport {

  private static final Log LOG = LogFactory.getLog(NatsClientReconnectBufferTest.class);

  private static final String TEST_SUBJECT = "test-subject";

  private static final String TEST_STREAM = "test-stream";

  @Autowired
  @Qualifier("natsTemplateDefaultBuffer")
  private NatsTemplate natsTemplateDefaultBuffer;

  @Autowired
  @Qualifier("natsTemplateCustomBuffer")
  private NatsTemplate natsTemplateCustomBuffer;

  /**
   * This is to test the default NATS reconnect buffer capacity. The default reconnect buffer size
   * is 8 * 1024 * 1024(8 MB)
   *
   * <p>Test scenario: To produce more messages(100000) which exceeds default reconnect buffer
   * size{@link
   * NatsClientReconnectBufferTest.ContextConfig#natsConnectionDefaultBuffer(NatsConnectionProperties)}
   * Result expected: The NATS client should throw java.lang.IllegalStateException: Unable to queue
   * any more messages during reconnect
   *
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testReconnectBufferWithDefaultBufferSize() throws InterruptedException {
    // stopping nats server
    stopNatsServer();
    // sleep 10 seconds to let the nats server shutdown
    Thread.sleep(10000);
    LOG.info("NATS server stopped.");
    // trying to produce 100000 messages to client buffer to fill the default reconnect buffer of 8
    // MB
    try {
      for (int i = 1; i <= 100000; i++) {
        this.natsTemplateDefaultBuffer.sendAsync("Hello " + i);
      }
      Assert.fail();
    } catch (Exception ex) {
      // This is the expected behavior of NATS client when reconnect buffer is full
      assertTrue(ex.getMessage().contains("Unable to queue any more messages during reconnect"));
    }
  }

  /**
   * This is to test the NATS reconnect buffer by increasing the buffer capacity to 16 * 1024 *
   * 1024(16 MB) to hold 100000 messages
   *
   * <p>Test scenario: Try to produce 100000 messages and see if this is accommodated in client
   * buffer. Result expected: The NATS client buffer should successfully hold 100000 messages.
   *
   * @throws InterruptedException if any thread has interrupted the current thread. The interrupted
   *     status of the current thread is cleared when this exception is thrown.
   */
  @Test
  public void testReconnectBufferWithCustomBufferSize() throws InterruptedException {
    // stopping nats server
    stopNatsServer();
    // sleep 10 seconds to let the nats server shutdown
    Thread.sleep(10000);
    LOG.info("NATS server stopped.");
    // trying to produce more 100000 messages to client buffer
    try {
      for (int i = 1; i <= 100000; i++) {
        this.natsTemplateCustomBuffer.sendAsync("Hello " + i);
      }
    } catch (Exception ex) {
      Assert.fail();
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
    private @Qualifier("natsConnectionDefaultBuffer") Connection natsConnectionDefaultBuffer;

    @Autowired
    private @Qualifier("natsConnectionCustomBuffer") Connection natsConnectionCustomBuffer;

    @Bean
    public Connection natsConnectionDefaultBuffer(
        @NonNull final NatsConnectionProperties natsEncryptedConnectionProperties)
        throws GeneralSecurityException, IOException, InterruptedException {
      try {
        Options.Builder builder = natsEncryptedConnectionProperties.toOptionsBuilder();
        builder.authHandler(NatsImpl.credentials(natsCredentials.getFilename()));
        builder.maxMessagesInOutgoingQueue(100100);
        // default client reconnect buffer size of 8 MB
        builder.reconnectBufferSize(8 * 1024 * 1024);
        return io.nats.client.Nats.connect(builder.build());
      } catch (final GeneralSecurityException | IOException | InterruptedException e) {
        LOG.error("error connecting to nats", e);
        throw e;
      }
    }

    @Bean
    public NatsTemplate natsTemplateDefaultBuffer() {
      return new NatsTemplate(this.natsConnectionDefaultBuffer, TEST_SUBJECT, messageConvertor());
    }

    @Bean
    public Connection natsConnectionCustomBuffer(
        @NonNull final NatsConnectionProperties natsEncryptedConnectionProperties)
        throws GeneralSecurityException, IOException, InterruptedException {
      try {
        Options.Builder builder = natsEncryptedConnectionProperties.toOptionsBuilder();
        builder.authHandler(NatsImpl.credentials(natsCredentials.getFilename()));
        builder.maxMessagesInOutgoingQueue(100100);
        // increased client reconnect buffer size to 16 MB
        builder.reconnectBufferSize(16 * 1024 * 1024);
        return io.nats.client.Nats.connect(builder.build());
      } catch (final GeneralSecurityException | IOException | InterruptedException e) {
        LOG.error("error connecting to nats", e);
        throw e;
      }
    }

    @Bean
    public NatsTemplate natsTemplateCustomBuffer() {
      return new NatsTemplate(this.natsConnectionCustomBuffer, TEST_SUBJECT, messageConvertor());
    }

    @Bean
    public MessageConverter<String> messageConvertor() {
      return new MessageConverter<>(String.class);
    }
  }
}
