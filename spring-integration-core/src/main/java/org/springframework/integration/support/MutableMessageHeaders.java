/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.integration.support;

import java.util.Map;

import org.springframework.messaging.MessageHeaders;


/**
 * A MessageHeaders that permits direct access to and modification of the
 * header map.
 *
 * @author Stuart Williams
 * @author David Turanski
 * @since 4.2
 */
public class MutableMessageHeaders extends MessageHeaders {

	private static final long serialVersionUID = 3084692953798643018L;

	public MutableMessageHeaders(Map<String, Object> headers) {
		super(headers);
	}

	@Override
	protected Map<String, Object> getRawHeaders() {
		return super.getRawHeaders();
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> map) {
		super.getRawHeaders().putAll(map);
	}

	@Override
	public Object put(String key, Object value) {
		return super.getRawHeaders().put(key, value);
	}

	@Override
	public void clear() {
		super.getRawHeaders().clear();
	}

	@Override
	public Object remove(Object key) {
		return super.getRawHeaders().remove(key);
	}

}
