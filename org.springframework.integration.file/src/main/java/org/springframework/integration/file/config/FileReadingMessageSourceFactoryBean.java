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

package org.springframework.integration.file.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileListFilter;
import org.springframework.integration.file.FileReadingMessageSource;

import java.io.File;
import java.util.Comparator;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @since 1.0.3
 */
public class FileReadingMessageSourceFactoryBean implements FactoryBean, ResourceLoaderAware {

	private volatile FileReadingMessageSource source;

	private volatile ResourceLoader resourceLoader;

	private volatile File directory;

	private volatile FileListFilter filter;

	private volatile Comparator<File> comparator;

    private volatile DirectoryScanner scanner;

	private volatile Boolean scanEachPoll;

	private volatile Boolean autoCreateDirectory;

	private final Object initializationMonitor = new Object();


	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public void setComparator(Comparator<File> comparator) {
		this.comparator = comparator;
	}

    public void setScanner(DirectoryScanner scanner) {
        this.scanner = scanner;
    }

    public void setFilter(FileListFilter filter) {
		this.filter = filter;
	}

	public void setScanEachPoll(Boolean scanEachPoll) {
		this.scanEachPoll = scanEachPoll;
	}

	public void setAutoCreateDirectory(Boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	public Object getObject() throws Exception {
		if (this.source == null) {
			initSource();
		}
		return this.source;
	}

	public Class<?> getObjectType() {
		return FileReadingMessageSource.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initSource() {
		synchronized (this.initializationMonitor) {
			if (this.source != null) {
				return;
			}
			this.source = (this.comparator != null) ?
					new FileReadingMessageSource(this.comparator) : new FileReadingMessageSource();
			this.source.setDirectory(this.directory);
			if (this.filter != null) {
				this.source.setFilter(this.filter);
			}
			if (this.scanEachPoll != null) {
				this.source.setScanEachPoll(this.scanEachPoll);
			}
			if (this.autoCreateDirectory != null) {
				this.source.setAutoCreateDirectory(this.autoCreateDirectory);
			}
            if (this.scanner != null) {
                this.source.setScanner(this.scanner);
            }
			this.source.afterPropertiesSet();
		}
	}

}
