/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.ftp.config;

import org.apache.commons.net.ftp.FTP;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import java.util.HashMap;
import java.util.Map;


/**
 * Provides namespace support for using FTP
 * <p/>
 * This is *heavily* influenced by the good work done by Iwein before.
 *
 * @author Josh Long
 */
@SuppressWarnings("unused")
public class FtpNamespaceHandler extends NamespaceHandlerSupport {
	static public Map<String, Integer> FILE_TYPES = new HashMap<String, Integer>();
	static public Map<String, Integer> CLIENT_MODES = new HashMap<String, Integer>();

	static {
		// file types
		FILE_TYPES.put("ebcdic-file-type" , FTP.EBCDIC_FILE_TYPE);
		FILE_TYPES.put("ascii-file-type" , FTP.ASCII_FILE_TYPE);
		FILE_TYPES.put("binary-file-type" , FTP.BINARY_FILE_TYPE);

		// client modes
		CLIENT_MODES.put("active-local-data-connection-mode", 0);
		CLIENT_MODES.put("active-remote-data-connection-mode", 1);
		CLIENT_MODES.put("passive-local-data-connection-mode", 2);
		CLIENT_MODES.put("passive-remote-data-connection-mode", 3);
	}

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FtpMessageSourceBeanDefinitionParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new FtpMessageSendingConsumerBeanDefinitionParser());
	}
}
