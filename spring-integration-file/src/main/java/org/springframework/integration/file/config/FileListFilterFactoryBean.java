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
package org.springframework.integration.file.config;

import org.springframework.beans.factory.FactoryBean;

import org.springframework.integration.file.entries.*;

import java.io.File;

import java.util.Collection;
import java.util.regex.Pattern;


/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<EntryListFilter<File>> {
    private volatile EntryListFilter<File> fileListFilter;
    private volatile EntryListFilter<File> filterReference;
    private volatile Pattern filenamePattern;
    private volatile Boolean preventDuplicates;
    private final Object monitor = new Object();
    private volatile Collection<EntryListFilter<File>> filterReferences;
    private FileEntryNamer fileNamer = new FileEntryNamer();

    public void setFilterReferences(Collection<EntryListFilter<File>> filterReferences) {
        this.filterReferences = filterReferences;
    }

    public void setFilterReference(EntryListFilter<File> filterReference) {
        this.filterReference = filterReference;
    }

    public void setFilenamePattern(Pattern filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    public void setPreventDuplicates(Boolean preventDuplicates) {
        this.preventDuplicates = preventDuplicates;
    }

    public EntryListFilter<File> getObject() throws Exception {
        if (this.fileListFilter == null) {
            synchronized (this.monitor) {
                this.intializeFileListFilter();
            }
        }

        return this.fileListFilter;
    }

    public Class<?> getObjectType() {
        return (this.fileListFilter != null) ? this.fileListFilter.getClass() : EntryListFilter.class;
    }

    public boolean isSingleton() {
        return true;
    }

    private void intializeFileListFilter() {
        if (this.fileListFilter != null) {
            return;
        }

        EntryListFilter<File> flf=null;

        if ((this.filterReference != null) && (this.filenamePattern != null)) {
            throw new IllegalArgumentException("The 'filter' reference and " + "'filename-pattern' attributes are mutually exclusive.");
        }

        if (this.filterReference != null) {
            if (Boolean.TRUE.equals(this.preventDuplicates)) {
                flf = this.createCompositeWithAcceptOnceFilter(this.filterReference);
            } else { // preventDuplicates is either FALSE or NULL 
                flf = this.filterReference;
            }
        } else if (this.filenamePattern != null) {
            PatternMatchingEntryListFilter<File> patternFilter = new PatternMatchingEntryListFilter<File>(fileNamer, this.filenamePattern);

            if (Boolean.FALSE.equals(this.preventDuplicates)) {
                flf = patternFilter;
            } else { // preventDuplicates is either TRUE or NULL
                flf = this.createCompositeWithAcceptOnceFilter(patternFilter);
            }
        } else if (Boolean.FALSE.equals(this.preventDuplicates)) {
            flf = new AcceptAllEntryListFilter<File>();
        } else     { // preventDuplicates is either TRUE or NULL
            flf = new AcceptOnceEntryFileListFilter<File>();
        }

        // finally, it might be that they simply want a {@link CompositeEntryListFilter}
        if ((this.filterReferences != null) && (this.filterReferences.size() > 0) ) {
            CompositeEntryListFilter<File> flfc = new CompositeEntryListFilter<File>();

            for (EntryListFilter<File> ff : filterReferences)
                flfc.addFilter(ff);

            flf = flfc;
        }
        if( flf== null)flf =new CompositeEntryListFilter<File>();

        this.fileListFilter = flf;
    }

    private CompositeEntryListFilter<File> createCompositeWithAcceptOnceFilter(EntryListFilter<File> otherFilter) {
        CompositeEntryListFilter<File> compositeFilter = new CompositeEntryListFilter<File>();
        compositeFilter.addFilter(new AcceptOnceEntryFileListFilter<File>(), otherFilter);

        return compositeFilter;
    }
}
