/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.core;

/**
 * Encapsulates a request for fetching messages from the server.
 *
 * @author Marius Bogoevici
 */
public class FetchRequest {

	private Partition partition;

	private long offset;

	private int maxSizeInBytes;

	public FetchRequest(Partition Partition, long offset, int maxSizeInBytes) {
		this.partition = Partition;
		this.offset = offset;
		this.maxSizeInBytes = maxSizeInBytes;
	}

	public Partition getPartition() {
		return partition;
	}

	public void setPartition(Partition partition) {
		this.partition = partition;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getMaxSizeInBytes() {
		return maxSizeInBytes;
	}

	public void setMaxSizeInBytes(int maxSizeInBytes) {
		this.maxSizeInBytes = maxSizeInBytes;
	}

}
