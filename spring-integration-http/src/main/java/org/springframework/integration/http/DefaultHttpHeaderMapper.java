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

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.message.HeaderMapper;

/**
 * Default {@link HeaderMapper} implementation for HTTP.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class DefaultHttpHeaderMapper implements HeaderMapper<HttpHeaders> {

	public void fromHeaders(MessageHeaders headers, HttpHeaders target) {
		for (String name : headers.keySet()) {
			if (!name.startsWith(MessageHeaders.PREFIX)) {
				Object value = headers.get(name);
				if (value instanceof String) {
					target.add(name, (String) value);
				}
				else if (value instanceof String[]) {
					for (String next : (String[]) value) {
						target.add(name, next);
					}
				}
				else if (value instanceof Iterable<?>) {
					for (Object next : (Iterable<?>) value) {
						if (next instanceof String) {
							target.add(name, (String) next);
						}
					}
				}
			}
		}
	}

	public Map<String, ?> toHeaders(HttpHeaders source) {
		return source;
	}

}
