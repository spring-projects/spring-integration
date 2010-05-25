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

import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * Representation of an HTTP request to be executed by an implementation of
 * the {@link HttpRequestExecutor} strategy.
 * 
 * @author Mark Fisher
 * @since 1.0.2
 */
public interface HttpRequest {

	/**
	 * Return the target URL for this request.
	 */
	URL getTargetUrl();

	/**
	 * Return the request method ("GET", "POST", etc).
	 */
	String getRequestMethod();

	/**
	 * Return the content type for requests.
	 */
	String getContentType();

	/**
	 * Return the content length if known, else <code>null</code>.
	 */
	Integer getContentLength();

	/**
	 * Return the request body as a {@link ByteArrayOutputStream},
	 * or <code>null</code> if this request has no body content.
	 */
	ByteArrayOutputStream getBody();

}
