/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.endpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;

/**
 * The base class for Message Endpoint implementations.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractEndpoint implements MessageEndpoint, TaskSchedulerAware, BeanNameAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile TaskScheduler taskScheduler;


	public void setBeanName(String name) {
		this.name = name;
	}

	protected TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

}
