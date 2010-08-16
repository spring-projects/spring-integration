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

package org.springframework.integration.http;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.CollectionUtils;

/**
 * Default {@link HeaderMapper} implementation for HTTP.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class DefaultHttpHeaderMapper implements HeaderMapper<HttpHeaders> {

	public static final String OUTBOUND_PREFIX = "X-";


	private volatile String[] outboundHeaderNames = new String[0];

	private volatile String[] inboundHeaderNames = new String[0];


	public void setOutboundHeaderNames(String[] outboundHeaderNames) {
		this.outboundHeaderNames = (outboundHeaderNames != null) ? outboundHeaderNames : new String[0];
	}

	public void setInboundHeaderNames(String[] inboundHeaderNames) {
		this.inboundHeaderNames = (inboundHeaderNames != null) ? inboundHeaderNames : new String[0];
	}

	public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
		for (String name : this.outboundHeaderNames) {
			Object value = headers.get(name);
			String prefixedName = OUTBOUND_PREFIX + name;
			if (value instanceof String) {
				target.add(prefixedName, (String) value);
			}
			else if (value instanceof String[]) {
				for (String next : (String[]) value) {
					target.add(prefixedName, next);
				}
			}
			else if (value instanceof Iterable<?>) {
				for (Object next : (Iterable<?>) value) {
					if (next instanceof String) {
						target.add(prefixedName, (String) next);
					}
				}
			}
		}
	}

	public Map<String, ?> toHeaders(HttpHeaders source) {
		Map<String, Object> target = new HashMap<String, Object>();
		for (String name : this.inboundHeaderNames) {
			List<String> values = source.get(name);
			if (!CollectionUtils.isEmpty(values)) {
				if (values.size() == 1) {
					target.put(name, values.get(0));
				}
				else {
					target.put(name, values);
				}
			}
		}
		return target;
	}

}
