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

/**
 * Exception that provides a response status code. This can be used by
 * {@link RequestMapper} implementations to indicate an error.
 * 
 * @author Mark Fisher
 */
@SuppressWarnings("serial")
public class ResponseStatusCodeException extends Exception {

	private final int statusCode;


	public ResponseStatusCodeException(int statusCode) {
		this.statusCode = statusCode;
	}


	public int getStatusCode() {
		return this.statusCode;
	}

}
