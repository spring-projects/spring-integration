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

import org.springframework.util.Assert;
import java.util.*;


public class CompositeEntryListFilter<T> implements EntryListFilter<T> {
    private final Set<EntryListFilter> fileFilters;

    public CompositeEntryListFilter(EntryListFilter... fileFilters) {
        this.fileFilters = new LinkedHashSet<EntryListFilter>(Arrays.asList(fileFilters));
    }

    public CompositeEntryListFilter(Collection<EntryListFilter> fileFilters) {
        this.fileFilters = new LinkedHashSet<EntryListFilter>(fileFilters);
    }

    @SuppressWarnings("unchecked")
    public List<T> filterEntries(T[] entries) {
        Assert.notNull(entries, "'files' should not be null");
        List<T> leftOver =  Arrays.asList(entries);
        for (EntryListFilter fileFilter : this.fileFilters) {
            T[] ts =(T[]) leftOver.toArray();
            leftOver = fileFilter.filterEntries(ts);
        }
        return leftOver;
    }

    /**
     * @param filters one or more new filters to add
     * @return this CompositeFileFilter instance with the added filters
     * @see #addFilters(Collection)
     */
    public CompositeEntryListFilter addFilter(EntryListFilter... filters) {
        return addFilters(Arrays.asList(filters));
    }

    /**
     * Not thread safe. Only a single thread may add filters at a time.
     * <p/>
     * Add the new filters to this CompositeFileFilter while maintaining the existing filters.
     *
     * @param filtersToAdd a list of filters to add
     * @return this CompositeEntryListFilter instance with the added filters
     */
    public CompositeEntryListFilter addFilters(Collection<EntryListFilter> filtersToAdd) {
        this.fileFilters.addAll(filtersToAdd);

        return this;
    }
}
