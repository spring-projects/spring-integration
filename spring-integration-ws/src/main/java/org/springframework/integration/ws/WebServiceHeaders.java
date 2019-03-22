/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.ws;

/**
 * Pre-defined header names to be used when storing or retrieving
 * Web Service properties to/from integration Message Headers.
 *
 * @author Mark Fisher
 */
public abstract class WebServiceHeaders {

	public static final String PREFIX = "ws_";

	public static final String SOAP_ACTION = PREFIX + "soapAction";

}
