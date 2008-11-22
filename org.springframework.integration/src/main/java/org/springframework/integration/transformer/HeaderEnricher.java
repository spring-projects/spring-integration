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

package org.springframework.integration.transformer;

import java.util.Map;

import org.springframework.util.Assert;

/**
 * A Transformer that adds statically configured header values to a Message.
 * Accepts the boolean 'overwrite' property that specifies whether values
 * should be overwritten. By default, any existing header values for
 * a given key, will <em>not</em> be replaced.
 * 
 * @author Mark Fisher
 */
public class HeaderEnricher extends AbstractHeaderTransformer {

	private final Map<String, Object> headersToAdd;

	private volatile boolean overwrite;


	/**
	 * Create a HeaderEnricher with the given map of headers.
	 */
	public HeaderEnricher(Map<String, Object> headersToAdd) {
		Assert.notNull(headersToAdd, "headersToAdd must not be null");
		this.headersToAdd = headersToAdd;
	}


	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	@Override
	protected final void transformHeaders(Map<String, Object> headers) {
		for (Map.Entry<String, Object> entry : this.headersToAdd.entrySet()) {
			String key = entry.getKey();
			if (this.overwrite || headers.get(key) == null) {
				headers.put(key, entry.getValue());
			}
		}
	}

}
