package org.springframework.integration.aggregator;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

/**
 * This implementation of MessageGroupProcessor will forward all messages inside the group to the given output channel.
 * This is useful if there is no requirement to process the messages, but they should just be blocked as a group until
 * their CompletionStrategy lets them pass through.
 *
 * @author Iwein Fuld
 * @since 2.0.0
 */
public class PassThroughMessageGroupProcessor implements MessageGroupProcessor {

    public void processAndSend(MessageGroup group, MessageChannelTemplate channelTemplate, MessageChannel outputChannel) {
        for (Message<?> message : group.getMessages()) {
            channelTemplate.send(message, outputChannel);
        }
    }

}
