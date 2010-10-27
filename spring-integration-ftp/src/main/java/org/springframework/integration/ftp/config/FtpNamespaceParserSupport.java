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

package org.springframework.integration.ftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * General support for parsers in the FTP namespace.
 *
 * @author Josh Long
 */
public class FtpNamespaceParserSupport {

	/**
	 * Handles values that are supported across all adapters.
	 * @param builder	   a builder
	 * @param element	   an element
	 * @param parserContext a parser context
	 */
	public static void configureCoreFtpClient(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		for (String p : "auto-create-directories,username,port,password,host,remote-directory".split(",")) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, p);
		}
		if (element.hasAttribute("file-type")) {
			int fileType = FtpNamespaceHandler.FILE_TYPES.get(element.getAttribute("file-type"));
			builder.addPropertyValue("fileType", fileType);
		}
		if (element.hasAttribute("client-mode")) {
			int clientMode = FtpNamespaceHandler.CLIENT_MODES.get(element.getAttribute("client-mode"));
			builder.addPropertyValue("clientMode", clientMode);
		}
	}

}
