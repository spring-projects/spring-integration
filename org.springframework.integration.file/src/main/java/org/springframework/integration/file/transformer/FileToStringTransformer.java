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

package org.springframework.integration.file.transformer;

import java.io.File;
import java.io.FileReader;

import org.springframework.util.FileCopyUtils;

/**
 * A payload transformer that copies a File's contents to a String.
 * 
 * @author Mark Fisher
 */
public class FileToStringTransformer extends AbstractFilePayloadTransformer<String> {

	@Override
	protected final String transformFile(File file) throws Exception {
		return FileCopyUtils.copyToString(new FileReader(file));
	}

}
