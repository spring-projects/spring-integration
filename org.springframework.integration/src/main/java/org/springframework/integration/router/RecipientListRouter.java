/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * <pre>
 * &lt;recipient-list-router id="simpleRouter" input-channel="routingChannelA"&gt;
 *     &lt;recipient channel="channel1"/&gt;
 *     &lt;recipient channel="channel2"/&gt;
 * &lt;/recipient-list-router>
 * </pre>
 * <p/>
 * A Message Router that sends Messages to a list of recipient channels. The
 * recipients can be provided as a static list of {@link MessageChannel}
 * instances via the {@link #setChannels(List)} method, or for dynamic
 * behavior, a map with {@link MessageSelector} instances as the keys and
 * collections of channels as the values can be provided via the
 * {@link #setChannelMap(Map)} method.
 * <p/>
 * For more advanced, programmatic control
 * of dynamic recipient lists, consider using the @Router annotation or
 * extending {@link AbstractChannelNameResolvingMessageRouter} instead.
 * <p/>
 * Contrary to a standard &lt;router .../&gt; this handler will try to send to all channels that are configured as
 * recipients. It is to channels what a publish subscribe channel is to endpoints.
 * <p/>
 * Using this class only makes sense if it is essential to send messages on multiple channels instead of
 * sending them to multiple handlers. If the latter is an option using a publish subscribe channel is the more flexible
 * solution.
 *
 * @author Mark Fisher
 */
public class RecipientListRouter extends AbstractMessageHandler implements InitializingBean {

    public static final String COMPONENT_TYPE_LABEL = "recipient-list-router";


	private volatile boolean ignoreSendFailures;

    private volatile boolean applySequence;

    private volatile Map<MessageSelector, ? extends Collection<MessageChannel>> channelMap;

    private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();


    /**
     * Set the output channels of this router.
     *
     * @param channels
     */
    public void setChannels(List<MessageChannel> channels) {
        Assert.notEmpty(channels, "channels must not be empty");
        MessageSelector selector = new MessageSelector() {
            public boolean accept(Message<?> message) {
                return true;
            }
        };
        this.setChannelMap(Collections.singletonMap(selector, channels));
    }

    /**
     * Set a custom map of selectors to channels. This allows a configuration where groups of channels are used for
     * messages matching a particular filter.
     *
     * @param channelMap
     */
    public void setChannelMap(Map<MessageSelector, ? extends Collection<MessageChannel>> channelMap) {
        this.channelMap = channelMap;
    }

    /**
     * Set the timeout for sending a message to the resolved channel(s). By
     * default, there is no timeout, meaning the send will block indefinitely.
     */
    public void setTimeout(long timeout) {
        this.channelTemplate.setSendTimeout(timeout);
    }

    /**
     * Specify whether send failures for one or more of the recipients
     * should be ignored. By default this is <code>false</code> meaning
     * that an Exception will be thrown whenever a send fails. To override
     * this and suppress Exceptions, set the value to <code>true</code>.
     */
    public void setIgnoreSendFailures(boolean ignoreSendFailures) {
        this.ignoreSendFailures = ignoreSendFailures;
    }

    /**
     * Specify whether to apply the sequence number and size headers to the
     * messages prior to sending to the recipient channels. By default, this
     * value is <code>false</code> meaning that sequence headers will
     * <em>not</em> be applied. If planning to use an Aggregator downstream
     * with the default correlation and completion strategies, you should set
     * this flag to <code>true</code>.
     */
    public void setApplySequence(boolean applySequence) {
        this.applySequence = applySequence;
    }

    public void afterPropertiesSet() {
        Assert.notEmpty(this.channelMap, "a non-empty channel map is required");
    }

    @Override
    protected void handleMessageInternal(Message<?> message) throws Exception {
        List<MessageChannel> recipients = new ArrayList<MessageChannel>();
        Map<MessageSelector, Collection<MessageChannel>> map =
                new HashMap<MessageSelector, Collection<MessageChannel>>(this.channelMap);
        for (MessageSelector selector : map.keySet()) {
            if (selector.accept(message)) {
                recipients.addAll(map.get(selector));
            }
        }
        int sequenceSize = recipients.size();
        int sequenceNumber = 1;
        for (MessageChannel channel : recipients) {
            final Message<?> messageToSend = (!this.applySequence) ? message
                    : MessageBuilder.fromMessage(message)
                    .setSequenceNumber(sequenceNumber++)
                    .setSequenceSize(sequenceSize)
                    .setCorrelationId(message.getHeaders().getId())
                    .setHeader(MessageHeaders.ID, UUID.randomUUID())
                    .build();
            boolean sent = this.channelTemplate.send(messageToSend, channel);
            if (!sent && !this.ignoreSendFailures) {
                throw new MessageDeliveryException(message,
                        "RecipientListRouter failed to send to channel: " + channel);
            }
        }
    }

}
