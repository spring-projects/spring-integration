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
package org.springframework.integration.monitor;

import java.util.Map;

/**
 * Locator interface for mapping bean names to JMX object names.
 * 
 * @author Dave Syer
 * 
 * @since 2.0
 * 
 */
public interface ObjectNameLocator {

	/**
	 * @param beanName the bean name to query
	 * @return a String representation of the corresponding JMX object name (or null if there is none)
	 */
	String getObjectName(String beanName);

	/**
	 * @return a map of all the known bean and object names
	 */
	Map<String, String> getObjectNames();

}