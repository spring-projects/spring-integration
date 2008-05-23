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
import java.io.FileReader;

import org.springframework.util.FileCopyUtils;

/**
 * A {@link org.springframework.integration.message.MessageMapper}
 * implementation for messages with a String payload.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class TextFileMessageCreator extends AbstractFileMessageCreator<String> {

	@Override
	protected String readMessagePayload(File file) throws Exception {
		return FileCopyUtils.copyToString(new FileReader(file));
	}

}
