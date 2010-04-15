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

package org.springframework.integration.ws;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.gateway.SimpleMessagingGateway;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.server.endpoint.AbstractMarshallingPayloadEndpoint;

/**
 * @author Mark Fisher
 * @since 1.0.2
 */
public class MarshallingWebServiceInboundGateway extends AbstractMarshallingPayloadEndpoint
		implements BeanNameAware, BeanFactoryAware, InitializingBean, Lifecycle {

	private final SimpleMessagingGateway gatewayDelegate = new SimpleMessagingGateway();


	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code>.
	 * The {@link Marshaller} and {@link Unmarshaller} must be injected using properties. 
	 */
	public MarshallingWebServiceInboundGateway() {
	}

	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller.
	 * The Marshaller must also implement {@link Unmarshaller}, since it is used for both marshalling and
	 * unmarshalling.
	 * <p/>
	 * Note that all {@link Marshaller} implementations in Spring-OXM also implement the {@link Unmarshaller}
	 * interface, so you can safely use this constructor for any of those implementations.
	 * 
	 * @param marshaller object used as marshaller and unmarshaller
	 * @throws IllegalArgumentException when <code>marshaller</code> does not implement {@link Unmarshaller}
	 * @see #MarshallingWebServiceInboundGateway(Marshaller, Unmarshaller)
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller) {
		super(marshaller);
	}

	/**
	 * Creates a new <code>MarshallingWebServiceInboundGateway</code> with the given marshaller and unmarshaller.
	 */
	public MarshallingWebServiceInboundGateway(Marshaller marshaller, Unmarshaller unmarshaller) {
		super(marshaller, unmarshaller);
	}


	public void setRequestChannel(MessageChannel requestChannel) {
		this.gatewayDelegate.setRequestChannel(requestChannel);
	}

	public void setRequestTimeout(long requestTimeout) {
		this.gatewayDelegate.setRequestTimeout(requestTimeout);
	}

	public void setReplyChannel(MessageChannel replyChannel) {
		this.gatewayDelegate.setReplyChannel(replyChannel);
	}

	public void setReplyTimeout(long replyTimeout) {
		this.gatewayDelegate.setReplyTimeout(replyTimeout);
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.gatewayDelegate.setTaskScheduler(taskScheduler);
	}

	public void setAutoStartup(boolean autoStartup) {
		this.gatewayDelegate.setAutoStartup(autoStartup);
	}

	public void setBeanName(String beanName) {
		this.gatewayDelegate.setBeanName(beanName);
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		this.gatewayDelegate.setBeanFactory(beanFactory);
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		this.gatewayDelegate.afterPropertiesSet();
	}


	@Override
	protected Object invokeInternal(Object requestObject) throws Exception {
		try {
			return this.gatewayDelegate.sendAndReceive(requestObject);
		}
		catch (Exception e) {
			while (e.getClass().getName().startsWith("org.springframework") &&
					e.getCause() instanceof Exception) {
				e = (Exception) e.getCause();
			}
			throw e;
		}
	}


	// Lifecycle implementation

	public boolean isRunning() {
		return this.gatewayDelegate.isRunning();
	}

	public void start() {
		this.gatewayDelegate.start();
	}

	public void stop() {
		this.gatewayDelegate.stop();
	}

}
