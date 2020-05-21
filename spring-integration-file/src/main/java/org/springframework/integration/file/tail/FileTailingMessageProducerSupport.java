/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.file.tail;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.event.FileIntegrationEvent;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Base class for file tailing inbound adapters.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ali Shahbour
 * @author Vladimir Plizga
 * @since 3.0
 *
 */
public abstract class FileTailingMessageProducerSupport extends MessageProducerSupport
		implements ApplicationEventPublisherAware {

	private volatile File file;

	private volatile ApplicationEventPublisher eventPublisher;

	private volatile TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private volatile long tailAttemptsDelay = 5000;

	private final AtomicLong lastNoMessageAlert = new AtomicLong();

	private long idleEventInterval = 0;

	private volatile long lastProduce = System.currentTimeMillis();

	private ScheduledFuture<?> idleEventScheduledFuture;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	/**
	 * The name of the file you wish to tail.
	 * @param file The absolute path of the file.
	 */
	public void setFile(File file) {
		Assert.notNull(file, "'file' cannot be null");
		this.file = file;
	}

	protected File getFile() {
		if (this.file == null) {
			throw new IllegalStateException("No 'file' has been provided");
		}
		return this.file;
	}

	/**
	 * A task executor; default is a {@link SimpleAsyncTaskExecutor}.
	 * @param taskExecutor The task executor.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "'taskExecutor' cannot be null");
		this.taskExecutor = taskExecutor;
	}

	/**
	 * The delay in milliseconds between attempts to tail a non-existent file,
	 * or between attempts to execute a process if it fails for any reason.
	 * @param tailAttemptsDelay the delay.
	 */
	public void setTailAttemptsDelay(long tailAttemptsDelay) {
		Assert.isTrue(tailAttemptsDelay > 0, "'tailAttemptsDelay' must be > 0");
		this.tailAttemptsDelay = tailAttemptsDelay;
	}

	/**
	 * How often to emit {@link FileTailingIdleEvent}s in milliseconds.
	 * @param idleEventInterval the interval.
	 * @since 5.0
	 */
	public void setIdleEventInterval(long idleEventInterval) {
		Assert.isTrue(idleEventInterval > 0, "'idleEventInterval' must be > 0");
		this.idleEventInterval = idleEventInterval;
	}

	protected long getMissingFileDelay() {
		return this.tailAttemptsDelay;
	}

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	@Override
	public String getComponentType() {
		return "file:tail-inbound-channel-adapter";
	}

	protected void send(String line) {
		Message<?> message = this.getMessageBuilderFactory().withPayload(line)
				.setHeader(FileHeaders.FILENAME, this.file.getName())
				.setHeader(FileHeaders.ORIGINAL_FILE, this.file)
				.build();
		super.sendMessage(message);
		updateLastProduce();
	}

	protected void publish(String message) {
		if (this.eventPublisher != null) {
			FileTailingEvent event = new FileTailingEvent(this, message, this.file);
			this.eventPublisher.publishEvent(event);
		}
		else {
			logger.info("No publisher for event: " + message);
		}
	}



	@Override
	protected void doStart() {
		super.doStart();
		if (this.idleEventInterval > 0) {
			this.idleEventScheduledFuture = getTaskScheduler().scheduleWithFixedDelay(() -> {
				long now = System.currentTimeMillis();
				long lastAlertAt = this.lastNoMessageAlert.get();
				long lastSend = this.lastProduce;
				if (now > lastSend + this.idleEventInterval
						&& now > lastAlertAt + this.idleEventInterval
						&& this.lastNoMessageAlert.compareAndSet(lastAlertAt, now)) {
					publishIdleEvent(now - lastSend);
				}
			}, this.idleEventInterval);
		}
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.idleEventScheduledFuture != null) {
			this.idleEventScheduledFuture.cancel(true);
		}
	}

	private void publishIdleEvent(long idleTime) {
		if (this.eventPublisher != null) {
			if (getFile().exists()) {
				FileTailingIdleEvent event = new FileTailingIdleEvent(this, this.file, idleTime);
				this.eventPublisher.publishEvent(event);
			}
		}
		else {
			logger.info("No publisher for idle event");
		}
	}

	private void updateLastProduce() {
		if (this.idleEventInterval > 0) {
			this.lastProduce = System.currentTimeMillis();
		}
	}

	public static class FileTailingIdleEvent extends FileTailingEvent {

		private static final long serialVersionUID = -967118535347976767L;

		private final long idleTime;

		public FileTailingIdleEvent(Object source, File file, long idleTime) {
			super(source, "Idle timeout", file);
			this.idleTime = idleTime;
		}

		@Override
		public String toString() {
			return super.toString() +
					" [idle time=" + this.idleTime + "]";
		}

	}

	public static class FileTailingEvent extends FileIntegrationEvent {

		private static final long serialVersionUID = -3382255736225946206L;

		private final String message;

		private final File file;

		public FileTailingEvent(Object source, String message, File file) {
			super(source);
			this.message = message;
			this.file = file;
		}

		/**
		 * Return the text message emitted from the underlying tailing producer
		 * ({@linkplain ApacheCommonsFileTailingMessageProducer Apache Commons} or one of
		 * {@linkplain OSDelegatingFileTailingMessageProducer OS natives}).
		 * <p>Note that for the same event type (e.g. 'file not found') the text may be different
		 * depending on the producer and its platform.
		 * @return the original text of the tailing event
		 */
		public String getMessage() {
			return this.message;
		}

		public File getFile() {
			return this.file;
		}

		@Override
		public String toString() {
			return "FileTailingEvent " + super.toString() +
					" [message=" + this.message +
					", file=" + this.file.getAbsolutePath() + "]";
		}

	}

}
