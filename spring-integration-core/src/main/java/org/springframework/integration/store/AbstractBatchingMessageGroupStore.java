/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.store;


/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public abstract class AbstractBatchingMessageGroupStore implements BasicMessageGroupStore {

	private static final int DEFAULT_REMOVE_BATCH_SIZE = 100;

	private volatile int removeBatchSize = DEFAULT_REMOVE_BATCH_SIZE;

	/**
	 * Set the batch size when bulk removing messages from groups for message stores
	 * that support batch removal.
	 * Default 100.
	 * @param removeBatchSize the batch size.
	 * @since 4.2
	 */
	public void setRemoveBatchSize(int removeBatchSize) {
		this.removeBatchSize = removeBatchSize;
	}

	public int getRemoveBatchSize() {
		return removeBatchSize;
	}

}
