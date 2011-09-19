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
package org.springframework.integration.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.aggregator.CompletionStrategy;

/**
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class CompletionStrategyFactoryBean implements FactoryBean<CompletionStrategy> {
	
	private volatile CompletionStrategy delegate;
	
	public CompletionStrategyFactoryBean(Object target) {
		if (target instanceof CompletionStrategy){
			this.delegate = (CompletionStrategy) target;
		}
		else {
			throw new IllegalArgumentException("'target' is not an instance of the CompletionStrategy");
		}
	}

	
	public CompletionStrategy getObject() throws Exception {
		return this.delegate;
	}

	public Class<?> getObjectType() {
		return CompletionStrategy.class;
	}

	public boolean isSingleton() {
		return true;
	}

}
