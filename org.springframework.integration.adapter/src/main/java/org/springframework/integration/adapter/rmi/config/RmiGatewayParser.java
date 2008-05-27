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

package org.springframework.integration.adapter.rmi.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.adapter.config.AbstractGatewayParser;
import org.springframework.integration.adapter.rmi.RmiGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;rmi-gateway/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class RmiGatewayParser extends AbstractGatewayParser {

	private static final String REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE = "remote-invocation-executor";


	@Override
	protected Class<?> getBeanClass(Element element) {
		return RmiGateway.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals(REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE)
				&& super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		String executorRef = element.getAttribute(REMOTE_INVOCATION_EXECUTOR_ATTRIBUTE);
		if (StringUtils.hasText(executorRef)) {
			builder.addPropertyReference("remoteInvocationExecutor", executorRef);
		}
	}

}
