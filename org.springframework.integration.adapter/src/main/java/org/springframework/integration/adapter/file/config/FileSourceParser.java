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

import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.file.FileSource;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;file-source/&gt; element.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSourceParser extends AbstractDirectorySourceParser {

	public static final String DIRECTORY_ATTRIBUTE = "directory";

	public static final String FILE_FILTER_ATTRIBUTE = "file-filter";

	public static final String FILENAME_FILTER_ATTRIBUTE = "filename-filter";


	public FileSourceParser() {
		super(false);
	}


	@Override
	protected Class<?> getBeanClass(Element element) {
		return FileSource.class;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !DIRECTORY_ATTRIBUTE.equals(attributeName) &&
				!FILE_FILTER_ATTRIBUTE.equals(attributeName) &&
				!FILENAME_FILTER_ATTRIBUTE.equals(attributeName) &&
				super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		String directoryLocation = element.getAttribute(DIRECTORY_ATTRIBUTE);
		if (!directoryLocation.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)
				&& !isUrl(directoryLocation)) {
			directoryLocation = "file:" + directoryLocation;
		}
		beanDefinition.addConstructorArgValue(directoryLocation);
		String fileFilter = element.getAttribute(FILE_FILTER_ATTRIBUTE);
		String filenameFilter = element.getAttribute(FILENAME_FILTER_ATTRIBUTE);
		if (StringUtils.hasText(fileFilter) && StringUtils.hasText(filenameFilter)) {
			throw new ConfigurationException("FileSource does not support both '" +
					FILE_FILTER_ATTRIBUTE + "' and '" + FILENAME_FILTER_ATTRIBUTE + "'.");
		}
		else if (StringUtils.hasText(fileFilter)) {
			beanDefinition.addPropertyReference("fileFilter", fileFilter);
		}
		else if (StringUtils.hasText(filenameFilter)) {
			beanDefinition.addPropertyReference("filenameFilter", filenameFilter);
		}
		super.postProcess(beanDefinition, element);
	}

	private boolean isUrl(String directoryLocation) {
		try {
			new URL(directoryLocation);
			return true;
		}
		catch (MalformedURLException e) {
			return false;
		}
	}

}
