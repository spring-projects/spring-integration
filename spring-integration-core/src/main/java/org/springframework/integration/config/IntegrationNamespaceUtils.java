/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.util.StringUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 */
public abstract class IntegrationNamespaceUtils {

	private static final String CORE_SIZE_ATTRIBUTE = "core";

	private static final String MAX_SIZE_ATTRIBUTE = "max";

	private static final String QUEUE_CAPACITY_ATTRIBUTE = "queue-capacity";

	private static final String KEEP_ALIVE_ATTRIBUTE = "keep-alive";


	public static ConcurrencyPolicy parseConcurrencyPolicy(Element element) {
		ConcurrencyPolicy policy = new ConcurrencyPolicy();
		String coreSize = element.getAttribute(CORE_SIZE_ATTRIBUTE);
		String maxSize = element.getAttribute(MAX_SIZE_ATTRIBUTE);
		String queueCapacity = element.getAttribute(QUEUE_CAPACITY_ATTRIBUTE);
		String keepAlive = element.getAttribute(KEEP_ALIVE_ATTRIBUTE);
		if (StringUtils.hasText(coreSize)) {
			policy.setCoreSize(Integer.parseInt(coreSize));
		}
		if (StringUtils.hasText(maxSize)) {
			policy.setMaxSize(Integer.parseInt(maxSize));
		}
		if (StringUtils.hasText(queueCapacity)) {
			policy.setQueueCapacity(Integer.parseInt(queueCapacity));
		}
		if (StringUtils.hasText(keepAlive)) {
			policy.setKeepAliveSeconds(Integer.parseInt(keepAlive));
		}
		return policy;
	}

}
