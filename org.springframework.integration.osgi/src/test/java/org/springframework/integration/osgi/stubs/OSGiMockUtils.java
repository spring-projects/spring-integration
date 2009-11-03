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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.osgi.framework.ServiceReference;
import org.springframework.osgi.util.internal.MapBasedDictionary;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class OSGiMockUtils {

	public static Dictionary parseFilterIntoDictionary(String filter){
		Dictionary properties = new MapBasedDictionary();
		String filterToEvaluate = null;
		if (filter != null){
			StringTokenizer tokenizer = new StringTokenizer(filter, "()&!");
			while(tokenizer.hasMoreTokens()){
				String nextToken = tokenizer.nextToken();
				String name = nextToken.substring(0, nextToken.indexOf("="));
				String value = nextToken.substring(nextToken.indexOf("=")+1);
				properties.put(name, value);
			}
		}	
		return properties;
	}
	/**
	 * 
	 * @param originalFilter
	 * @param valueToAdd
	 */
	public static String addToFilter(String originalFilter, String valueToAdd) {
		MapBasedDictionary original = new MapBasedDictionary(OSGiMockUtils.parseFilterIntoDictionary(originalFilter));
		Dictionary added = OSGiMockUtils.parseFilterIntoDictionary(valueToAdd);
		original.putAll(added);
		StringBuffer newFilter = new StringBuffer("(&");
		Enumeration emum = original.keys();
		while (emum.hasMoreElements()) {
			Object key = (Object) emum.nextElement();
			Object value = original.get(key);
			newFilter.append("(");
			newFilter.append(key);
			newFilter.append("=");
			newFilter.append(value);
			newFilter.append(")");
		}
		newFilter.append(")");
		return newFilter.toString();
	}
	/**
	 * 
	 * @param allServiceReferences
	 * @param filter
	 * @return
	 */
	public static ServiceReference[] buildFilteredServiceReferences(Set<ServiceReference> allServiceReferences, String filter){
		Dictionary properties = OSGiMockUtils.parseFilterIntoDictionary(filter);
		ArrayList<ServiceReference> filteredReferences = new ArrayList<ServiceReference>();
		
		for (ServiceReference sr : allServiceReferences) {	
			Enumeration keys = properties.keys();
			boolean match = true;
			inner:
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				Object value = properties.get(key);
				Object compareToValue = sr.getProperty(key);
				if (compareToValue != null && compareToValue instanceof String[] && key.equals("objectClass")){
					List<String> srImplementedClasses  = Arrays.asList((String[])compareToValue);
					List<String> filteredClasses  = Arrays.asList(StringUtils.commaDelimitedListToStringArray((String)value));
					match = srImplementedClasses.containsAll(filteredClasses);
					if (!match){
						break inner;
					}
				} else if (!value.equals(compareToValue)){
					match = false;
					break inner;
				}
			}
			if (match){
				filteredReferences.add(sr);
			}
		}
		ServiceReference[] references = null;
		if (filteredReferences.size() > 0){
			references = new ServiceReference[filteredReferences.size()];
			for (int i = 0; i < filteredReferences.size(); i++) {
				references[i] = filteredReferences.get(i);
			}
		}		
		return references;
	}
	
}
