/*
 * Copyright 2018-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.aop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogMessage;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractFetchLimitingMessageSource;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.util.Assert;

/**
 * Standard rotation policy; iterates over key/directory pairs; when the end is reached,
 * starts again at the beginning. If the fair option is true the rotation occurs on every
 * poll, regardless of result. Otherwise, rotation occurs when the current pair returns no
 * message.
 * <p>
 * Subclasses implement {@code onRotation(MessageSource<?> source)} to configure the
 * {@link MessageSource} on each rotation.
 *
 * @author Gary Russell
 * @author Michael Forstner
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 5.2
 */
public class StandardRotationPolicy implements RotationPolicy {

	protected final Log logger = LogFactory.getLog(getClass()); // NOSONAR final

	private final DelegatingSessionFactory<?> factory;

	private final List<KeyDirectory> keyDirectories = new ArrayList<>();

	private final boolean fair;

	private volatile Iterator<KeyDirectory> iterator;

	private volatile KeyDirectory current;

	private volatile boolean initialized;

	public StandardRotationPolicy(DelegatingSessionFactory<?> factory, List<KeyDirectory> keyDirectories,
			boolean fair) {

		Assert.notNull(factory, "factory cannot be null");
		Assert.notNull(keyDirectories, "keyDirectories cannot be null");
		Assert.isTrue(!keyDirectories.isEmpty(), "At least one KeyDirectory is required");
		this.factory = factory;
		this.keyDirectories.addAll(keyDirectories);
		this.fair = fair;
		this.iterator = this.keyDirectories.iterator();
	}

	@Override
	public void beforeReceive(MessageSource<?> source) {
		if (this.fair || !this.initialized) {
			configureSource(source);
			if (this.fair && !this.initialized
					&& source instanceof AbstractFetchLimitingMessageSource<?> fetchLimitingMessageSource) {

				this.logger.info(LogMessage.format("Enforce 'maxFetchSize = 1' for '%s' in the 'fair' mode", source));
				fetchLimitingMessageSource.setMaxFetchSize(1);
			}
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

	@Override
	public KeyDirectory getCurrent() {
		return this.current;
	}

	protected DelegatingSessionFactory<?> getFactory() {
		return this.factory;
	}

	protected List<KeyDirectory> getKeyDirectories() {
		return Collections.unmodifiableList(this.keyDirectories);
	}

	protected boolean isFair() {
		return this.fair;
	}

	protected Iterator<KeyDirectory> getIterator() {
		return this.iterator;
	}

	protected boolean isInitialized() {
		return this.initialized;
	}

	protected void configureSource(MessageSource<?> source) {
		if (!this.iterator.hasNext()) {
			this.iterator = this.keyDirectories.iterator();
		}
		this.current = this.iterator.next();
		onRotation(source);
	}

	/**
	 * Update the state of the {@link MessageSource} after the server is rotated, if necessary.
	 * The default implementation updates the remote directory for known MessageSource implementations that require it,
	 * specifically, instances of {@link AbstractRemoteFileStreamingMessageSource}, and
	 * {@link AbstractInboundFileSynchronizingMessageSource}, and does nothing otherwise.
	 * Subclasses may override this method to support other MessageSource types.
	 * @param source the MessageSource.
	 */
	protected void onRotation(MessageSource<?> source) {
		if (source instanceof AbstractRemoteFileStreamingMessageSource<?> streamingMessageSource) {
			streamingMessageSource.setRemoteDirectory(this.current.getDirectory());
		}
		else if (source instanceof AbstractInboundFileSynchronizingMessageSource<?> synchronizingMessageSource) {
			synchronizingMessageSource.getSynchronizer()
					.setRemoteDirectory(this.current.getDirectory());
		}
	}

}
