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
 * Base class providing common behavior for file-based message creators. The
 * subclasses will redefine the {@code readMessagePayload()} method. This class
 * allows to choose between keeping the file after message creation and removing
 * it, by setting the appropriate value in the constructor. The desired
 * behaviour depends on the nature of the created message (i.e. messages with a
 * {@link String} payload can safely remove the file after creation, but
 * messages with a {@link File} payload cannot do that) or of the collaborator
 * that uses the class instance (e.g. if the file is a locally created copy, it
 * can be always discarded).
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class AbstractFileMessageCreator<T> implements MessageCreator<File, T> {

	protected Log logger = LogFactory.getLog(this.getClass());

	private final boolean deleteFileAfterCreation;
	
	
	/**
	 * @param deleteFileAfterCreation Indicates whether the file should be
	 * deleted after the message has been created.
	 */
	public AbstractFileMessageCreator(boolean deleteFileAfterCreation) {
		this.deleteFileAfterCreation = deleteFileAfterCreation;
	}
	

	public final Message<T> createMessage(File file) {
		try {
			T payload = this.readMessagePayload(file);
			if (payload == null) {
				return null;
			}
			Message<T> message = new GenericMessage<T>(payload);
			if (deleteFileAfterCreation) {
				file.delete();
			}
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
