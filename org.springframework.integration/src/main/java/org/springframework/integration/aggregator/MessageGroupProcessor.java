package org.springframework.integration.aggregator;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.MessageChannel;

/**
 * A processor for <i>correlated</i> groups of messages. When a message group is <i>complete</i> it is passed to the
 * processor by e.g. the CorrelatingMessageHandler.
 *
 * @author Iwein Fuld
 * @see org.springframework.integration.aggregator.CorrelatingMessageHandler
 */
public interface MessageGroupProcessor {

    /**
     * Processed the given group and sends the resulting message(s) to the output channel using the channelTemplate.
     * Implementations are free to send as little or as many messages based on the invocation as needed. For example the
     * DefaultAggregatingMessageGroupProcessor will send only a single message containing a collection of all messages
     * in the group, where the resequencing equivalent strategy will send all messages in the group individually.
     */
    void processAndSend(MessageGroup group,
                        MessageChannelTemplate channelTemplate, MessageChannel outputChannel
    );
}