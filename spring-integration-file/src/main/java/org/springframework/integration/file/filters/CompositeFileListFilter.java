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

import java.util.Set;


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
 */          @Deprecated
public class CompositeFileListFilter implements FileListFilter {

    private final Set<FileListFilter> fileFilters;


    public CompositeFileListFilter(FileListFilter... fileFilters) {
        this.fileFilters = new LinkedHashSet<FileListFilter>(Arrays.asList(fileFilters));
    }

    public CompositeFileListFilter(Collection<FileListFilter> fileFilters) {
        this.fileFilters = new LinkedHashSet<FileListFilter>(fileFilters);
    }


    /**
     * {@inheritDoc}
     * <p/>
     * This implementation delegates to a collection of filters and returns only files that pass all the filters.
     */
    public List<File> filterFiles(File[] files) {
        Assert.notNull(files, "'files' should not be null");
        List<File> leftOver = Arrays.asList(files);
        for (FileListFilter fileFilter : this.fileFilters) {
            leftOver = fileFilter.filterFiles(leftOver.toArray(new File[]{}));
        }
        return leftOver;
    }

    /**
     * @param filters one or more new filters to add
     * @return this CompositeFileFilter instance with the added filters
     * @see #addFilters(Collection)
     */
    public CompositeFileListFilter addFilter(FileListFilter... filters) {
        return addFilters(Arrays.asList(filters));
    }

    /**
     * Not thread safe. Only a single thread may add filters at a time.
     *
     * Add the new filters to this CompositeFileFilter while maintaining the existing filters.
     *
     * @param filtersToAdd a list of filters to add
     * @return this CompositeFileFilter instance with the added filters
     */
    public CompositeFileListFilter addFilters(Collection<FileListFilter> filtersToAdd) {
        this.fileFilters.addAll(filtersToAdd);
		return this;
	}

}
