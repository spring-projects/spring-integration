/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.file.remote.aop;

import org.springframework.integration.core.MessageSource;

/**
 * A strategy for rotating advices to allow reconfiguring
 * the message source before and/or after a poll.
 *
 * @author Gary Russell
 * @author Michael Forstner
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 5.2
 */
public interface RotationPolicy {

	/**
	 * Invoked before the message source receive() method.
	 * @param source the message source.
	 */
	void beforeReceive(MessageSource<?> source);

	/**
	 * Invoked after the message source receive() method.
	 * @param messageReceived true if a message was received.
	 * @param source the message source.
	 */
	void afterReceive(boolean messageReceived, MessageSource<?> source);

	/**
	 * Return the current {@link KeyDirectory}.
	 * @return the current {@link KeyDirectory}
	 * @since 5.2
	 */
	KeyDirectory getCurrent();

	/**
	 * A key for a thread-local store and its related directory pair.
	 * @param key the for entry
	 * @param directory the directory for entry
	 */
	record KeyDirectory(Object key, String directory) {

	}

}
