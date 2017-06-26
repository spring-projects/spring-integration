/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.file.support;

/**
 * When writing processing file, this record stores all metadata info
 * about the file that is currently being processed.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public class FileProcessingRecord {
	private FileProcessingStatus status;
	private int numberOfRetries;
	private long modified;
	private long lastRetry;

	private FileProcessingRecord() {
		super();
	}

	public FileProcessingRecord(FileProcessingStatus status, long modified, long lastRetry) {
		this.status = status;
		this.modified = modified;
		this.lastRetry = lastRetry;
	}

	public FileProcessingStatus getStatus() {
		return this.status;
	}

	public int getNumberOfRetries() {
		return this.numberOfRetries;
	}

	public long getModified() {
		return this.modified;
	}

	public long getLastRetry() {
		return this.lastRetry;
	}

	public void markAsRejected() {
		this.status = FileProcessingStatus.REJECTED;
	}

	public void markAsProcessed() {
		this.status = FileProcessingStatus.PROCESSED;
	}

	public void retriedAt(long timestamp) {
		this.lastRetry = timestamp;
		this.numberOfRetries++;
	}
}
