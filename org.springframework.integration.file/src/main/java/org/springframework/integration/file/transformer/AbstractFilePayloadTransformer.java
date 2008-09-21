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

import org.springframework.integration.message.MessagingException;
import org.springframework.integration.transformer.AbstractPayloadTransformer;
import org.springframework.util.Assert;

/**
 * Base class for transformers that convert a File payload.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractFilePayloadTransformer<T> extends AbstractPayloadTransformer<File, T> {

	private volatile boolean deleteFile;


	/**
	 * Specify whether to delete the File after transformation.
	 */
	public void setDeleteFile(boolean deleteFile) {
		this.deleteFile = deleteFile;
	}

	@Override
	protected final T transformPayload(File file) throws Exception {
		Assert.notNull(file, "File must not be null");
		if (!file.exists()) {
			throw new MessagingException("File '" + file + "' no longer exists.");
		}
		if (!file.canRead()) {
			throw new MessagingException("Unable to read File '" + file + "'");
		}
		T result = transformFile(file);
		if (this.deleteFile) {
			file.delete();
		}
		return result;
	}

	/**
	 * Subclasses must implement this method to transform the File contents.
	 */
	protected abstract T transformFile(File file) throws Exception;

}
