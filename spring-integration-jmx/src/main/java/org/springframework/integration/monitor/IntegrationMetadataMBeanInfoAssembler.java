/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.monitor;

import javax.management.Descriptor;

import org.springframework.integration.support.management.LifecycleMessageHandlerMetrics;
import org.springframework.integration.support.management.LifecycleMessageSourceMetrics;
import org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler;
import org.springframework.jmx.export.metadata.JmxAttributeSource;

/**
 * The {@link MetadataMBeanInfoAssembler} extension to assemble metadata MBean info
 * from the {@link LifecycleMessageSourceMetrics} or {@link LifecycleMessageHandlerMetrics}
 * managed bean's delegate.
 * <p>
 * All other managed beans are left as is.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class IntegrationMetadataMBeanInfoAssembler extends MetadataMBeanInfoAssembler {

	public IntegrationMetadataMBeanInfoAssembler(JmxAttributeSource attributeSource) {
		super(attributeSource);
	}

	@Override
	protected String getDescription(Object managedBean, String beanKey) {
		return super.getDescription(extractManagedBean(managedBean), beanKey);
	}

	@Override
	protected void populateMBeanDescriptor(Descriptor desc, Object managedBean, String beanKey) {
		super.populateMBeanDescriptor(desc, extractManagedBean(managedBean), beanKey);
	}

	private Object extractManagedBean(Object managedBean) {
		if (managedBean instanceof LifecycleMessageSourceMetrics) {
			return ((LifecycleMessageSourceMetrics) managedBean).getDelegate();
		}
		else if (managedBean instanceof LifecycleMessageHandlerMetrics) {
			return ((LifecycleMessageHandlerMetrics) managedBean).getDelegate();
		}
		return managedBean;
	}

}
