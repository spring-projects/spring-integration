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

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.ftp.FtpSource;

/**
 * @author Mark Fisher
 */
public class FtpSourceParserTests {

	@Test
	public void testFtpSourceAdapterParser() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSource");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
	}

}
