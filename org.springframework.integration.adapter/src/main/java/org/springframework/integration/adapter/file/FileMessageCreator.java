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

import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;

/**
 * A {@link MessageCreator} that creates {@link Message} instances with the
 * absolute path to the {@link File} as payload.
 * 
 * @author Marius Bogoevici
 */
public class FileMessageCreator extends AbstractFileMessageCreator<File> {

	public FileMessageCreator() {
		// The file should never be removed, as just the reference to it is
		// passed to the message
		super(false);
	}
	
	@Override
	protected File readMessagePayload(File file) throws Exception {
		return file;
	}

}
