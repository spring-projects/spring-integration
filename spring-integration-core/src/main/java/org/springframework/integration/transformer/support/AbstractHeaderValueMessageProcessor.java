/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.transformer.support;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 3.0
 */
public abstract class AbstractHeaderValueMessageProcessor<T> implements HeaderValueMessageProcessor<T> {

	// null indicates no explicit setting
	private volatile Boolean overwrite = null;

	public void setOverwrite(Boolean overwrite) {
		this.overwrite = overwrite;
	}

	public Boolean isOverwrite() {
		return this.overwrite;
	}

}
