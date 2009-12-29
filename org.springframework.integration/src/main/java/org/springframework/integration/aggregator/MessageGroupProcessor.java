package org.springframework.integration.aggregator;

import org.springframework.integration.core.MessageChannel;

/**
 * @author Iwein Fuld
 */
public interface MessageGroupProcessor {

    void processAndSend(MessageGroup group,
                        MessageChannel outputChannel
    );
}