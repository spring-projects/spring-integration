/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file.filters;

/**
 * Simple implementation of {@link FileListFilter} that always returns true.
 * Suitable as a default.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class AcceptAllFileListFilter<F> extends AbstractFileListFilter<F> {

	@Override
	public boolean accept(F file) {
		return true;
	}

}
