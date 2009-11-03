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

import java.util.Dictionary;
import java.util.Set;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.osgi.mock.MockServiceRegistration;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SIServiceRegistrationStub extends MockServiceRegistration {
	private SIBundleContextStub context;
	
	public SIServiceRegistrationStub(String[] clazz, Dictionary props) {
		super(clazz, props);
	}
	public void setBundleContext(SIBundleContextStub context){
		this.context = context;
	}
	public void unregister() {
		ServiceReference ref = this.getReference();
		DirectFieldAccessor refAccessor = new DirectFieldAccessor(ref);
		Dictionary properties = (Dictionary) refAccessor.getPropertyValue("properties");
		
		context.removeService(this.getReference());
		Set<ServiceListener> listeners = context.getFilteredListeners(properties);
		for (ServiceListener serviceListener : listeners) {
			serviceListener.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, ref));
		}
	}
}
