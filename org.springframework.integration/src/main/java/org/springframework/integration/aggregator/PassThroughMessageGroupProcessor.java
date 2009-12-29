package org.springframework.integration.aggregator;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;

public class PassThroughMessageGroupProcessor implements MessageGroupProcessor {

    public void processAndSend(MessageGroup group, MessageChannel outputChannel) {
        for (Message<?> message : group.getMessages()) {
            outputChannel.send(message);
            group.onProcessingOf(message);
        }
        group.onCompletion();
    }
}
