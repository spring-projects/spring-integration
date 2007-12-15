/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageMapper;
import org.springframework.integration.message.SimplePayloadMessageMapper;
import org.springframework.util.Assert;

/**
 * Convenience base class for channel adapters.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelAdapter implements MessageChannel, InitializingBean {

	protected Log logger = LogFactory.getLog(this.getClass());

	private MessageMapper mapper = new SimplePayloadMessageMapper();

	private volatile boolean initialized;


	public final void afterPropertiesSet() {
		this.initialize();
		this.initialized = true;
	}

	public void setMapper(MessageMapper mapper) {
		Assert.notNull(mapper, "'mapper' must not be null");
		this.mapper = mapper;
	}

	protected MessageMapper getMapper() {
		return this.mapper;
	}

	public boolean send(Message message) {
		if (!this.initialized) {
			throw new MessageHandlingException("adapter not initialized");
		}
		try {
			Object source = this.getMapper().fromMessage(message);
			return this.sendObject(source);
		}
		catch (Exception e) {
			throw new MessageHandlingException("failed to send message", e);
		}
	}

	public boolean send(final Message message, long timeout) {
		if (!this.initialized) {
			throw new MessageHandlingException("adapter not initialized");
		}
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Boolean> result = executor.submit(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				return send(message);
			}
		});
		try {
			result.get(timeout, TimeUnit.MILLISECONDS);
			if (result.isDone()) {
				return result.get();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		catch (TimeoutException e) {
			return false;
		}
		catch (ExecutionException e) {
			throw new MessageHandlingException("Exception occurred in message source", e);
		}
		result.cancel(true);
		return false;
	}

	public Message receive() {
		if (!this.initialized) {
			throw new MessageHandlingException("adapter not initialized");
		}
		try {
			Object result = this.receiveObject();
			if (result != null) {
				return this.getMapper().toMessage(result);
			}
		}
		catch (Exception e) {
			throw new MessageHandlingException("Failed to receive message from source", e);
		}
		return null;
	}

	public Message receive(long timeout) {
		if (!this.initialized) {
			throw new MessageHandlingException("adapter not initialized");
		}
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Message> result = executor.submit(new Callable<Message>() {
			public Message call() throws Exception {
				return receive();
			}
		});
		try {
			result.get(timeout, TimeUnit.MILLISECONDS);
			if (result.isDone()) {
				return result.get();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		catch (TimeoutException e) {
			return null;
		}
		catch (ExecutionException e) {
			throw new MessageHandlingException("Exception occurred in message source", e);
		}
		result.cancel(true);
		return null;
	}

	protected void initialize() {
	}

	protected abstract boolean sendObject(Object object) throws Exception;

	protected abstract Object receiveObject() throws Exception;

}
