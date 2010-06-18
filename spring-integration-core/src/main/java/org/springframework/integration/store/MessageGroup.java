package org.springframework.integration.store;

import org.springframework.integration.core.Message;

import java.util.Collection;

/**
 * A group of messages that are correlated with each other and should be processed in the same context. The group is
 * divided into marked and unmarked messages. The marked messages are typically already processed, the unmarked messages
 * are to be processed in the future.
 * <p/>
 * The message group allows implementations to be mutable, but this behavior is optional. Implementations should take
 * care to document their thread safety and mutability.
 */
public interface MessageGroup {

	/**
	 * Query if the message can be added.
	 */
	boolean canAdd(Message<?> message);

	/**
	 * @return unmarked messages in the group at time of the invocation
	 */
	Collection<Message<?>> getUnmarked();

	/**
	 * @return marked messages in the group at the time of the invocation
	 */
	Collection<Message<?>> getMarked();

	/**
	 * @return the correlation key that links these messages together, typically according
	 *         to a particular CorrelationStrategy
	 */
	Object getCorrelationKey();

	/**
	 * @return true if the group is complete (i.e. no more messages are expected to be added)
	 */
	boolean isComplete();

	/**
	 * @return the size of the sequence expected 0 if unknown
	 */
	int getSequenceSize();

	/**
	 * @return the total number of messages (marked and unmarked) in this group
	 */
	int size();

	/**
	 * @return a single message from the group
	 */
	Message<?> getOne();

	/**
	 * @return the timestamp (milliseconds since epoch) associated with the creation of this group
	 */
	long getTimestamp();

}