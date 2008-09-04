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

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.util.Assert;

/**
 * FileFilter that passes all files with a lastModified after a specified
 * timestamp. The filter uses a mutable AtomicLong so the timestamp can be
 * changed during the lifetime of the filter.
 * 
 * @author Iwein Fuld
 */
public class ModificationTimeFileFilter implements FileFilter {

	private final AtomicLong minimumLastModified;


	public ModificationTimeFileFilter(AtomicLong lastRecieveTimestamp) {
		this.minimumLastModified = lastRecieveTimestamp;
	}

	public boolean accept(File file) {
		Assert.notNull(file);
		return file.exists() && file.lastModified() > minimumLastModified.get();
	}

}
