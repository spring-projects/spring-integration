/*
 * Copyright 2002-2009 the original author or authors.
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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Representation of an HTTP response as returned by an implementation of
 * the {@link HttpRequestExecutor} strategy.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public interface HttpResponse {

	/**
	 * Return all response headers as a map. There may be multiple values per
	 * key in the map. Hence, the value type is a List of Strings.
	 */
	Map<String, List<String>> getHeaders();

	/**
	 * Return all header values for a given key, or null if it has no values.
	 */
	List<String> getHeaders(String key);

	/**
	 * Return the first header value for a given key, or null if it has no values.
	 */
	String getFirstHeader(String key);

	/**
	 * Return the body of the response as an InputStream.
	 */
	InputStream getBody();

}
