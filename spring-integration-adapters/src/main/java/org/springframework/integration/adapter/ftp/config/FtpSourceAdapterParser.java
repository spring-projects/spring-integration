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

package org.springframework.integration.adapter.ftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.core.Conventions;
import org.springframework.integration.adapter.ftp.FtpSourceAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;ftp-source/&gt; element.
 * 
 * @author Marius Bogoevici
 */
public class FtpSourceAdapterParser extends AbstractSimpleBeanDefinitionParser {

	private static final String CHANNEL_ATTRIBUTE = "channel";


	protected Class<?> getBeanClass(Element element) {
		return FtpSourceAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {	
		return !CHANNEL_ATTRIBUTE.equals(attributeName) && super.isEligibleAttribute(attributeName);
	}

	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		String channelRef = element.getAttribute(CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(channelRef)) {
			beanDefinition.addPropertyReference(
					Conventions.attributeNameToPropertyName(CHANNEL_ATTRIBUTE), channelRef);
		}
	}

}
