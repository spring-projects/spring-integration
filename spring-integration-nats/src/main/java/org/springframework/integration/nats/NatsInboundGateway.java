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

import io.nats.client.JetStreamApiException;
import io.nats.client.MessageHandler;
import io.nats.client.impl.Headers;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;

/** Nats Inbound Gateway - Core NATS consumer and produce reply messages */

/**
 * @author Viktor Rohlenko - lead and architect
 * @author Vennila Pazhamalai - maintainer
 * @author Vivek Duraisamy - maintainer
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 * @since 6.4.x
 */
public class NatsInboundGateway extends MessagingGatewaySupport implements OrderlyShutdownCapable {

  private static final Log LOG = LogFactory.getLog(NatsInboundGateway.class);

  private final AbstractNatsMessageListenerContainer container;

  private final NatsTemplate natsTemplate;

  private final NatsCoreMessageHandler natsCoreMessageHandler = new NatsCoreMessageHandler();
  private final MessageConverter messageConverter;
  private boolean extractRequestPayload = true;
  private boolean extractReplyPayload = true;

  public NatsInboundGateway(
      AbstractNatsMessageListenerContainer pContainer,
      MessageConverter pMessageConverter,
      NatsTemplate pNatsTemplate) {
    this.container = pContainer;
    this.messageConverter = pMessageConverter;
    this.natsTemplate = pNatsTemplate;
  }

  public boolean isExtractRequestPayload() {
    return extractRequestPayload;
  }

  public void setExtractRequestPayload(boolean extractRequestPayload) {
    this.extractRequestPayload = extractRequestPayload;
  }

  public boolean isExtractReplyPayload() {
    return extractReplyPayload;
  }

  public void setExtractReplyPayload(boolean extractReplyPayload) {
    this.extractReplyPayload = extractReplyPayload;
  }

  @Override
  protected void onInit() {
    super.onInit();
    this.container.setMessageHandler(this.natsCoreMessageHandler);
  }

  @Override
  protected void doStart() {
    super.doStart();
    this.container.start();
  }

  @Override
  protected void doStop() {
    super.doStop();
    this.container.stop();
  }

  @Override
  public void destroy() {
    this.container.stop();
    super.destroy();
  }

  @Override
  public int beforeShutdown() {
    return 0;
  }

  @Override
  public int afterShutdown() {
    return 0;
  }

  /**
   * Message Handler to process core NATS messages and send reply
   *
   * @param <T>
   */
  public class NatsCoreMessageHandler<T> implements MessageHandler {

    /** Determine a reply subject for the given message. */
    private String getReplyToSubject(io.nats.client.Message message) {
      String replyTo = message.getReplyTo();
      if (replyTo == null) {
        throw new IllegalArgumentException(
            "Cannot determine reply destination: "
                + "Request message does not contain reply-to destination, and no default reply destination set.");
      }
      return replyTo;
    }

    private void sendReply(Object replyMessage, Headers headers, String replyToSubject)
        throws JetStreamApiException, IOException, InterruptedException {
      natsTemplate.publishReply(replyMessage, headers, replyToSubject);
    }

    @Override
    public void onMessage(io.nats.client.Message msg) throws InterruptedException {
      org.springframework.messaging.Message<?> requestMessage;
      try {
        final Object result;
        if (extractRequestPayload) {
          result = messageConverter.fromMessage(msg);
          LOG.debug(
              "converted NATS Message ["
                  + msg
                  + "] to integration Message payload ["
                  + result
                  + "]");
        } else {
          result = msg;
        }

        Map<String, Object> headers = NatsUtils.enrichMessageHeader(msg);
        requestMessage =
            (result instanceof org.springframework.messaging.Message<?>)
                ? getMessageBuilderFactory()
                    .fromMessage((org.springframework.messaging.Message<?>) result)
                    .copyHeaders(headers)
                    .build()
                : getMessageBuilderFactory().withPayload(result).copyHeaders(headers).build();
      } catch (RuntimeException | IOException e) {
        MessageChannel errorChannel = getErrorChannel();
        if (errorChannel == null) {
          throw new RuntimeException(e);
        }
        messagingTemplate.send(
            errorChannel,
            buildErrorMessage(
                null, new MessagingException("Inbound conversion failed for: " + msg, e)));
        return;
      }

      org.springframework.messaging.Message<?> replyMessage = null;
      try {
        replyMessage = sendAndReceiveMessage(requestMessage);
        if (replyMessage != null) {
          String replyToSubject = getReplyToSubject(msg);
          LOG.debug("Reply Message: " + replyMessage);
          LOG.debug("Reply Subject: " + replyToSubject);
          final Object replyResult;
          if (extractReplyPayload) {
            replyResult = replyMessage.getPayload();
          } else {
            replyResult = replyMessage;
          }
          // copyCorrelationIdFromRequestToReply(jmsMessage, jmsReply);
          sendReply(
              replyResult, NatsUtils.populateNatsMessageHeaders(replyMessage), replyToSubject);
        } else {
          LOG.info(" REPLY************* NULL *************expected a reply but none was received");
        }
      } catch (RuntimeException | JetStreamApiException | IOException e) {
        handleError(msg, replyMessage, e);
      }
    }

    private void handleError(
        io.nats.client.Message msg,
        org.springframework.messaging.Message<?> replyMessage,
        Exception e) {
      org.springframework.messaging.Message<?> finalReplyMessage = replyMessage;
      LOG.error("Failed to generate and send NATS Reply Message from: " + finalReplyMessage, e);
      MessageChannel errorChannel = getErrorChannel();
      if (errorChannel == null) {
        throw new RuntimeException(e);
      }
      messagingTemplate.send(
          errorChannel,
          buildErrorMessage(
              null,
              new MessagingException(
                  "Failed to generate and send NATS Reply Message from  " + msg, e)));
    }
  }
}
