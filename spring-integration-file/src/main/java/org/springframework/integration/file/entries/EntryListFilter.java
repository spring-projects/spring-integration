/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.file.entries;

import java.util.List;


/**
 * Strategy interface for filtering entries representing files on a local or remote file system. This is a generic
 * variant of FileListFilter that also works with references to remote files.
 * <p/>
 * Implementations must be thread safe.
 *
 * @author Josh Long
 * @author Iwein Fuld
 *
 * @since 2.0.0
 *
 * @see org.springframework.integration.file.filters.FileListFilter
 */
public interface EntryListFilter<T> {

    /**
     * Filters out entries and returns the entries that are left in a list, or an
     * empty list when a null is passed in.
     */
	List<T> filterEntries(T[] entries);
}
