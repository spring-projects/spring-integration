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

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.file.AbstractFileListFilter;
import org.springframework.integration.file.AcceptOnceFileListFilter;
import org.springframework.integration.file.CompositeFileListFilter;
import org.springframework.integration.file.FileListFilter;
import org.springframework.integration.file.PatternMatchingFileListFilter;

/**
 * @author Mark Fisher
 * @since 1.0.3
 */
public class FileListFilterFactoryBean implements FactoryBean<FileListFilter> {

	private volatile FileListFilter fileListFilter;

	private volatile FileListFilter filterReference;

	private volatile Pattern filenamePattern;

	private volatile Boolean preventDuplicates;

	private final Object monitor = new Object();


	public void setFilterReference(FileListFilter filterReference) {
		this.filterReference = filterReference;
	}

	public void setFilenamePattern(Pattern filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public void setPreventDuplicates(Boolean preventDuplicates) {
		this.preventDuplicates = preventDuplicates;
	}

	public FileListFilter getObject() throws Exception {
		if (this.fileListFilter == null) {
			synchronized (this.monitor) {
				this.intializeFileListFilter();
			}
		}
		return this.fileListFilter;
	}

	public Class<?> getObjectType() {
		return (this.fileListFilter != null)
			? this.fileListFilter.getClass() : FileListFilter.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void intializeFileListFilter() {
		if (this.fileListFilter != null) {
			return;
		}
		FileListFilter flf = null;
		if (this.filterReference != null && this.filenamePattern != null) {
			throw new IllegalArgumentException("The 'filter' reference and " +
					"'filename-pattern' attributes are mutually exclusive.");
		}
		if (this.filterReference != null) {
			if (Boolean.TRUE.equals(this.preventDuplicates)) {
				flf = this.createCompositeWithAcceptOnceFilter(this.filterReference);
			}
			else { // preventDuplicates is either FALSE or NULL 
				flf = this.filterReference; 
			}
		}
		else if (this.filenamePattern != null) {
			PatternMatchingFileListFilter patternFilter = new PatternMatchingFileListFilter(this.filenamePattern);
			if (Boolean.FALSE.equals(this.preventDuplicates)) {
				flf = patternFilter;
			}
			else { // preventDuplicates is either TRUE or NULL
				flf = this.createCompositeWithAcceptOnceFilter(patternFilter);
			}
		}
		else if (Boolean.FALSE.equals(this.preventDuplicates)) {
			flf = new AbstractFileListFilter() {
				@Override
				protected boolean accept(File file) {
					return true;
				}
			};
		}
		else { // preventDuplicates is either TRUE or NULL
			flf = new AcceptOnceFileListFilter();
		}
		this.fileListFilter = flf;
	}

	private FileListFilter createCompositeWithAcceptOnceFilter(FileListFilter otherFilter) {
		CompositeFileListFilter compositeFilter = new CompositeFileListFilter();
		compositeFilter.addFilter(new AcceptOnceFileListFilter(), otherFilter);
		return compositeFilter;
	}

}
