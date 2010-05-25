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

package org.springframework.integration.file;

import java.io.File;

import org.springframework.integration.core.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the filename generator strategy. It first checks
 * for a message header whose name matches its 'headerName' property. The
 * default header name is defined by the constant {@link FileHeaders#FILENAME}.
 * A custom header name can be provided via {@link #setHeaderName(String)}. If
 * no String-typed value is associated with that header it checks if the
 * Message payload is a File instance, and if so, it uses the same name.
 * Finally, it falls back to the Message ID and adds the suffix '.msg'.
 * 
 * @author Mark Fisher
 */
public class DefaultFileNameGenerator implements FileNameGenerator {

	private volatile String headerName = FileHeaders.FILENAME;


	/**
	 * Specify a custom header name to check for the file name.
	 * The default is defined by {@link FileHeaders#FILENAME}.
	 */
	public void setHeaderName(String headerName) {
		Assert.notNull(headerName, "'headerName' must not be null");
		this.headerName = headerName;
	}

	public String generateFileName(Message<?> message) {
		Object filenameProperty = message.getHeaders().get(this.headerName);
		if (filenameProperty instanceof String
				&& StringUtils.hasText((String) filenameProperty)) {
			return (String) filenameProperty;
		}
		if (message.getPayload() instanceof File) {
			return ((File) message.getPayload()).getName();
		}
		return message.getHeaders().getId() + ".msg";
	}

}
