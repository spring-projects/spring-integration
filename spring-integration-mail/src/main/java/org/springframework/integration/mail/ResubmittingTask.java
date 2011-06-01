/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.mail;

import java.util.Date;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0.2
 *
 * Will run the provided task right away resubmitting it after each successful execution 
 * or after receiving a {@link IllegalStateException}. If resubmission is after exception then 
 * the delay will be applied before resubmission. This is useful when the underlying task deals 
 * with reconnection logic 
 * Currently only used to manage IDLE task of ImapIdleChannelAdapter
 */
class ResubmittingTask implements Runnable{
	private static final Log logger = LogFactory.getLog(ResubmittingTask.class);
	private final Runnable targetTask;
	private final TaskScheduler scheduler;
	private final long delay;
	private Executor taskExecutor = new SimpleAsyncTaskExecutor();
	
	private volatile boolean running;
	
	public ResubmittingTask(Runnable targetTask, TaskScheduler scheduler, long delay) {
		this.targetTask = targetTask;
		this.scheduler = scheduler;
		this.delay = delay;
	}
	
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = (taskExecutor != null) ? taskExecutor : new SimpleAsyncTaskExecutor();
	}

	public void run() {
		taskExecutor.execute(new Runnable() {	
			public void run() {
				ResubmittingTask.this.invokeTask();
			}
		});	
	}
	
	protected void stop(){
		this.running = false;
	}
	
	protected void start(){
		this.running = true;
	}
	
	protected boolean isRunning(){
		return this.running;
	}
	
	private void invokeTask(){
		try {
			targetTask.run();
			if (this.running){
				if (logger.isDebugEnabled()){
					logger.debug("Task completed successfully. Re-scheduling it again right away");
				}
				scheduler.schedule(this, new Date());
			}
			else {
				if (logger.isDebugEnabled()){
					logger.debug("IDLE Task is stopped");
				}
			}
			
		}
		catch (IllegalStateException e) { //run again after a delay
			logger.warn("Failed to execute IDLE task. Will atempt to resubmit in " + delay + " milliseconds", e);
			scheduler.schedule(this, new Date(System.currentTimeMillis() + delay));	
		}
	}
}
