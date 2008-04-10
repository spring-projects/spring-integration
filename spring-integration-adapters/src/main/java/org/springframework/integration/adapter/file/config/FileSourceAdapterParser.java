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

package org.springframework.integration.adapter.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.adapter.file.FileSourceAdapter;

/**
 * Parser for the &lt;file-source/&gt; element.
 * 
 * @author Mark Fisher
 */
public class FileSourceAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return FileSourceAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String directory = element.getAttribute("directory");
		String channel = element.getAttribute("channel");
		String pollPeriod = element.getAttribute("poll-period");
		builder.addConstructorArgValue(directory);
		builder.addPropertyReference("channel", channel);
		builder.addPropertyValue("period", pollPeriod);
	}

}
