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

package org.springframework.integration.ws.config;

import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

/**
 * @author Mark Fisher
 */
public class StubClientInterceptor implements ClientInterceptor {

	public boolean handleRequest(MessageContext context) throws WebServiceClientException {
		return true;
	}

	public boolean handleResponse(MessageContext context) throws WebServiceClientException {
		return true;
	}

	public boolean handleFault(MessageContext context) throws WebServiceClientException {
		return false;
	}

	public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {

	}

}
