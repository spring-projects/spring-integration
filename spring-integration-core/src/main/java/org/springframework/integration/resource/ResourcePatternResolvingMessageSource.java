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
package org.springframework.integration.resource;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.MessagingException;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.util.AcceptOnceUntilPurgedListFilter;
import org.springframework.integration.util.ListFilter;
import org.springframework.util.Assert;

/**
 * Implementation of {@link MessageSource} based on {@link ResourcePatternResolver} which will 
 * attempt to resolve {@link Resource}s based on the pattern specified.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class ResourcePatternResolvingMessageSource extends AbstractMessageSource<Resource> implements ApplicationContextAware, InitializingBean {

	private volatile String pattern;
	
	private volatile ApplicationContext applicationContext;
	
	private volatile ResourcePatternResolver patternResolver;
	
	private volatile ListFilter<Resource> filter = new AcceptOnceUntilPurgedListFilter<Resource>();

	private final Queue<Resource> queuedResources = new ConcurrentLinkedQueue<Resource>();

	public void setPatternResolver(ResourcePatternResolver patternResolver) {
		this.patternResolver = patternResolver;
	}
	
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public void setFilter(ListFilter<Resource> filter) {
		this.filter = filter;
	}
	
	@Override
	protected Resource doReceive() {
		
		Resource resource = queuedResources.poll();
		if (resource == null){
			this.synchronize();
			resource = queuedResources.poll();
		}
	
		return resource;
	}
	
	private void synchronize(){
		try {
			Resource[] resources = this.patternResolver.getResources(this.pattern);
			if (resources != null){
				List<Resource> filteredResources = this.filter.filter(Arrays.asList(resources));
				if (filteredResources != null){
					this.queuedResources.addAll(filteredResources);
				}	
			}	
		} catch (Exception e) {
			throw new MessagingException("Attempt to retrieve Resources failed", e);
		}
		 
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void afterPropertiesSet() throws Exception {
		if (this.patternResolver == null){
			if (this.applicationContext instanceof ResourcePatternResolver){
				this.patternResolver = this.applicationContext;
			}
		}
		Assert.notNull(this.patternResolver, "no 'patternResolver' is specified");
		Assert.hasText(this.pattern, "'pattern' must be specified");
	}

}
