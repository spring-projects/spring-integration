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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mockito.Mockito;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.mock.MockBundleContext;
import org.springframework.osgi.mock.MockServiceReference;
import org.springframework.osgi.util.internal.MapBasedDictionary;

/**
 * Mock BundleContext to be used for testing
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SIBundleContextStub extends MockBundleContext {
	private static final Log log = LogFactory.getLog(SIBundleContextStub.class);
	private static  SIBundleContextStub bundleContext = new SIBundleContextStub();
	
	private Map<ServiceReference, Object> services = new HashMap<ServiceReference, Object>();
	

	private Set<ServiceReference> serviceReferences = new HashSet<ServiceReference>();
	private Map<MapBasedDictionary, ServiceListener> serviceListenerMap = 
							new HashMap<MapBasedDictionary, ServiceListener>();
	
	
	/**
	 * 
	 * @return
	 */
	public static SIBundleContextStub getInstance(){	
		log.debug("Returning Stubed BundleContext");
		return bundleContext;
	}
	/**
	 * 
	 * @return
	 */
	public static SIBundleContextStub getNewInstance(){	
		log.debug("Returning newly created Stubed BundleContext");
		return new SIBundleContextStub();
	}
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
		if (properties instanceof Hashtable){
			properties = new MapBasedDictionary(properties);
		}
		log.info("Registering SERVICE: " + service);
		SIServiceRegistrationStub reg = new SIServiceRegistrationStub(clazzes, properties);
		reg.setBundleContext(this);

		MockServiceReference ref = new MockServiceReference(this.getBundle(), properties, reg, clazzes);
		for (int i = 0; i < clazzes.length; i++) {
			serviceReferences.add(ref);
		}
		if (service instanceof ServiceFactory){
			service = ((ServiceFactory)service).getService(this.getBundle(), reg);
		}
		reg.setReference(ref);
		services.put(ref, service);
		final ServiceEvent event = new ServiceEvent(ServiceEvent.REGISTERED, ref);
		final Set<ServiceListener> listeners = this.getFilteredListeners(properties);
		for (final ServiceListener listener : listeners) {
//			Thread t = new Thread(new Runnable() {
//				public void run() {
					listener.serviceChanged(event);
//				}
//			});
//			t.start();
		}
		log.info("Registered SERVICE: " + ref);
		return reg;
	}
	/**
	 * 
	 */
	public Object getService(ServiceReference sr){
		return services.get(sr);
	}
	/**
	 * 
	 */
	public ServiceReference getServiceReference(String serviceName){
		String filter = "(&(objectClass=" + serviceName + "))";
		ServiceReference[] references = OSGiMockUtils.buildFilteredServiceReferences(serviceReferences, filter);
		return references == null ? null : references[0];
	}
	/**
	 * 
	 */
	public ServiceReference[] getServiceReferences(String serviceName, String filter) throws InvalidSyntaxException {
		if (serviceName != null){
			String classFilter = "(&(objectClass=" + serviceName + "))";
			filter = OSGiMockUtils.addToFilter(filter, classFilter);
		}	
		ServiceReference[] references = OSGiMockUtils.buildFilteredServiceReferences(serviceReferences, filter);
		return references;
	}
	/**
	 * 
	 */
	public void addServiceListener(ServiceListener listener) {
		MapBasedDictionary properties = new MapBasedDictionary();
		serviceListenerMap.put(properties, listener);
	}
	/**
	 * 
	 */
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
		MapBasedDictionary properties = (MapBasedDictionary) OSGiMockUtils.parseFilterIntoDictionary(filter);
		serviceListenerMap.put(properties, listener);
	}
	@SuppressWarnings("unchecked")
	public Set<ServiceListener> getFilteredListeners(Dictionary properties){
		Set filteredListeners = new HashSet<ServiceListener>();
		if (properties != null && properties.size() > 0){	
			for (Dictionary filter : serviceListenerMap.keySet()) {
				MapBasedDictionary listenerFilter = new MapBasedDictionary(filter);
				
				log.trace("Trying to match filter properties: " + listenerFilter);
				MapBasedDictionary inFilter = new MapBasedDictionary(properties);
				log.trace("Current filter entry: " + inFilter);
				boolean objecClassMatch = true;
				if (listenerFilter.containsKey("objectClass")){
					objecClassMatch = this.matchObjectClass(listenerFilter, inFilter);
				}
				listenerFilter.remove("objectClass");
				inFilter.remove("objectClass");
				if (inFilter.keySet().containsAll(listenerFilter.keySet()) &&
					inFilter.values().containsAll(listenerFilter.values()) && objecClassMatch){
					filteredListeners.add(serviceListenerMap.get(filter));
					log.debug("Found listener for properties: " + listenerFilter + " -  " + serviceListenerMap.get(filter));
				}
			}				
		}
		return filteredListeners;
	}
	
	public boolean matchObjectClass(MapBasedDictionary listenerFilter, MapBasedDictionary inFilter){
		String interfaze = (String) listenerFilter.get("objectClass");
		String[] interfaces = (String[]) inFilter.get("objectClass");
		
		return Arrays.binarySearch(interfaces, interfaze) >=0;
	}
	public Map<MapBasedDictionary, ServiceListener> getServiceListenerMap() {
		return serviceListenerMap;
	}
	public Map<ServiceReference, Object> getServices() {
		return services;
	}
}
