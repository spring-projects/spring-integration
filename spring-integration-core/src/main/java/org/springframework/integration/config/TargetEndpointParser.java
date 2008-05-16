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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.handler.MethodInvokingTarget;

/**
 * Parser for the &lt;target-endpoint&gt; element.
 * 
 * @author Mark Fisher
 */
public class TargetEndpointParser extends AbstractTargetEndpointParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return TargetEndpoint.class;
	}

	@Override
	protected String getTargetAttributeName() {
		return "target";
	}

	@Override
	protected Class<?> getAdapterClass() {
		return MethodInvokingTarget.class;
	}

}
