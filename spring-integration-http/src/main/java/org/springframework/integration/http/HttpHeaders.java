/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.http;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.2
 */
public abstract class HttpHeaders {

	public static final String PREFIX = "http_";

	public static final String REQUEST_URL = PREFIX + "requestUrl";

	public static final String REQUEST_METHOD = PREFIX + "requestMethod";

	public static final String USER_PRINCIPAL = PREFIX + "userPrincipal";

	public static final String STATUS_CODE = PREFIX + "statusCode";

}
