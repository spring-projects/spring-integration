/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.integration.flow.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.config.AbstractSimpleMessageHandlerFactoryBean;
import org.springframework.integration.flow.Flow;
import org.springframework.integration.flow.PortConfiguration;
import org.springframework.integration.flow.handler.FlowMessageHandler;
import org.springframework.util.Assert;

/**
 * Creates an instance of {@link FlowMessageHandler}
 * @author David Turanski
 * 
 */
public class FlowMessageHandlerFactoryBean extends AbstractSimpleMessageHandlerFactoryBean<FlowMessageHandler>
		implements InitializingBean {

	@SuppressWarnings("unused")
	private static Log logger = LogFactory.getLog(FlowMessageHandlerFactoryBean.class);

	private volatile Flow flow;

	private volatile String inputPortName;

	private volatile MessageChannel errorChannel;

	private volatile long timeout;

	// private volatile DirectChannel flowOutputChannel;

	private volatile PortConfiguration flowConfiguration;

	@Override
	protected FlowMessageHandler createHandler() {

		MessageChannel flowInputChannel = flow.resolveChannelName((String) flowConfiguration.getInputChannel());

		FlowMessageHandler flowMessageHandler = new FlowMessageHandler(flowInputChannel, flow.getFlowOutputChannel(),
				timeout);

		flowMessageHandler.setErrorChannel(this.errorChannel);

		return flowMessageHandler;
	}

	/**
	 * 
	 * @param flow the flow handled by the FlowMessageHandler
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	/**
	 * 
	 * @param inputPortName the flow input port associated with the handler. If
	 * not set and the flow defines only one input port, that will be used by
	 * default.
	 */
	public void setInputPortName(String inputPortName) {
		this.inputPortName = inputPortName;
	}

	/**
	 * 
	 * @param timeout send timeout for the handler
	 */
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	/**
	 * 
	 * @param errorChannel
	 */
	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	public void afterPropertiesSet() throws Exception {
		this.flowConfiguration = null;
		if (this.inputPortName == null) {
			Assert.isTrue(!(this.flow.getFlowConfiguration().getPortConfigurations().size() > 1),
					"flow [" + this.flow.getBeanName()
							+ "] exposes multiple port configurations. Must specify an input port");

			this.flowConfiguration = this.flow.getFlowConfiguration().getPortConfigurations().get(0);
			this.inputPortName = this.flowConfiguration.getInputPortName();
		}
		else {
			this.flowConfiguration = this.flow.getFlowConfiguration().getConfigurationForInputPort(this.inputPortName);
		}
	}

}
