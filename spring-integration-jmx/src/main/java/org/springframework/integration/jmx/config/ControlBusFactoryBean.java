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
package org.springframework.integration.jmx.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.jmx.ControlBus;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.monitor.IntegrationMBeanExporter;

/**
 * @author Dave Syer
 * @since 2.0
 *
 */
public class ControlBusFactoryBean implements FactoryBean<ControlBus> {
	
	private final SubscribableChannel operationChannel;
	private final IntegrationMBeanExporter exporter;

	public ControlBusFactoryBean(IntegrationMBeanExporter exporter, SubscribableChannel operationChannel) {
		this.exporter = exporter;
		this.operationChannel = operationChannel;
	}

	public ControlBusFactoryBean(IntegrationMBeanExporter exporter) {
		this(exporter, new DirectChannel());
	}

	public ControlBus getObject() throws Exception {
		return new ControlBus(exporter, exporter.getServer(), operationChannel);
	}

	public Class<?> getObjectType() {
		return ControlBus.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
