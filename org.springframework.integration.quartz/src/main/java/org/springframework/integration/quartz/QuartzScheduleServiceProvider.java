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

package org.springframework.integration.quartz;

import java.util.Date;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.CronTrigger;
import org.quartz.InterruptableJob;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.UnableToInterruptJobException;
import org.quartz.listeners.JobListenerSupport;

import org.springframework.integration.scheduling.spi.ScheduleServiceProvider;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

/**
 * A Quartz-based implementation of the {@link org.springframework.integration.scheduling.spi.ScheduleServiceProvider}.
 *
 * @author Marius Bogoevici
 */
public class QuartzScheduleServiceProvider implements ScheduleServiceProvider {

	private static final String RUN_METHOD_NAME = "run";

	private static final String RUNNABLE_INSTANCE = "runnable.instance";

	private static final String FIXED_DELAY_PARAMETER = "fixedDelay";

	private static final String TIME_UNIT_PARAMETER = "timeUnit";

	private static final String TRIGGER_NAME_PARAMETER = "triggerName";

	private static final AtomicLong sequenceNumber = new AtomicLong(Long.MIN_VALUE);

	private final Scheduler scheduler;

	private final JobListener fixedDelayJobListener;


	public QuartzScheduleServiceProvider(Scheduler scheduler) {
		this.scheduler = scheduler;
		this.fixedDelayJobListener = new FixedDelayJobListener();
		try {
			this.scheduler.addJobListener(this.fixedDelayJobListener);
		}
		catch (SchedulerException e) {
			throw new QuartzSchedulingException(e);
		}
	}


	public void execute(Runnable runnable) {
		try {
			scheduler.scheduleJob(wrapAsJobDetail(runnable),
					TriggerUtils.makeImmediateTrigger(generateNameForInstance(runnable), 0, 0l));
		}
		catch (Exception e) {
			throw new QuartzSchedulingException(e);
		}
	}

	public void shutdown(boolean waitForTasksToCompleteOnShutdown) {
		try {
			this.scheduler.shutdown(waitForTasksToCompleteOnShutdown);
		}
		catch (SchedulerException e) {
			throw new QuartzSchedulingException(e);
		}
	}

	public ScheduledFuture<?> scheduleWithInitialDelay(Runnable runnable, long initialDelay, TimeUnit timeUnit)
			throws Exception {
		getFutureDate(initialDelay, timeUnit);
		Trigger initialDelayTrigger = new SimpleTrigger(generateNameForInstance(runnable), Scheduler.DEFAULT_GROUP,
				getFutureDate(initialDelay, timeUnit));
		return scheduleWithTrigger(runnable, initialDelayTrigger, null);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long initialDelay, long period, TimeUnit timeUnit)
			throws Exception {
		Trigger fixedRateTrigger = new SimpleTrigger(generateNameForInstance(runnable), Scheduler.DEFAULT_GROUP,
				getFutureDate(initialDelay, timeUnit), null, SimpleTrigger.REPEAT_INDEFINITELY,
				TimeUnit.MILLISECONDS.convert(period, timeUnit));
		return scheduleWithTrigger(runnable, fixedRateTrigger, null);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long initialDelay, long delay,
	                                                 TimeUnit timeUnit) throws Exception {
		getFutureDate(initialDelay, timeUnit);
		Trigger fixedDelayTrigger = createFixedDelayTrigger(runnable, initialDelay, delay, timeUnit);
		return scheduleWithTrigger(runnable, fixedDelayTrigger, new String[]{this.fixedDelayJobListener.getName()});
	}

	public ScheduledFuture<?> scheduleWithCronExpression(Runnable runnable, String cronExpression)
			throws Exception {
		return scheduleWithTrigger(runnable, new CronTrigger(generateNameForInstance(runnable), Scheduler.DEFAULT_GROUP,
				cronExpression), null);
	}

	private ScheduledFuture<?> scheduleWithTrigger(Runnable runnable, Trigger fixedRateTrigger,
	                                               String[] jobListenerNames) throws Exception {
		JobDetail jobDetail = wrapAsJobDetail(runnable);
		if (null != jobListenerNames) {
			for (String jobListenerName : jobListenerNames) {
				jobDetail.addJobListener(jobListenerName);
			}
		}
		this.scheduler.scheduleJob(jobDetail, fixedRateTrigger);
		return new ScheduledFutureJobWrapper(scheduler, jobDetail, fixedRateTrigger);
	}

	private static JobDetail wrapAsJobDetail(Runnable runnable) throws Exception {
		MethodInvokingJobDetailFactoryBean factoryBean = new MethodInvokingJobDetailFactoryBean();
		factoryBean.setTargetObject(runnable);
		factoryBean.setTargetMethod(RUN_METHOD_NAME);
		factoryBean.setBeanName(generateNameForInstance(runnable));
		factoryBean.afterPropertiesSet();
		JobDetail jobDetail = (JobDetail) factoryBean.getObject();
		jobDetail.setJobClass(InterruptableMethodInvokingJob.class);
		jobDetail.getJobDataMap().put(RUNNABLE_INSTANCE, runnable);
		return jobDetail;
	}

	private static String generateNameForInstance(Object instance) {
		return instance.getClass().getName() +  "#" + sequenceNumber.incrementAndGet();
	}

