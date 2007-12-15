/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultBeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.endpoint.GenericMessageEndpoint;
import org.springframework.integration.endpoint.InboundMethodInvokingChannelAdapter;
import org.springframework.integration.endpoint.OutboundMethodInvokingChannelAdapter;
import org.springframework.integration.handler.DefaultMessageHandlerAdapter;
import org.springframework.util.Assert;

/**
 * Factory for creating integration component bean definitions.
 * 
 * @author Mark Fisher
 */
public class ComponentConfigurer {

	private BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator;


	public ComponentConfigurer(BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
		Assert.notNull(registry, "registry must not be null");
		this.registry = registry;
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new DefaultBeanNameGenerator());
	}

	public String serviceActivator(String inputChannel, String outputChannel, String objectRef, String method) {
		RootBeanDefinition endpointDef = new RootBeanDefinition(GenericMessageEndpoint.class);
		RootBeanDefinition adapterDef = new RootBeanDefinition(DefaultMessageHandlerAdapter.class);
		adapterDef.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(objectRef));
		adapterDef.getPropertyValues().addPropertyValue("method", method);
		String adapterName = beanNameGenerator.generateBeanName(adapterDef, this.registry);
		this.registry.registerBeanDefinition(adapterName, adapterDef);
		endpointDef.getPropertyValues().addPropertyValue("handler", new RuntimeBeanReference(adapterName));
		if (inputChannel != null) {
			endpointDef.getPropertyValues().addPropertyValue("inputChannelName", inputChannel);
		}
		if (outputChannel != null) {
			endpointDef.getPropertyValues().addPropertyValue("defaultOutputChannelName", outputChannel);
		}
		String endpointName = beanNameGenerator.generateBeanName(endpointDef, this.registry);
		this.registry.registerBeanDefinition(endpointName, endpointDef);
		return endpointName;
	}

	public String inboundChannelAdapter(String objectRef, String method) {
		RootBeanDefinition bd = new RootBeanDefinition(InboundMethodInvokingChannelAdapter.class);
		bd.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(objectRef));
		bd.getPropertyValues().addPropertyValue("method", method);
		String beanName = this.beanNameGenerator.generateBeanName(bd, this.registry);
		this.registry.registerBeanDefinition(beanName, bd);
		return beanName;
	}

	public String outboundChannelAdapter(String objectRef, String method) {
		RootBeanDefinition bd = new RootBeanDefinition(OutboundMethodInvokingChannelAdapter.class);
		bd.getPropertyValues().addPropertyValue("object", new RuntimeBeanReference(objectRef));
		bd.getPropertyValues().addPropertyValue("method", method);
		String beanName = this.beanNameGenerator.generateBeanName(bd, this.registry);
		this.registry.registerBeanDefinition(beanName, bd);
		return beanName;
	}

}
