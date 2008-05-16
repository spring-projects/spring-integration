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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.handler.MethodInvokingTarget;
import org.springframework.integration.message.MethodInvokingSource;
import org.springframework.util.StringUtils;

/**
 * Parser for &lt;source-adapter&gt; and &lt;target-adapter&gt;.
 * Creates a {@link MethodInvokingSource} or {@link MethodInvokingTarget}.
 * 
 * @author Mark Fisher
 */
public class MethodInvokingAdapterParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return "source-adapter".equals(element.getLocalName()) ? MethodInvokingSource.class : MethodInvokingTarget.class;
	}

	@Override
	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute("ref");
		String method = element.getAttribute("method");
		if (!StringUtils.hasText(ref)) {
			throw new ConfigurationException("The 'ref' attribute is required.");
		}
		if (!StringUtils.hasText(method)) {
			throw new ConfigurationException("The 'method' attribute is required.");
		}
		builder.addPropertyReference("object", ref);
		builder.addPropertyValue("method", method);
	}

}
