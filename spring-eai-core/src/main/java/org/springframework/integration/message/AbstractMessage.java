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

package org.springframework.integration.message;

import java.util.concurrent.locks.ReentrantLock;

import org.springframework.integration.SystemInterruptedException;

/**
 * Base Message class defining common properties such as id, header, and lock.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessage implements Message {

	private Object id;

	private MessageHeader header = new MessageHeader();

	private ReentrantLock lock;


	protected AbstractMessage(Object id) {
		if (id == null) {
			throw new IllegalArgumentException("id must not be null");
		}
		this.id = id;
	}

	public Object getId() {
		return this.id;
	}

	public MessageHeader getHeader() {
		return this.header;
	}

	protected void setHeader(MessageHeader header) {
		this.header = header;
	}

	public void lock() {
		this.lock.lock();
	}

	public void lockInterruptibly() {
		try {
			lock.lockInterruptibly();
		}
		catch (InterruptedException e) {
			throw new SystemInterruptedException("Unable to obtain lock for message", e);
		}
	}

	public void unlock() {
		lock.unlock();
	}

}
