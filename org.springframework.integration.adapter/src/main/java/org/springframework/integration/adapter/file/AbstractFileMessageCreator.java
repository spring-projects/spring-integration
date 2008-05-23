/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.file;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessagingException;

/**
 * Base class providing common behavior for file-based message creators.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class AbstractFileMessageCreator<T> implements MessageCreator<File, T> {

	protected Log logger = LogFactory.getLog(this.getClass());

	public Message<T> createMessage(File file) {
		try {
			T payload = this.readMessagePayload(file);
			if (payload == null) {
				return null;
			}
			Message<T> message = new GenericMessage<T>(payload);
			message.getHeader().setProperty(FileNameGenerator.FILENAME_PROPERTY_KEY, file.getName());
			return message;
		}
		catch (Exception e) {
			String description = "failure occurred mapping file to message";
			if (logger.isWarnEnabled()) {
				logger.warn(description, e);
			}
			throw new MessagingException(description, e);
		}
	}

	protected abstract T readMessagePayload(File file) throws Exception;

}
