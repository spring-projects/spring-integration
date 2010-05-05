package org.springframework.integration.store;

import java.util.Collection;

import org.springframework.integration.core.Message;

public interface MessageGroup {

	/**
	 * Add a message to the internal list. This is needed to avoid hitting the
	 * underlying store or copying the internal list. Use with care.
	 */
	boolean add(Message<?> message);

	/**
	 * @return internal message list, modification is allowed, but not
	 *         recommended
	 */
	Collection<Message<?>> getUnmarked();

	/**
	 * @return internal message list, modification is allowed, but not
	 *         recommended
	 */
	Collection<Message<?>> getMarked();

	/**
	 * @return the correlation key that links these messages together according
	 *         to a particular CorrelationStrategy
	 */
	Object getCorrelationKey();

	boolean isComplete();

	int getSequenceSize();

	int size();

	void mark();

	Message<?> getOne();
	
	long getTimestamp();

}