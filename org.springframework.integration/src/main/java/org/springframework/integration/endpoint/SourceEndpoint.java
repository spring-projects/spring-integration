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

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.StaticMethodMatcherPointcutAdvisor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.DispatcherPolicy;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.dispatcher.DefaultPollingDispatcher;
import org.springframework.integration.dispatcher.PollingDispatcher;
import org.springframework.integration.dispatcher.PollingDispatcherTask;
import org.springframework.integration.message.Source;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves messages from a {@link Source}
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class SourceEndpoint extends AbstractEndpoint implements MessagingTask, InitializingBean {

	private final Schedule schedule;

	private final DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();

	private volatile PollingDispatcher dispatcher;

	private volatile List<Advice> dispatchAdviceChain;

	private volatile MessagingTask task;

	private volatile List<Advice> taskAdviceChain;

	private volatile boolean taskInitialized;

	private final Object taskMonitor = new Object();


	public SourceEndpoint(Source<?> source, MessageChannel channel, Schedule schedule) {
		Assert.notNull(source, "source must not be null");
		Assert.notNull(channel, "channel must not be null");
		Assert.notNull(schedule, "schedule must not be null");
		this.dispatcher = new DefaultPollingDispatcher(source, this.dispatcherPolicy);
		this.dispatcher.subscribe(channel);
		this.schedule = schedule;
	}


	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.dispatcherPolicy.setMaxMessagesPerTask(maxMessagesPerTask);
	}

	public void setSendTimeout(long sendTimeout) {
		this.dispatcher.setSendTimeout(sendTimeout);
	}

	public void setTaskAdviceChain(List<Advice> taskAdviceChain) {
		this.taskAdviceChain = taskAdviceChain;
	}

	public void setDispatchAdviceChain(List<Advice> dispatchAdviceChain) {
		this.dispatchAdviceChain = dispatchAdviceChain;
	}

	public Schedule getSchedule() {
		return this.schedule;
	}

	public void afterPropertiesSet() {
		this.initializeTask();
	}

	public void initializeTask() {
		synchronized (this.taskMonitor) {
			if (this.taskInitialized) {
				return;
			}
			this.refreshTask();
			this.taskInitialized = true;
		}
	}

	public void refreshTask() {
		synchronized (this.taskMonitor) {
			PollingDispatcher dispatcherProxy = null;
			if (this.dispatchAdviceChain != null && this.dispatchAdviceChain.size() > 0) {
				ProxyFactory proxyFactory = new ProxyFactory(this.dispatcher);
				proxyFactory.setInterfaces(new Class[] { PollingDispatcher.class });
				for (Advice advice : this.dispatchAdviceChain) {
					proxyFactory.addAdvisor(new MethodNameAdvisor(advice, "dispatch"));
				}
				dispatcherProxy = (PollingDispatcher) proxyFactory.getProxy();
			}
			this.task = new PollingDispatcherTask((dispatcherProxy != null) ? dispatcherProxy : this.dispatcher, this.schedule);
			if (this.taskAdviceChain != null && this.taskAdviceChain.size() > 0) {
				ProxyFactory proxyFactory = new ProxyFactory(this.task);
				proxyFactory.setInterfaces(new Class[] { MessagingTask.class });
				for (Advice advice : this.taskAdviceChain) {
					proxyFactory.addAdvisor(new MethodNameAdvisor(advice, "run"));
				}
				this.task = (MessagingTask) proxyFactory.getProxy();
			}
		}
	}

	private MessagingTask getTask() {
		synchronized (this.taskMonitor) {
			if (!this.taskInitialized) {
				this.initializeTask();
			}
			return this.task;
		}
	}

	public void run() {
		this.getTask().run();
	}


	@SuppressWarnings("serial")
	private static class MethodNameAdvisor extends StaticMethodMatcherPointcutAdvisor {

		private final String methodName;

		MethodNameAdvisor(Advice advice, String methodName) {
			super(advice);
			this.methodName = methodName;
		}

		@SuppressWarnings("unchecked")
		public boolean matches(Method method, Class targetClass) {
			return method.getName().equals(methodName);
		}
	}

}
