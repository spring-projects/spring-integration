/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.session;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.aop.AbstractMessageSourceAdvice;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A smart poller advice that rotates across multiple remote servers/directories.
 *
 * @author Gary Russell
 * @since 5.0.7
 *
 */
public class RotatingServerAdvice extends AbstractMessageSourceAdvice {

	private final RotationPolicy rotationPolicy;

	/**
	 * Create an instance that rotates to the next server/directory if no message is
	 * received.
	 * @param factory the {@link DelegatingSessionFactory}.
	 * @param keyDirectories a list of {@link KeyDirectory}.
	 */
	public RotatingServerAdvice(DelegatingSessionFactory<?> factory, List<KeyDirectory> keyDirectories) {
		this(factory, keyDirectories, false);
	}

	/**
	 * Create an instance that rotates to the next server/directory depending on the fair
	 * argument.
	 * @param factory the {@link DelegatingSessionFactory}.
	 * @param keyDirectories a list of {@link KeyDirectory}.
	 * @param fair true to rotate on every poll, false to rotate when no message is received.
	 */
	public RotatingServerAdvice(DelegatingSessionFactory<?> factory, List<KeyDirectory> keyDirectories, boolean fair) {
		this(new StandardRotationPolicy(factory, keyDirectories, fair));
	}

	/**
	 * Construct an instance that rotates according to the supplied
	 * {@link RotationPolicy}.
	 * @param rotationPolicy the policy.
	 */
	public RotatingServerAdvice(RotationPolicy rotationPolicy) {
		Assert.notNull(rotationPolicy, "'rotationPolicy' cannot be null");
		this.rotationPolicy = rotationPolicy;
	}

	@Override
	public boolean beforeReceive(MessageSource<?> source) {
		this.rotationPolicy.beforeReceive(source);
		return true;
	}

	@Override
	public Message<?> afterReceive(Message<?> result, MessageSource<?> source) {
		this.rotationPolicy.afterReceive(result != null, source);
		return result;
	}

	/**
	 * Implementations can reconfigure the message source before and/or after
	 * a poll.
	 * @since 5.0.7
	 *
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

	}

	/**
	 * Standard rotation policy; iterates over key/directory pairs; when the end
	 * is reached, starts again at the beginning. If the fair option is true
	 * the rotation occurs on every poll, regardless of result. Otherwise rotation
	 * occurs when the current pair returns no message.
	 * @since 5.0.7
	 *
	 */
	private static class StandardRotationPolicy implements RotationPolicy {

		private final Log logger = LogFactory.getLog(getClass());

		private final DelegatingSessionFactory<?> factory;

		private final List<KeyDirectory> keyDirectories = new ArrayList<>();

		private final boolean fair;

		private volatile Iterator<KeyDirectory> iterator;

		private volatile KeyDirectory current;

		private volatile boolean initialized;

		StandardRotationPolicy(DelegatingSessionFactory<?> factory, List<KeyDirectory> keyDirectories,
				boolean fair) {
			Assert.notNull(factory, "factory cannot be null");
			Assert.notNull(keyDirectories, "keyDirectories cannot be null");
			Assert.isTrue(keyDirectories.size() > 0, "At least one KeyDirectory is required");
			this.factory = factory;
			this.keyDirectories.addAll(keyDirectories);
			this.fair = fair;
			this.iterator = this.keyDirectories.iterator();
		}

		@Override
		public void beforeReceive(MessageSource<?> source) {
			if (this.fair || !this.initialized) {
				configureSource(source);
				this.initialized = true;
			}
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Next poll is for " + this.current);
			}
			this.factory.setThreadKey(this.current.getKey());
		}

		@Override
		public void afterReceive(boolean messageReceived, MessageSource<?> source) {
			if (this.logger.isTraceEnabled()) {
				this.logger.trace("Poll produced "
						+ (messageReceived ? "a" : "no")
						+ " message");
			}
			this.factory.clearThreadKey();
			if (!this.fair && !messageReceived) {
				configureSource(source);
			}
		}

		private void configureSource(MessageSource<?> source) {
			Assert.isTrue(source instanceof AbstractInboundFileSynchronizingMessageSource
					|| source instanceof AbstractRemoteFileStreamingMessageSource,
				"source must be an AbstractInboundFileSynchronizingMessageSource or a "
				+ "AbstractRemoteFileStreamingMessageSource");
			if (!this.iterator.hasNext()) {
				this.iterator = this.keyDirectories.iterator();
			}
			this.current = this.iterator.next();
			if (source instanceof AbstractRemoteFileStreamingMessageSource) {
				((AbstractRemoteFileStreamingMessageSource<?>) source).setRemoteDirectory(this.current.getValue());
			}
			else {
				((AbstractInboundFileSynchronizingMessageSource<?>) source).getSynchronizer()
					.setRemoteDirectory(this.current.getValue());
			}
		}

	}

	/**
	 * A {@link DelegatingSessionFactory} key/directory pair.
	 * @since 5.0.7
	 *
	 */
	@SuppressWarnings("serial")
	public static class KeyDirectory extends SimpleEntry<String, String> {

		public KeyDirectory(String key, String directory) {
			super(key, directory);
			Assert.notNull(key, "key cannot be null");
			Assert.notNull(directory, "directory cannot be null");
		}

		@Override
		public String toString() {
			return "KeyDirectory [key=" + getKey() + ", directory=" + getValue() + "]";
		}

	}

}
