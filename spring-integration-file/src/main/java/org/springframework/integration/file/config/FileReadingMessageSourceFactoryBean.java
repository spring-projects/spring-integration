/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.file.config;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.locking.AbstractFileLockerFilter;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
public class FileReadingMessageSourceFactoryBean extends AbstractFactoryBean<FileReadingMessageSource>
		implements BeanNameAware {

	private FileReadingMessageSource source;

	private File directory;

	private FileListFilter<File> filter;

	private AbstractFileLockerFilter locker;

	private Comparator<File> comparator;

	private DirectoryScanner scanner;

	private boolean useWatchService;

	private FileReadingMessageSource.WatchEventType[] watchEvents;

	private Boolean scanEachPoll;

	private Boolean autoCreateDirectory;

	private Integer queueSize;

	private String name;

	@Override
	public void setBeanName(String name) {
		this.name = name;
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

	public void setUseWatchService(boolean useWatchService) {
		this.useWatchService = useWatchService;
	}

	public void setWatchEvents(FileReadingMessageSource.WatchEventType... watchEvents) {
		Assert.notEmpty(watchEvents, "at least one watch event type is required");
		this.watchEvents = Arrays.copyOf(watchEvents, watchEvents.length);
	}

	public void setFilter(FileListFilter<File> filter) {
		if (filter instanceof AbstractFileLockerFilter && (this.locker == null)) {
			this.setLocker((AbstractFileLockerFilter) filter);
		}
		this.filter = filter;
	}

	public void setScanEachPoll(Boolean scanEachPoll) {
		this.scanEachPoll = scanEachPoll;
	}

	public void setAutoCreateDirectory(Boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	public void setQueueSize(Integer queueSize) {
		this.queueSize = queueSize;
	}

	public void setLocker(AbstractFileLockerFilter locker) {
		this.locker = locker;
	}

	@Override
	public Class<?> getObjectType() {
		return FileReadingMessageSource.class;
	}

	@Override
	protected FileReadingMessageSource createInstance() {
		if (this.source == null) {
			initSource();
		}
		return this.source;
	}

	private void initSource() {
		boolean comparatorSet = this.comparator != null;
		boolean queueSizeSet = this.queueSize != null;
		if (comparatorSet) {
			if (queueSizeSet) {
				logger.warn("'comparator' and 'queueSize' are mutually exclusive. Ignoring 'queueSize'");
			}
			this.source = new FileReadingMessageSource(this.comparator);
		}
		else if (queueSizeSet) {
			this.source = new FileReadingMessageSource(this.queueSize);
		}
		else {
			this.source = new FileReadingMessageSource();
		}
		this.source.setDirectory(this.directory);
		if (this.scanner != null) {
			this.source.setScanner(this.scanner);
		}
		else {
			this.source.setUseWatchService(this.useWatchService);
			if (this.watchEvents != null) {
				this.source.setWatchEvents(this.watchEvents);
			}
		}
		configureFilterAndLockerOnSourceIfAny();
		if (this.scanEachPoll != null) {
			this.source.setScanEachPoll(this.scanEachPoll);
		}
		if (this.autoCreateDirectory != null) {
			this.source.setAutoCreateDirectory(this.autoCreateDirectory);
		}
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.source.setBeanFactory(beanFactory);
		}
		this.source.setBeanName(this.name);
		this.source.afterPropertiesSet();
	}

	private void configureFilterAndLockerOnSourceIfAny() {
		if (this.filter != null) {
			if (this.locker == null) {
				this.source.setFilter(this.filter);
			}
			else {
				CompositeFileListFilter<File> compositeFileListFilter = new CompositeFileListFilter<>();
				compositeFileListFilter.addFilter(this.filter);
				compositeFileListFilter.addFilter(this.locker);
				this.source.setFilter(compositeFileListFilter);
				this.source.setLocker(this.locker);
			}
		}
		else if (this.locker != null) {
			CompositeFileListFilter<File> compositeFileListFilter = new CompositeFileListFilter<>();
			compositeFileListFilter.addFilter(new FileListFilterFactoryBean().getObject());
			compositeFileListFilter.addFilter(this.locker);
			this.source.setFilter(compositeFileListFilter);
			this.source.setLocker(this.locker);
		}
	}

}
