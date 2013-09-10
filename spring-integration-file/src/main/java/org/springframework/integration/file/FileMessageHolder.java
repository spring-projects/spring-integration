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
package org.springframework.integration.file;

import java.io.File;

import org.springframework.messaging.Message;

/**
 * A simple wrapper for a {@code Message<File>;} used for
 * file disposition after the send completes, or
 * after the transaction commits with a transactional
 * poller.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class FileMessageHolder {

	private Message<File> message;

	Message<File> getMessage() {
		return message;
	}

	void setMessage(Message<File> message) {
		this.message = message;
	}

}
