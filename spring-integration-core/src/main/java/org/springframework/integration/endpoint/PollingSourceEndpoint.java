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
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.scheduling.MessagingTask;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.util.Assert;

/**
 * A channel adapter that retrieves messages from a {@link PollableSource}
 * and then sends the resulting messages to the provided {@link MessageChannel}.
 * 
 * @author Mark Fisher
 */
public class PollingSourceEndpoint extends AbstractSourceEndpoint implements MessagingTask, InitializingBean {

	private final DispatcherPolicy dispatcherPolicy = new DispatcherPolicy();

	private volatile PollingDispatcher dispatcher;

	private volatile MessagingTask task;

	private volatile List<Advice> taskAdviceChain;

	private volatile List<Advice> dispatchAdviceChain;

	private volatile boolean proxiesInitialized;

	private final Object proxyInitializationMonitor = new Object();


	public PollingSourceEndpoint(PollableSource<?> source, MessageChannel channel, PollingSchedule schedule) {
		super(source, channel);
		Assert.notNull(schedule, "schedule must not be null");
		this.dispatcher = new DefaultPollingDispatcher(source, this.dispatcherPolicy);
		this.dispatcher.subscribe(this.getChannel());
		this.task = new PollingDispatcherTask(this.dispatcher, schedule);
	}


	public void setTaskAdviceChain(List<Advice> taskAdviceChain) {
		this.taskAdviceChain = taskAdviceChain;
	}

	public void setDispatchAdviceChain(List<Advice> dispatchAdviceChain) {
		this.dispatchAdviceChain = dispatchAdviceChain;
	}

	public void afterPropertiesSet() {
		this.initializeProxies();
	}

	public void initializeProxies() {
		synchronized (this.proxyInitializationMonitor) {
			if (this.proxiesInitialized) {
				return;
			}
			if (this.dispatchAdviceChain != null && this.dispatchAdviceChain.size() > 0) {
				ProxyFactory proxyFactory = new ProxyFactory(this.dispatcher);
				proxyFactory.setInterfaces(new Class[] { PollingDispatcher.class });
				for (Advice advice : this.dispatchAdviceChain) {
					proxyFactory.addAdvisor(new MethodNameAdvisor(advice, "dispatch"));
				}
				this.dispatcher = (PollingDispatcher) proxyFactory.getProxy();
				this.task = new PollingDispatcherTask(this.dispatcher, this.task.getSchedule());
			}
			if (this.taskAdviceChain != null && this.taskAdviceChain.size() > 0) {
				ProxyFactory proxyFactory = new ProxyFactory(this.task);
				proxyFactory.setInterfaces(new Class[] { MessagingTask.class });
				for (Advice advice : this.taskAdviceChain) {
					proxyFactory.addAdvisor(new MethodNameAdvisor(advice, "run"));
				}
				this.task = (MessagingTask) proxyFactory.getProxy();
			}
			this.proxiesInitialized = true;
		}
	}

	public void setMaxMessagesPerTask(int maxMessagesPerTask) {
		this.dispatcherPolicy.setMaxMessagesPerTask(maxMessagesPerTask);
	}

	public void setSendTimeout(long sendTimeout) {
		this.dispatcher.setSendTimeout(sendTimeout);
	}

	public Schedule getSchedule() {
		return this.task.getSchedule();
	}

	public void run() {
		this.task.run();
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
