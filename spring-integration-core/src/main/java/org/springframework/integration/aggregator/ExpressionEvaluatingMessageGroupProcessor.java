package org.springframework.integration.aggregator;

import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} implementation that evaluates a SpEL expression. The SpEL context root is the list of
 * all Messages in the group. The evaluation result can be any Object and is send as new Message payload to the output
 * channel.
 * 
 * @author Alex Peters
 * @author Dave Syer
 * 
 */
public class ExpressionEvaluatingMessageGroupProcessor extends AbstractExpressionEvaluatingMessageListProcessor
		implements MessageGroupProcessor {

	public ExpressionEvaluatingMessageGroupProcessor(String expression) {
		super(expression);
	}

	/**
	 * Evaluate the expression provided on the unmarked messages (a collection) in the group, and delegate to the
	 * {@link MessagingTemplate} to send dowstream.
	 */
	public void processAndSend(MessageGroup group, MessagingTemplate messagingTemplate, MessageChannel outputChannel) {
		Object newPayload = process(group.getUnmarked());
		messagingTemplate.send(outputChannel, MessageBuilder.withPayload(newPayload).build());
	}

}
