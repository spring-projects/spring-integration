package org.springframework.integration.aggregator;

import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.store.MessageGroup;

/**
 * A processor for <i>correlated</i> groups of messages.
 * 
 * @author Iwein Fuld
 * @see org.springframework.integration.aggregator.CorrelatingMessageHandler
 */
public interface MessageGroupProcessor {

	/**
	 * Process the given group and send the resulting message(s) to the output channel using the channel template.
	 * Implementations are free to send as little or as many messages based on the invocation as needed. For example an
	 * aggregating processor will send only a single message representing the group, where a resequencing strategy will
	 * send all messages in the group individually.
	 */
	void processAndSend(MessageGroup group, MessageChannelTemplate channelTemplate, MessageChannel outputChannel);
}