	private static Date getFutureDate(long delay, TimeUnit timeUnit) {
		return new Date(new Date().getTime() + TimeUnit.MILLISECONDS.convert(delay, timeUnit));
	}

	private static Trigger createFixedDelayTrigger(Runnable runnable, long initialDelay, long taskDelay, TimeUnit timeUnit) {
		Trigger fixedDelayTrigger = new SimpleTrigger(generateNameForInstance(runnable), Scheduler.DEFAULT_GROUP,
				getFutureDate(initialDelay, timeUnit));
		fixedDelayTrigger.getJobDataMap().put(FIXED_DELAY_PARAMETER, taskDelay);
		fixedDelayTrigger.getJobDataMap().put(TIME_UNIT_PARAMETER, timeUnit);
		fixedDelayTrigger.getJobDataMap().put(TRIGGER_NAME_PARAMETER, fixedDelayTrigger.getName());
		return fixedDelayTrigger;
	}

	/**
	 * 	Wrapper class for a Quartz {@link Job}, allowing running Quartz jobs top be manipulated via
	 *  the {@link java.util.concurrent.ScheduledFuture} interface.
	 *  It is designed to be used for periodic tasks, therefore get() either block or throw
	 *  {@link java.util.concurrent.CancellationException}.
	 */
	public class ScheduledFutureJobWrapper implements ScheduledFuture<Object> {

		private final Scheduler scheduler;

		private final JobDetail jobDetail;

		private final Trigger trigger;

		private final CountDownLatch cancellationLatch = new CountDownLatch(1);


		private ScheduledFutureJobWrapper(Scheduler scheduler, JobDetail jobDetail, Trigger trigger) {
			this.scheduler = scheduler;
			this.jobDetail = jobDetail;
			this.trigger = trigger;
		}


		public long getDelay(TimeUnit unit) {
			long nextFireTime = this.trigger.getNextFireTime().getTime();
			long timeNow = new Date().getTime();
			return nextFireTime > timeNow ? unit.convert(nextFireTime - timeNow, TimeUnit.MILLISECONDS) : 0;
		}

		public int compareTo(Delayed o) {
			return new Long(getDelay(TimeUnit.MILLISECONDS)).compareTo(o.getDelay(TimeUnit.MILLISECONDS));
		}

		/*
		 * Synchronized for thread-safety. This is not supposed to be a highly concurrent operation, therefore
		 * contention should be minimal.
		 */
		public synchronized boolean cancel(boolean mayInterruptIfRunning) {
			if (this.cancellationLatch.getCount() == 0) {
				return true;
			}
			try {
				if (mayInterruptIfRunning) {
					this.scheduler.interrupt(this.jobDetail.getName(), Scheduler.DEFAULT_GROUP);
				}
				if (this.scheduler.deleteJob(this.jobDetail.getName(), Scheduler.DEFAULT_GROUP)) {
					this.cancellationLatch.countDown();
					return true;
				}
				else {
					return false;
				}
			}
			catch (SchedulerException e) {
				throw new QuartzSchedulingException(e);
			}
		}

		public boolean isCancelled() {
			return this.cancellationLatch.getCount() == 0;
		}

		public boolean isDone() {
			return isCancelled();
		}

		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			this.cancellationLatch.await(timeout, unit);
			throw new CancellationException();
		}

		public Object get() throws InterruptedException, ExecutionException {
			this.cancellationLatch.await();
			throw new CancellationException();
		}
	}

	/**
	 * Wrapper class allowing for Quartz jobs to be interrupted.
	 */
	public static class InterruptableMethodInvokingJob extends MethodInvokingJobDetailFactoryBean.MethodInvokingJob
			implements InterruptableJob {

		private Thread executionThread;

		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
			this.executionThread = Thread.currentThread();
			super.executeInternal(context);
		}

		public void interrupt() throws UnableToInterruptJobException {
			this.executionThread.interrupt();
		}

	}


	/**
	 *  {@link org.quartz.JobListener} used for re-scheduling a fixed delay task, as Quartz does
	 *  not support fixed delay tasks out of the box.
	 */
	public class FixedDelayJobListener extends JobListenerSupport {

		private final String name;


		private FixedDelayJobListener() {
			this.name = generateNameForInstance(this);
		}

		
		public String getName() {
			return name;
		}

		public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
			try {
				Trigger trigger = createFixedDelayTrigger((Runnable) context.getMergedJobDataMap().get(
						RUNNABLE_INSTANCE),
						context.getMergedJobDataMap().getLong(FIXED_DELAY_PARAMETER),
						context.getMergedJobDataMap().getLong(FIXED_DELAY_PARAMETER),
						(TimeUnit) context.getMergedJobDataMap().get(TIME_UNIT_PARAMETER));
				trigger.setJobGroup(context.getJobDetail().getGroup());
				trigger.setJobName(context.getJobDetail().getName());
				scheduler.rescheduleJob(context.getMergedJobDataMap().getString(TRIGGER_NAME_PARAMETER),
						Scheduler.DEFAULT_GROUP, trigger); 
			}
			catch (Exception e) {
				throw new QuartzSchedulingException(e);
			}
		}

	}

}
