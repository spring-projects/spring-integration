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

import io.nats.client.MessageHandler;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.lang.NonNull;
import org.springframework.messaging.support.GenericMessage;

/** Nats Message Driven Channel Adapter */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsMessageDrivenChannelAdapter extends MessageProducerSupport
    implements OrderlyShutdownCapable {

  private static final Log LOG = LogFactory.getLog(NatsMessageDrivenChannelAdapter.class);

  @NonNull private final AbstractNatsMessageListenerContainer natsMessageListenerContainer;

  @NonNull private final NatsMessageHandler<?> messageHandler;

  /**
   * Constructs an instance with MessageConverter to receive messages on output Channel
   *
   * @param pNatsMessageListenerContainer the container
   * @param pMessageConverter the messageConvertor
   */
  public NatsMessageDrivenChannelAdapter(
      @NonNull final AbstractNatsMessageListenerContainer pNatsMessageListenerContainer,
      @NonNull final MessageConverter<?> pMessageConverter) {
    this.natsMessageListenerContainer = pNatsMessageListenerContainer;
    this.messageHandler = new NatsMessageHandler<>(pMessageConverter);
    this.natsMessageListenerContainer.setAutoStartup(false);
  }

  /**
   * Constructs an instance without MessageConverter to receive NatsMessage directly on output
   * Channel
   *
   * @param pNatsMessageListenerContainer the container
   */
  public NatsMessageDrivenChannelAdapter(
      @NonNull final AbstractNatsMessageListenerContainer pNatsMessageListenerContainer) {
    this.natsMessageListenerContainer = pNatsMessageListenerContainer;
    this.messageHandler = new NatsMessageHandler<>();
    this.natsMessageListenerContainer.setAutoStartup(false);
  }

  @Override
  public String getComponentType() {
    return "nats:message-driven-channel-adapter";
  }

  @Override
  protected void onInit() {
    super.onInit();
    this.natsMessageListenerContainer.setMessageHandler(this.messageHandler);
  }

  @Override
  protected void doStart() {
    // starts container to trigger the polling to receive messages
    this.natsMessageListenerContainer.start();
  }

  @Override
  protected void doStop() {
    this.natsMessageListenerContainer.stop();
  }

  @Override
  public int beforeShutdown() {
    this.natsMessageListenerContainer.stop();
    return getPhase();
  }

  @Override
  public int afterShutdown() {
    return getPhase();
  }

  /**
   * Listener/Handler to receive the messages from container and trigger the sendMessage method to
   * send the messages to output channel
   *
   * @param <T> type to which the message should be converted
   */
  public class NatsMessageHandler<T> implements MessageHandler {

    private final MessageConverter<T> messageConverter;

    protected NatsMessageHandler() {
      this.messageConverter = null;
    }

    protected NatsMessageHandler(@NonNull final MessageConverter<T> pMessageConverter) {
      this.messageConverter = pMessageConverter;
    }

    @Override
    public void onMessage(final io.nats.client.Message msg) {
      // TODO-NATS: find a solution to clear this value in respective
      // flows.
      // Clears MDC context to refresh with context key from the headers
      // of new message
      // MdcUtils.removeBusJobContextId();
      msg.inProgress();
      if (msg.isJetStream()) {
        boolean isSuccess = false;
        try {
          final Map<String, Object> headers = NatsUtils.enrichMessageHeader(msg);
          if (this.messageConverter == null) {
            handleMessage(msg, headers);
          } else {
            T t = this.messageConverter.fromMessage(msg);
            // On successful conversion send the converted data to
            // outputChannel
            LOG.debug("NATS message converted in MessageHandler =>  DATA: " + t);
            handleMessage(t, headers);
          }
          // Acknowledge message when processing is done
          msg.ack();
          isSuccess = true;
        } catch (final IOException | RuntimeException e) {
          // we expect that any type of exception can happen
          // during message conversion and processing of messages
          // in business logic and do not want to stop consumer
          // polling process and it could be unexpected exception
          // we decide to take RuntimeException
          final String message =
              "Exception occurred while converting and sending Nats Message in adapter "
                  + getBeanName()
                  + " Message metaData: "
                  + msg.metaData();
          handleMessageProcessingError(message, e);
        } finally {
          final String result = isSuccess ? "successfully" : "unsuccessfully";
          LOG.debug(
              "NATS message received and processed "
                  + result
                  + " in MessageHandler, metaData: "
                  + msg.metaData());
        }
      } else if (msg.isStatusMessage()) {
        LOG.warn("Received Status: " + msg.getStatus());
      }
    }

    private void handleMessageProcessingError(final String message, final Exception e) {
      // log the error prone message and send the message to
      // errorChannel
      LOG.error(message, e);
      sendErrorMessageIfNecessary(new GenericMessage<>(message), e);
    }

    /**
     * Sends converted message to the output channel
     *
     * @param message converted message object
     * @param headersToCopy headers to cpoied to spring integration message
     */
    private void handleMessage(
        @NonNull final T message, @NonNull final Map<String, Object> headersToCopy) {
      sendMessage(MessageBuilder.withPayload(message).copyHeaders(headersToCopy).build());
    }

    /**
     * Sends NatsMessage directly to the output channel without conversion
     *
     * @param msg NATS message
     */
    private void handleMessage(
        @NonNull final io.nats.client.Message msg,
        @NonNull final Map<String, Object> headersToCopy) {
      sendMessage(MessageBuilder.withPayload(msg).copyHeaders(headersToCopy).build());
    }
  }
}
