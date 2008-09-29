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

package org.springframework.integration.adapter.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.Assert;

/**
 * Base class for url-based remoting outbound gateway parsers. 
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRemotingOutboundGatewayParser extends AbstractRemotingGatewayParser {

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals("url") && super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		String url = element.getAttribute("url");
		Assert.hasText(url, "The 'url' attribute is required.");
		builder.addConstructorArgValue(url);
	}

}
