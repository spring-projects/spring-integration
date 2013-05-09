/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.file.tail.ApacheCommonsFileTailingMessageProducer;
import org.springframework.integration.file.tail.FileTailingMessageProducerSupport;
import org.springframework.integration.file.tail.OSDelegatingFileTailingMessageProducer;
import org.springframework.util.Assert;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class FileTailInboundChannelAdapterFactoryBean extends AbstractFactoryBean<FileTailingMessageProducerSupport>
		implements BeanNameAware {

	private volatile String nativeOptions;

	private volatile File file;

	private volatile TaskExecutor taskExecutor;

	private volatile Long delay;

	private volatile Long fileDelay;

	private volatile FileTailingMessageProducerSupport adapter;

	private volatile String beanName;

	private volatile MessageChannel outputChannel;

	private volatile Boolean autoStartup;

	private volatile Integer phase;

	public void setNativeOptions(String nativeOptions) {
		this.nativeOptions = nativeOptions;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public void setFileDelay(long fileDelay) {
		this.fileDelay = fileDelay;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public Class<?> getObjectType() {
		return this.adapter == null ? FileTailingMessageProducerSupport.class : this.adapter.getClass();
	}

	@Override
	protected FileTailingMessageProducerSupport createInstance() throws Exception {
		FileTailingMessageProducerSupport adapter;
		if (this.delay == null && this.fileDelay == null) {
			adapter = new OSDelegatingFileTailingMessageProducer();
			if (this.nativeOptions != null) {
				((OSDelegatingFileTailingMessageProducer) adapter).setOptions(this.nativeOptions);
			}
		}
		else {
			Assert.isNull(this.nativeOptions, "Cannot have 'delay' or 'file-delay' with a native adapter");
			adapter = new ApacheCommonsFileTailingMessageProducer();
			if (this.delay != null) {
				((ApacheCommonsFileTailingMessageProducer) adapter).setPollingDelay(this.delay);
			}
			if (this.fileDelay != null) {
				((ApacheCommonsFileTailingMessageProducer) adapter).setMissingFileDelay(this.fileDelay);
			}
		}
		adapter.setFile(this.file);
		adapter.setTaskExecutor(this.taskExecutor);
		adapter.setOutputChannel(outputChannel);
		adapter.setBeanName(this.beanName);
		if (this.autoStartup != null) {
			adapter.setAutoStartup(this.autoStartup);
		}
		if (this.phase != null) {
			adapter.setPhase(this.phase);
		}
		adapter.afterPropertiesSet();
		this.adapter = adapter;
		return adapter;
	}

}
