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
 * Possible status of file that is being processed.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public enum FileProcessingStatus {

	/**
	 * File is accepted by FileListFilter and is ready to be processed in the transaction.
	 */
	PROCESSING,

	/**
	 * In case that transaction committed successfully, status is transitioned to processed.
	 */
	PROCESSED,

	/**
	 * In case that we reached maximum number of retries for file processing, file is flagged
	 * so that we don't take it again for processing.
	 */
	REJECTED
}
