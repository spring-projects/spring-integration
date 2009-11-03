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
package org.springframework.integration.osgi.stubs;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SIBundleContextStubTest {

	@Test
	public void validateServiceRegistrationWithNoFilters(){
		BundleContext context = SIBundleContextStub.getInstance();
		context.registerService(Serializable.class.getName(), new Serializable(){}, null);

		ServiceReference sr = context.getServiceReference(Serializable.class.getName());
		Assert.assertNotNull(sr);
		Serializable service = (Serializable) context.getService(sr);
		Assert.assertNotNull(service);
	}
	@Test
	public void validateServiceRegistrationWithFiltersNegativeReturn() throws Exception {
		BundleContext context = SIBundleContextStub.getInstance();
		Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put("name", "foo");
		context.registerService(Serializable.class.getName(), new Serializable(){}, properties);
	
		ServiceReference[] srs = context.getServiceReferences(Serializable.class.getName(), "(&(name=bar))");
		Assert.assertTrue("Expected no ServiceReferences", srs == null);
	}
	@Test
	public void validateServiceRegistrationWithFiltersPositiveReturn() throws Exception {
		BundleContext context = SIBundleContextStub.getInstance();
		Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put("name", "foo");
		context.registerService(Serializable.class.getName(), new Serializable(){}, properties);
		
		properties = new Hashtable<String, String>();
		properties.put("name", "bar");
		context.registerService(Serializable.class.getName(), new Serializable(){}, properties);
	
		ServiceReference[] srs = context.getServiceReferences(Serializable.class.getName(), "(&(name=bar))");
		Assert.assertNotNull(srs);
	}
	@Test
	public void validateServiceListenerRegistrationWithFilter() throws Exception{
		BundleContext context = SIBundleContextStub.getInstance();
		ServiceListener sl = Mockito.mock(ServiceListener.class);
		context.addServiceListener(sl, "(&(name=foo))");
		Dictionary<String, String> properties = new Hashtable<String, String>();
		properties.put("name", "foo");
		ServiceRegistration sr = 
			context.registerService(Serializable.class.getName(), new Serializable(){}, properties);
		Mockito.verify(sl, Mockito.times(1)).serviceChanged((ServiceEvent) Mockito.anyObject());
	}
}
