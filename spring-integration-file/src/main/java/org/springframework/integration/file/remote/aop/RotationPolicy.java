/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.file.remote.aop;

import org.springframework.integration.core.MessageSource;
import org.springframework.util.Assert;

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
	 */
	class KeyDirectory {

		private final Object key;

		private final String directory;

		public KeyDirectory(Object key, String directory) {
			Assert.notNull(key, "key cannot be null");
			Assert.notNull(directory, "directory cannot be null");
			this.key = key;
			this.directory = directory;
		}

		public Object getKey() {
			return this.key;
		}

		public String getDirectory() {
			return this.directory;
		}

		@Override
		public String toString() {
			return "KeyDirectory [key=" + this.key.toString() + ", directory=" + this.directory + "]";
		}

	}

}
