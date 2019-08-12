/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.remote.aop;

import org.springframework.integration.file.remote.session.DelegatingSessionFactory;
import org.springframework.util.Assert;

/**
 * A {@link DelegatingSessionFactory} key/directory pair.
 */
public class KeyDirectory {

	private final Object key;

	private final String directory;

	public KeyDirectory(Object key, String directory) {
		Assert.notNull(key, "key cannot be null");
		Assert.notNull(directory, "directory cannot be null");
		this.key = key;
		this.directory = directory;
	}

	public Object getKey() {
		return this.key;
	}

	public String getDirectory() {
		return this.directory;
	}

	@Override
	public String toString() {
		return "KeyDirectory [key=" + this.key.toString() + ", directory=" + this.directory + "]";
	}

}
