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

package org.springframework.integration.ftp;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * This simple example demonstrates sending a file to a remote FTP server using the ftp:outbound-channel-adapter
 * <p/>
 * It reads files from a directory on your computer and systematically puts them on the remote FTP server,
 *
 * @author Josh Long
 */
public class OutboundFtpExample {

	public static void main(String[] args) throws Throwable {
		new ClassPathXmlApplicationContext("outbound-ftp-context.xml");
	}

}
