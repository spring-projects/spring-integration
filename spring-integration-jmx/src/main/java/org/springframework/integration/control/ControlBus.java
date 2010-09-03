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

package org.springframework.integration.control;

import javax.management.MBeanServer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.jmx.OperationInvokingMessageHandler;
import org.springframework.integration.monitor.ObjectNameLocator;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * JMX-based Control Bus implementation. Exports all channel and endpoint beans from a given BeanFactory as MBeans.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ControlBus implements BeanFactoryAware, InitializingBean {

	public static final String TARGET_BEAN_NAME = JmxHeaders.PREFIX + "_controlBus_targetBeanName";

	private final SubscribableChannel operationChannel;

	private volatile ListableBeanFactory beanFactory;

	private final ObjectNameLocator exporter;

	private final MBeanServer server;

	/**
	 * Create a {@link ControlBus}.
	 */
	public ControlBus(ObjectNameLocator locator, MBeanServer server, SubscribableChannel operationChannel) {
		this.exporter = locator;
		this.server = server;
		this.operationChannel = operationChannel;
	}

	/**
	 * Returns the channel to which operation-invoking Messages may be sent. Any messages sent to this channel must
	 * contain {@link ControlBus#TARGET_BEAN_NAME} and {@link JmxHeaders#OPERATION_NAME} header values, and the target
	 * bean name must match one that has been exported by this Control Bus. If the operation returns a result, the
	 * {@link MessageHeaders#REPLY_CHANNEL} header is also required.
	 */
	public SubscribableChannel getOperationChannel() {
		return this.operationChannel;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isTrue(beanFactory instanceof ListableBeanFactory, "A ListableBeanFactory is required.");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		OperationInvokingMessageHandler handler = new ControlBusOperationInvokingMessageHandler();
		handler.setBeanFactory(this.beanFactory);
		handler.setServer(this.server);
		handler.afterPropertiesSet();
		this.operationChannel.subscribe(handler);
	}

	private class ControlBusOperationInvokingMessageHandler extends OperationInvokingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			String beanName = requestMessage.getHeaders().get(TARGET_BEAN_NAME, String.class);
			Assert.notNull(beanName, "The ControlBus.TARGET_BEAN_NAME is required.");
			String objectName = exporter.getObjectName(beanName);
			Assert.notNull(objectName, "ControlBus has not exported an MBean for '" + beanName + "'");
			requestMessage = MessageBuilder.fromMessage(requestMessage).setHeader(JmxHeaders.OBJECT_NAME, objectName)
					.setHeader(TARGET_BEAN_NAME, null).build();
			return super.handleRequestMessage(requestMessage);
		}
	}

}
