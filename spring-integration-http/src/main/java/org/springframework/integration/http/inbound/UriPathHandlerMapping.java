/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.http.inbound;

import java.util.Collections;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.handler.AbstractDetectingUrlHandlerMapping;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class UriPathHandlerMapping extends AbstractDetectingUrlHandlerMapping {
	
	public UriPathHandlerMapping(){
		this.setOrder(Integer.MIN_VALUE);
	}
	
	protected void detectHandlers() throws BeansException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for URL mappings in application context: " + getApplicationContext());
		}
		
		String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(getApplicationContext(), HttpRequestHandlingEndpointSupport.class);
	
		for (String beanName : beanNames) {
			String[] urls = determineUrlsForHandler(beanName);
			if (!ObjectUtils.isEmpty(urls)) {
				// URL paths found: Let's consider it a handler.
				registerHandler(urls, beanName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Rejected bean name '" + beanName + "': no URL paths identified");
				}
			}
		}
	}

	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		ApplicationContext context = this.getApplicationContext();
		HttpRequestHandlingEndpointSupport handler = context.getBean(beanName, HttpRequestHandlingEndpointSupport.class);
		String path = handler.getPath();
		return Collections.singletonList(path).toArray(new String[]{});
	}
}
