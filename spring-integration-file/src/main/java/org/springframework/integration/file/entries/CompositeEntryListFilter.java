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

import org.springframework.beans.factory.InitializingBean;

import org.springframework.util.Assert;

import java.util.*;


public class CompositeEntryListFilter<T> implements EntryListFilter<T> {
    private final Set<EntryListFilter<T>> fileFilters;

    public CompositeEntryListFilter(EntryListFilter<T>... fileFilters) {
        this.fileFilters = new LinkedHashSet<EntryListFilter<T>>(Arrays.asList(fileFilters));
    }

    public CompositeEntryListFilter(Collection<?extends EntryListFilter<T>> fileFilters) {
        this.fileFilters = new LinkedHashSet<EntryListFilter<T>>(fileFilters);
    }

    @SuppressWarnings("unchecked")
    public List<T> filterEntries(T[] entries) {
        Assert.notNull(entries, "'files' should not be null");

        List<T> leftOver = Arrays.asList(entries);

        for (EntryListFilter<T> fileFilter : this.fileFilters) {
            T[] ts = (T[]) leftOver.toArray();
            leftOver = fileFilter.filterEntries(ts);
        }

        return leftOver;
    }

    public CompositeEntryListFilter<T> addFilter(EntryListFilter<T> filter) {
        return this.addFilters(Arrays.asList(filter));
    }

    /**
     * @param filters one or more new filters to add
     * @return this CompositeFileFilter instance with the added filters
     * @see #addFilters(Collection)
     */
    @SuppressWarnings("unused")
    public CompositeEntryListFilter<T> addFilters(EntryListFilter<T>[] filters) {
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
    public CompositeEntryListFilter<T> addFilters(Collection<EntryListFilter<T>> filtersToAdd) {
        for (EntryListFilter<T> elf : filtersToAdd)
            if (elf instanceof InitializingBean) {
                try {
                    ((InitializingBean) elf).afterPropertiesSet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        this.fileFilters.addAll(filtersToAdd);

        return this;
    }
}
