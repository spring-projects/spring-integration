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
package org.springframework.integration.osgi.extender;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.omg.CORBA.MARSHAL;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.controlbus.ControlBus;
import org.springframework.integration.osgi.AbstractSIConfigBundleTestDeployer;
import org.springframework.integration.osgi.stubs.SIBundleContextStub;
import org.springframework.osgi.config.internal.adapter.OsgiServiceRegistrationListenerAdapter;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.service.exporter.OsgiServiceRegistrationListener;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.springframework.osgi.util.OsgiServiceUtils;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class BusParserProducersTests extends AbstractSIConfigBundleTestDeployer  {
	@SuppressWarnings("unchecked")
	@Test
	public void testBusWithSubscribersBusPresent() throws Exception {
		SIBundleContextStub bundleContext = SIBundleContextStub.getInstance();
		this.deploySIConfig(bundleContext, 
                "org/springframework/integration/osgi/config/xml/", 
                "BusConfigParserTests-default.xml");
		ApplicationContext ac = this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/extender/", 
				                                    "BusParserProducersTests.xml");
		Map beans = ac.getBeansOfType(OsgiServiceFactoryBean.class);
		assertTrue(beans.size() == 1);
		OsgiServiceFactoryBean serviceExporter = (OsgiServiceFactoryBean) beans.values().toArray()[0];
		assertTrue(serviceExporter.getTargetBeanName().equals("exportedChannel"));
		DirectFieldAccessor serviceExporterAccessor = new DirectFieldAccessor(serviceExporter);
		OsgiServiceRegistrationListener[] listeners = 
			(OsgiServiceRegistrationListener[]) serviceExporterAccessor.getPropertyValue("listeners");
		assertTrue(listeners.length == 1);
		
		ServiceReference sr = 
			bundleContext.getServiceReferences(null, "(&(org.springframework.osgi.bean.name=exportedChannel))")[0];
		assertNotNull(sr);
	}
	@Test
	public void testBusWithSubscribersBusNotPresent() throws Exception {
		SIBundleContextStub bundleContext = SIBundleContextStub.getInstance();
		this.deploySIConfig(bundleContext, 
				                                    "org/springframework/integration/osgi/extender/", 
				                                    "BusParserProducersTests.xml");
		assertTrue(true); // if exception was not thrown we are ok
	}
}
