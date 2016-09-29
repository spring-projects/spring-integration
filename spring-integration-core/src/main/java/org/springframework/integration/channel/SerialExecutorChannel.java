/* Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.channel;

import java.util.concurrent.Executor;

import org.springframework.integration.dispatcher.LoadBalancingStrategy;
import org.springframework.integration.dispatcher.SerialUnicastingDispatcher;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.util.ErrorHandler;
/**
 * This variation of {@link ExecutorChannel} designed to work with {@link SerialUnicastingDispatcher}
 * which will dispatch Messages that have the same 'correlationId' headers serially and 
 * in sequence (based on the 'sequenceNumber' header) while Messages with different 'correlationId' will be 
 * dispatched concurrently. 
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class SerialExecutorChannel extends AbstractSubscribableChannel {
	
	private volatile Executor executor;
	
	private volatile SerialUnicastingDispatcher dispatcher;
	
	private volatile boolean failover = true;

	private volatile LoadBalancingStrategy loadBalancingStrategy;
	
	public SerialExecutorChannel(){
		this(null, null);
	}
	
	public SerialExecutorChannel(Executor executor){
		this(executor, null);
	}
	
	public SerialExecutorChannel(Executor executor, LoadBalancingStrategy loadBalancingStrategy){
		this.executor = executor;
		this.loadBalancingStrategy = loadBalancingStrategy;
	}

	@Override
	protected SerialUnicastingDispatcher getDispatcher() {
		return this.dispatcher;
	}
	
	public void setFailover(boolean failover) {
		this.failover = failover;
		this.dispatcher.setFailover(failover);
	}

	@Override
	public final void onInit() {
		
		if (this.executor != null){
			if (!(this.executor instanceof ErrorHandlingTaskExecutor)) {
				ErrorHandler errorHandler = new MessagePublishingErrorHandler(
						new BeanFactoryChannelResolver(this.getBeanFactory()));
				this.executor = new ErrorHandlingTaskExecutor(this.executor, errorHandler);
			}
			this.dispatcher = new SerialUnicastingDispatcher(this.executor);
		}
		
		this.dispatcher.setFailover(this.failover);
		if (this.loadBalancingStrategy != null) {
			this.dispatcher.setLoadBalancingStrategy(this.loadBalancingStrategy);
		}
	}

}
