/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;
import org.springframework.integration.file.tail.OSDelegatingFileTailingMessageProducer;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ali Shahbour
 *
 * @since 3.0
 *
 */
public class FileTailInboundChannelAdapterFactoryBean extends AbstractFactoryBean<FileTailingMessageProducerSupport>
		implements BeanNameAware, SmartLifecycle, ApplicationEventPublisherAware {

	private String nativeOptions;

	private boolean enableStatusReader = true;

	private Long idleEventInterval;

	private File file;

	private TaskExecutor taskExecutor;

	private TaskScheduler taskScheduler;

	private Long delay;

	private Long fileDelay;

	private Boolean end;

	private Boolean reopen;

	private FileTailingMessageProducerSupport tailAdapter;

	private String beanName;

	private MessageChannel outputChannel;

	private MessageChannel errorChannel;

	private String outputChannelName;

	private String errorChannelName;

	private Boolean autoStartup;

	private Integer phase;

	private ApplicationEventPublisher applicationEventPublisher;

	private long sendTimeout;

	private boolean shouldTrack;

	private ErrorMessageStrategy errorMessageStrategy;

	public void setNativeOptions(String nativeOptions) {
		if (StringUtils.hasText(nativeOptions)) {
			this.nativeOptions = nativeOptions;
		}
	}

	/**
	 * If false, thread for capturing stderr will not be started
	 * and stderr output will be ignored.
	 * @param enableStatusReader true or false
	 * @since 4.3.6
	 */
	public void setEnableStatusReader(boolean enableStatusReader) {
		this.enableStatusReader = enableStatusReader;
	}

	/**
	 * How often to emit {@link FileTailingMessageProducerSupport.FileTailingIdleEvent}s in milliseconds.
	 * @param idleEventInterval the interval.
	 * @since 5.0
	 */
	public void setIdleEventInterval(long idleEventInterval) {
		this.idleEventInterval = idleEventInterval;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void setDelay(Long delay) {
		this.delay = delay;
	}

	public void setFileDelay(Long fileDelay) {
		this.fileDelay = fileDelay;
	}

	public void setEnd(Boolean end) {
		this.end = end;
	}

	public void setReopen(Boolean reopen) {
		this.reopen = reopen;
	}

	@Override
	public void setBeanName(@Nullable String name) {
		this.beanName = name;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void setErrorChannelName(String errorChannelName) {
		this.errorChannelName = errorChannelName;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	public void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		this.errorMessageStrategy = errorMessageStrategy;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void start() {
		if (this.tailAdapter != null) {
			this.tailAdapter.start();
		}
	}

	@Override
	public void stop() {
		if (this.tailAdapter != null) {
			this.tailAdapter.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.tailAdapter != null && this.tailAdapter.isRunning();
	}

	@Override
	public int getPhase() {
		if (this.tailAdapter != null) {
			return this.tailAdapter.getPhase();
		}
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		return this.tailAdapter != null && this.tailAdapter.isAutoStartup();
	}

	@Override
	public void stop(Runnable callback) {
		if (this.tailAdapter != null) {
			this.tailAdapter.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public Class<?> getObjectType() {
		return this.tailAdapter == null ? FileTailingMessageProducerSupport.class : this.tailAdapter.getClass();
	}

	@Override
	protected FileTailingMessageProducerSupport createInstance() {
		FileTailingMessageProducerSupport adapter;
		if (this.delay == null && this.end == null && this.reopen == null) {
			adapter = new OSDelegatingFileTailingMessageProducer();
			((OSDelegatingFileTailingMessageProducer) adapter).setEnableStatusReader(this.enableStatusReader);
			if (this.nativeOptions != null) {
				((OSDelegatingFileTailingMessageProducer) adapter).setOptions(this.nativeOptions);
			}
		}
		else {
			Assert.isTrue(this.nativeOptions == null,
					"'native-options' is not allowed with 'delay', 'end', or 'reopen'");
			ApacheCommonsFileTailingMessageProducer apache = new ApacheCommonsFileTailingMessageProducer();
			JavaUtils.INSTANCE
					.acceptIfNotNull(this.delay, apache::setPollingDelay)
					.acceptIfNotNull(this.end, apache::setEnd)
					.acceptIfNotNull(this.reopen, apache::setReopen);
			adapter = apache;
		}
		adapter.setFile(this.file);
		adapter.setOutputChannel(this.outputChannel);
		adapter.setErrorChannel(this.errorChannel);
		adapter.setBeanName(this.beanName);
		adapter.setSendTimeout(this.sendTimeout);
		adapter.setShouldTrack(this.shouldTrack);
		BeanFactory beanFactory = getBeanFactory();
		JavaUtils.INSTANCE
				.acceptIfNotNull(this.taskExecutor, adapter::setTaskExecutor)
				.acceptIfNotNull(this.taskScheduler, adapter::setTaskScheduler)
				.acceptIfNotNull(this.fileDelay, adapter::setTailAttemptsDelay)
				.acceptIfNotNull(this.idleEventInterval, adapter::setIdleEventInterval)
				.acceptIfNotNull(this.autoStartup, adapter::setAutoStartup)
				.acceptIfNotNull(this.phase, adapter::setPhase)
				.acceptIfNotNull(this.applicationEventPublisher, adapter::setApplicationEventPublisher)
				.acceptIfNotNull(this.outputChannelName, adapter::setOutputChannelName)
				.acceptIfNotNull(this.errorChannelName, adapter::setErrorChannelName)
				.acceptIfNotNull(this.errorMessageStrategy, adapter::setErrorMessageStrategy)
				.acceptIfNotNull(beanFactory, adapter::setBeanFactory);
		adapter.afterPropertiesSet();
		this.tailAdapter = adapter;
		return adapter;
	}

}
