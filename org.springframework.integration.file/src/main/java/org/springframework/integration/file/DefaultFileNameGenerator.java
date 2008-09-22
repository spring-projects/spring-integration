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

package org.springframework.integration.file;

import org.springframework.integration.message.Message;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the filename generator strategy. It first checks
 * for the "filename" Message header, then falls back to the Message ID.
 * 
 * @author Mark Fisher
 */
public class DefaultFileNameGenerator implements FileNameGenerator {

	public String generateFileName(Message<?> message) {
		String filenameProperty = message.getHeaders().get(FILENAME_PROPERTY_KEY, String.class);
		return StringUtils.hasText(filenameProperty) ?
				filenameProperty : message.getHeaders().getId() + ".msg";
	}

}
