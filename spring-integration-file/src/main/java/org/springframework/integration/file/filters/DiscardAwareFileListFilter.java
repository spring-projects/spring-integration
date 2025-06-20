/*
 * Copyright 2018-present the original author or authors.
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

import java.util.function.Consumer;

import org.springframework.lang.Nullable;

/**
 * The {@link FileListFilter} modification which can accept a {@link Consumer}
 * which can be called when the filter discards the file.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0.5
 */
public interface DiscardAwareFileListFilter<F> extends FileListFilter<F> {

	void addDiscardCallback(@Nullable Consumer<F> discardCallback);

}
