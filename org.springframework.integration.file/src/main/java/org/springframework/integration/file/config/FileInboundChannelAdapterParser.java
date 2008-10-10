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

package org.springframework.integration.file.config;

import java.util.regex.Pattern;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.file.AcceptOnceFileListFilter;
import org.springframework.integration.file.CompositeFileListFilter;
import org.springframework.integration.file.PatternMatchingFileListFilter;
import org.springframework.integration.file.PollableFileSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of the 'file' namespace.
 * 
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FileInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PollableFileSource.class);
		String directory = element.getAttribute("directory");
		if (StringUtils.hasText(directory)) {
			builder.addPropertyValue("inputDirectory", directory);
		}
		String filter = element.getAttribute("filter");
		if (StringUtils.hasText(filter)){
			builder.addPropertyReference("filter", filter);
		}
		String filenamePattern = element.getAttribute("filename-pattern");
		if (StringUtils.hasText(filenamePattern)) {
			Assert.isTrue(!StringUtils.hasText(filter),
					"at most one of 'filter' and 'filename-pattern' may be provided");
			AcceptOnceFileListFilter acceptOnceFilter = new AcceptOnceFileListFilter();
			Pattern pattern = Pattern.compile(filenamePattern);
			PatternMatchingFileListFilter patternFilter = new PatternMatchingFileListFilter(pattern);
			CompositeFileListFilter compositeFilter = new CompositeFileListFilter(acceptOnceFilter, patternFilter);
			builder.addPropertyValue("filter", compositeFilter);
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
