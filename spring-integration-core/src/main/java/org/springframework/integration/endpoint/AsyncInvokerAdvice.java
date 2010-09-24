/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.concurrent.Executor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.util.ErrorHandler;

/**
 * Simple advise to support async execution of tasks.
 * It will simply delegate <code>invocation.proceed()</code> calls to its {@link TaskExecutor}
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class AsyncInvokerAdvice implements MethodInterceptor, InitializingBean,BeanFactoryAware {
	private Executor taskExecutor;
	private volatile ErrorHandler errorHandler;
	private BeanFactory beanFactory;
	/**
	 * @param taskExecutor
	 */
	public AsyncInvokerAdvice(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}
	/*
	 * (non-Javadoc)
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		taskExecutor.execute(new Runnable() {	
			public void run() {
				try {
					invocation.proceed();
				} catch (Throwable e) {
					if (e instanceof RuntimeException){
						throw (RuntimeException)e;
					} else {
						throw new MessagingException("Problems during asynchronous invocation of task: " + this, e);
					}
				}
			}
		});
		return null;
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		if (!(this.taskExecutor instanceof ErrorHandlingTaskExecutor)) {
			if (this.errorHandler == null) {
				this.errorHandler = new MessagePublishingErrorHandler(
						new BeanFactoryChannelResolver(this.beanFactory));
			}
			this.taskExecutor = new ErrorHandlingTaskExecutor(taskExecutor, errorHandler);
		}
	}
	/*
	 * (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}