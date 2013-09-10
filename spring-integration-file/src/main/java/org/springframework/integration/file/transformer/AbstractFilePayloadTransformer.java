/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.file.transformer;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.Transformer;
import org.springframework.util.Assert;

/**
 * Base class for transformers that convert a File payload.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractFilePayloadTransformer<T> implements Transformer {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile boolean deleteFiles;


	/**
	 * Specify whether to delete the File after transformation.
	 * Default is <em>false</em>.
	 */
	public void setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
	}

	public final Message<?> transform(Message<?> message) {
		try {
			Assert.notNull(message, "Message must not be null");
			Object payload = message.getPayload();
			Assert.notNull(payload, "Mesasge payload must not be null");
			Assert.isInstanceOf(File.class, payload, "Message payload must be of type [java.io.File]");
			File file = (File) payload;
	        T result = this.transformFile(file);
	        Message<?> transformedMessage = MessageBuilder.withPayload(result)
	        		.copyHeaders(message.getHeaders())
	        		.setHeaderIfAbsent(FileHeaders.ORIGINAL_FILE, file)
	        		.setHeaderIfAbsent(FileHeaders.FILENAME, file.getName())
	        		.build();
			if (this.deleteFiles) {
				if (!file.delete() && this.logger.isWarnEnabled()) {
					this.logger.warn("failed to delete File '" + file + "'");
				}
			}
			return transformedMessage;
        } catch (Exception e) {
        	throw new MessagingException(message, "failed to transform File Message", e);
        }
	}

	/**
	 * Subclasses must implement this method to transform the File contents.
	 */
	protected abstract T transformFile(File file) throws Exception;

}
