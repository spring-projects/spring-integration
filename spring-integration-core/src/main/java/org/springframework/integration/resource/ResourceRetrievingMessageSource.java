/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.resource;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.util.CollectionFilter;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link org.springframework.integration.core.MessageSource} based on
 * {@link ResourcePatternResolver} which will attempt to resolve {@link Resource}s based
 * on the pattern specified.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class ResourceRetrievingMessageSource extends AbstractMessageSource<Resource[]>
		implements ApplicationContextAware {

	private final String pattern;

	private volatile ApplicationContext applicationContext;

	private volatile ResourcePatternResolver patternResolver;

	private volatile CollectionFilter<Resource> filter;

	public ResourceRetrievingMessageSource(String pattern) {
		Assert.hasText(pattern, "pattern must not be empty");
		this.pattern = pattern;
	}

	public void setPatternResolver(ResourcePatternResolver patternResolver) {
		this.patternResolver = patternResolver;
	}

	public void setFilter(CollectionFilter<Resource> filter) {
		this.filter = filter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public String getComponentType() {
		return "resource-inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		if (this.patternResolver == null) {
			this.patternResolver = this.applicationContext;
		}
		Assert.notNull(this.patternResolver, "no 'patternResolver' available");
	}

	@Override
	protected Resource[] doReceive() {
		try {
			Resource[] resources = this.patternResolver.getResources(this.pattern);
			if (ObjectUtils.isEmpty(resources)) {
				resources = null;
			}
			else if (this.filter != null) {
				Collection<Resource> filteredResources = this.filter.filter(Arrays.asList(resources));
				if (CollectionUtils.isEmpty(filteredResources)) {
					resources = null;
				}
				else {
					resources = filteredResources.toArray(new Resource[0]);
				}
			}
			return resources;
		}
		catch (Exception e) {
			throw new MessagingException("Attempt to retrieve Resources failed", e);
		}
	}

}
