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

import org.springframework.util.Assert;

/**
 * Metadata for configuring a pool of concurrent threads.
 * 
 * @author Mark Fisher
 */
public class ConcurrencyPolicy {

	public static final int DEFAULT_CORE_SIZE = 1;

	public static final int DEFAULT_MAX_SIZE = 10;

	public static final int DEFAULT_QUEUE_CAPACITY = 0;

	public static final int DEFAULT_KEEP_ALIVE_SECONDS = 60;


	private int coreSize = DEFAULT_CORE_SIZE;

	private int maxSize = DEFAULT_MAX_SIZE;

	private int queueCapacity = DEFAULT_QUEUE_CAPACITY;

	private int keepAliveSeconds = DEFAULT_KEEP_ALIVE_SECONDS;


	public ConcurrencyPolicy() {
	}

	public ConcurrencyPolicy(int coreSize, int maxSize) {
		Assert.isTrue(maxSize >= coreSize, "'coreSize' must not exceed 'maxSize'");
		this.setCoreSize(coreSize);
		this.setMaxSize(maxSize);
	}


	public int getCoreSize() {
		return this.coreSize;
	}

	public void setCoreSize(int coreSize) {
		Assert.isTrue(coreSize > 0, "'coreSize' must be at least 1");
		this.coreSize = coreSize;
	}

	public int getMaxSize() {
		return this.maxSize;
	}

	public void setMaxSize(int maxSize) {
		Assert.isTrue(maxSize > 0, "'maxSize' must be at least 1");
		this.maxSize = maxSize;
	}

	public int getQueueCapacity() {
		return this.queueCapacity;
	}

	public void setQueueCapacity(int queueCapacity) {
		Assert.isTrue(queueCapacity >= 0, "'queueCapacity' must not be negative");
		this.queueCapacity = queueCapacity;
	}

	public int getKeepAliveSeconds() {
		return this.keepAliveSeconds;
	}

	public void setKeepAliveSeconds(int keepAliveSeconds) {
		Assert.isTrue(keepAliveSeconds >= 0, "'keepAliveSeconds' must not be negative");
		this.keepAliveSeconds = keepAliveSeconds;
	}

	public String toString() {
		return "[coreSize=" + this.coreSize + ", maxSize=" + this.maxSize +
				", queueCapacity=" + this.queueCapacity + ", keepAliveSeconds=" + this.keepAliveSeconds + "]";
	}

}
