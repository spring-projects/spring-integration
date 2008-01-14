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

package org.springframework.integration.endpoint;

import org.springframework.util.Assert;

/**
 * Metadata for configuring a pool of concurrent threads.
 * 
 * @author Mark Fisher
 */
public class ConcurrencyPolicy implements EndpointPolicy {

	private int coreConcurrency;

	private int maxConcurrency;


	public int getCoreConcurrency() {
		return this.coreConcurrency;
	}

	public void setCoreConcurrency(int coreConcurrency) {
		Assert.isTrue(coreConcurrency > 0, "'coreConcurrency' must be at least 1");
		this.coreConcurrency = coreConcurrency;
	}

	public int getMaxConcurrency() {
		return this.maxConcurrency;
	}

	public void setMaxConcurrency(int maxConcurrency) {
		Assert.isTrue(maxConcurrency > 0, "'maxConcurrency' must be at least 1");
		this.maxConcurrency = maxConcurrency;
	}

}
