/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.integration.file.filters;

import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileFilter;
import java.util.*;


/**
 * Composition that delegates to multiple {@link FileFilter}s. The composition is AND based, meaning that a file must
 * pass through each filter's {@link #filterFiles(java.io.File[])} method in order to be accepted by the composite.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class CompositeFileListFilter extends CompositeEntryListFilter<File> implements FileListFilter{

    public CompositeFileListFilter(EntryListFilter<File>... fileFilters) {
		this(Arrays.asList(fileFilters));
    }

    public CompositeFileListFilter(Collection<? extends EntryListFilter<File>> fileFilters) {
		super(fileFilters);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation delegates to a collection of filters and returns only files that pass all the filters.
	 * @deprecated use {@link #filterEntries} instead
     */
	@Deprecated
    public List<File> filterFiles(File[] files) {
        Assert.notNull(files, "'files' should not be null");

		return this.filterEntries(files);
    }
}
