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
package org.springframework.integration.scheduling;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Helper class for creating predefined {@link TaskScheduler} classes.
 * 
 * @author Marius Bogoevici
 */
public class Schedulers {

	public static TaskScheduler createDefaultTaskExecutor(int poolSize, ThreadFactory threadFactory) {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(poolSize);
		executor.setThreadFactory(threadFactory);
		executor.setRejectedExecutionHandler(new CallerRunsPolicy());
		executor.afterPropertiesSet();
		return new SimpleTaskScheduler(executor);
	}
	
	public static TaskScheduler createDefaultTaskScheduler(int poolSize) {
		return createDefaultTaskExecutor(poolSize, null);
	}

}
