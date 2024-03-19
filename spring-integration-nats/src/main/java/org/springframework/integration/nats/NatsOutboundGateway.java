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
import java.io.IOException;
import java.time.Duration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

/**
 * Nats OutboundGateway - Reply Producing Message Handler
 *
 * <p>Implemented using core NATS request reply pattern
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
public class NatsOutboundGateway extends AbstractReplyProducingMessageHandler {

  private static final Log LOG = LogFactory.getLog(NatsOutboundGateway.class);
  private static final long DEFAULT_TIMEOUT = 1000L;
  @NonNull private final NatsTemplate natsTemplate;
  private final MessageConverter messageConverter;

  /**
   * Specify a timeout in milliseconds for how long this NatsOutboundGateway should wait for reply
   */
  private long replyTimeout = DEFAULT_TIMEOUT;

  private MessageChannel errorChannel;
  private String errorChannelName;
  private MessageChannel replyChannel;
  private String replyChannelName;
  private boolean extractRequestPayload = true;

  private boolean extractReplyPayload = true;

  public NatsOutboundGateway(
      @NonNull final NatsTemplate pNatsTemplate, MessageConverter messageConverter) {
    this.natsTemplate = pNatsTemplate;
    this.messageConverter = messageConverter;
  }

  public long getReplyTimeout() {
    return replyTimeout;
  }

  public void setReplyTimeout(long replyTimeout) {
    this.replyTimeout = replyTimeout;
  }

  public MessageChannel getErrorChannel() {
    if (this.errorChannel != null) {
      return this.errorChannel;
    } else if (this.errorChannelName != null) {
      this.errorChannel = getChannelResolver().resolveDestination(this.errorChannelName);
      return this.errorChannel;
    }
    return null;
  }

  public void setErrorChannel(MessageChannel errorChannel) {
    this.errorChannel = errorChannel;
  }

  public void setErrorChannelName(String errorChannelName) {
    this.errorChannelName = errorChannelName;
  }

  public MessageChannel getReplyChannel() {
    if (this.replyChannel != null) {
      return this.replyChannel;
    } else if (this.replyChannelName != null) {
      this.replyChannel = getChannelResolver().resolveDestination(this.replyChannelName);
      return this.errorChannel;
    }
    return null;
  }

  public void setReplyChannel(MessageChannel replyChannel) {
    this.setOutputChannel(replyChannel);
    this.replyChannel = replyChannel;
  }

  public String getReplyChannelName() {
    return replyChannelName;
  }

  public void setReplyChannelName(String replyChannelName) {
    this.setOutputChannelName(replyChannelName);
    this.replyChannelName = replyChannelName;
  }

  /**
   * This property describes how a NATS Message should be generated from the Spring Integration
   * Message. If set to 'true', the body of the NATS Message will be created from the Spring
   * Integration Message's payload (via the MessageConverter). If set to 'false', then the entire
   * Spring Integration Message will serve as the base for NATS Message creation. Since the NATS
   * Message is created by the MessageConverter, this really manages what is sent to the {@link
   * MessageConverter}: the entire Spring Integration Message or only its payload. Default is
   * 'true'.
   *
   * @param extractRequestPayload true to extract the request payload.
   */
  public void setExtractRequestPayload(boolean extractRequestPayload) {
    this.extractRequestPayload = extractRequestPayload;
  }

  /**
   * This property describes what to do with a NATS reply Message. If set to 'true', the payload of
   * the Spring Integration Message will be created from the NATS Reply Message's body (via
   * MessageConverter). Otherwise, the entire NATS Message will become the payload of the Spring
   * Integration Message.
   *
   * @param extractReplyPayload true to extract the reply payload.
   */
  public void setExtractReplyPayload(boolean extractReplyPayload) {
    this.extractReplyPayload = extractReplyPayload;
  }

  /**
   * Supplied NatsTemplate(subject) is used to trigger request reply method in NATS core API.
   * Received response is then sent to the reply Channel configured in outbound gateway
   *
   * @param requestMessage The request message.
   * @return Response received from NATS server
   */
  @Override
  protected Object handleRequestMessage(Message<?> requestMessage) {
    Object payload = requestMessage;
    if (this.extractRequestPayload) {
      payload = requestMessage.getPayload();
    }
    if (payload != null) {
      try {
        LOG.debug("Requesting reply from subject: " + this.natsTemplate.getSubject());
        final io.nats.client.Message replyMessage =
            this.natsTemplate.requestReply(
                payload,
                NatsUtils.populateNatsMessageHeaders(requestMessage),
                Duration.ofMillis(this.replyTimeout));
        if (replyMessage == null) {
          String errorMessage =
              "failed to receive NATS response within timeout of: " + this.replyTimeout + " ms";
          LOG.debug(errorMessage);
          sendErrorMessage(
              requestMessage, new MessageTimeoutException(requestMessage, errorMessage));
		} else {
          LOG.debug(
              "Nats Message sent "
                  + requestMessage.getPayload()
                  + " and reply received: "
                  + replyMessage);
          if (this.extractReplyPayload) {
            return this.messageConverter.fromMessage(replyMessage);
          }
          return replyMessage;
        }
      } catch (IOException | JetStreamApiException e) {
        sendErrorMessage(requestMessage, e);
	  } catch (InterruptedException e) {
        sendErrorMessage(requestMessage, e);
      }
    }
    return null;
  }

  @Override
  protected void sendErrorMessage(Message<?> requestMessage, Throwable ex) {
    Throwable result = ex;
    if (!(ex instanceof MessagingException)) {
      result = new MessageHandlingException(requestMessage, ex);
    }
    if (getErrorChannel() == null) {
      super.sendErrorMessage(requestMessage, ex);
    } else {
      try {
        sendOutput(new ErrorMessage(result), errorChannel, true);
      } catch (Exception e) {
        Exception exceptionToLog =
            IntegrationUtils.wrapInHandlingExceptionIfNecessary(
                requestMessage, () -> "failed to send error message in the [" + this + ']', e);
        logger.error(exceptionToLog, "Failed to send reply");
      }
    }
  }
}
