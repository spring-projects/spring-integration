/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.file.tail;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Base class for file tailing inbound adapters.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class FileTailingMessageProducerSupport extends MessageProducerSupport
		implements ApplicationEventPublisherAware {

	private volatile File file;

	private volatile ApplicationEventPublisher eventPublisher;

	private volatile TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private volatile CountDownLatch started = new CountDownLatch(1);


	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	public void setFile(File file) {
		Assert.notNull("'file' cannot be null");
		this.file = file;
	}

	protected File getFile() {
		if (this.file == null) {
			throw new IllegalStateException("No 'file' has been provided");
		}
		return this.file;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		Assert.notNull("'taskExecutor' cannot be null");
		this.taskExecutor = taskExecutor;
	}

	protected TaskExecutor getTaskExecutor() {
		return this.taskExecutor;
	}

	protected void hasStarted() {
		this.started.countDown();
	}

	@Override
	public String getComponentType() {
		return "file:tail-inbound-channel-adapter";
	}

	@Override
	protected void doStop() {
		super.doStop();
		// prepare a new latch for the next start
		this.started = new CountDownLatch(1);
	}

	protected void send(String line) {
		Message<?> message = MessageBuilder.withPayload(line)
				.setHeader(FileHeaders.FILENAME, this.file.getAbsolutePath())
				.build();
		super.sendMessage(message);
	}

	protected void publish(String message) {
		if (this.eventPublisher != null) {
			FileTailingEvent event = new FileTailingEvent(this, message, this.file);
			this.eventPublisher.publishEvent(event);
		}
		else {
			logger.info("No publisher for event:" + message);
		}
	}

	public static class FileTailingEvent extends ApplicationEvent {

		private static final long serialVersionUID = -3382255736225946206L;

		private final String message;

		private final File file;

		public FileTailingEvent(Object source, String message, File file) {
			super(source);
			this.message = message;
			this.file = file;
		}

		protected String getMessage() {
			return message;
		}

		public File getFile() {
			return file;
		}

		@Override
		public String toString() {
			return "FileTailingEvent " + super.toString() +
					" [message=" + this.message +
					", file=" + this.file.getAbsolutePath() + "]";
		}

	}
}
