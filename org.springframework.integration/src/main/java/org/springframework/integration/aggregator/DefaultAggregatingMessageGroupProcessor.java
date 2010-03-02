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

package org.springframework.integration.aggregator;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This implementation of MessageGroupProcessor will take the messages from the MessageGroup and pass them on in a
 * single message with a Collection as a payload.
 *
 * @author Iwein Fuld
 * @author Alexander Peters
 * @since 2.0.0
 */
public class DefaultAggregatingMessageGroupProcessor implements MessageGroupProcessor {
    public void processAndSend(MessageGroup group, MessageChannelTemplate channelTemplate, MessageChannel outputChannel) {
        Assert.notNull(group, "Message group must not be null.");
        Assert.notNull(outputChannel, "'outputChannel' must not be null.");

        List<Message<?>> messages = group.getMessages();

        Assert.notEmpty(messages, this.getClass().getSimpleName() + " cannot process empty message groups");

        channelTemplate.send(aggregateMessages(messages), outputChannel);

    }

    private Message<? extends Collection> aggregateMessages(List<Message<?>> messages) {
        List payloads = new ArrayList(messages.size());
        for (Message<?> message : messages) {
            payloads.add(message.getPayload());
        }
        return MessageBuilder.withPayload(payloads).build();
    }
}
