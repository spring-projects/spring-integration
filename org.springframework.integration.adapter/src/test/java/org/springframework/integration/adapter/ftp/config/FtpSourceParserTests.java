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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.file.ByteArrayFileMessageCreator;
import org.springframework.integration.adapter.file.FileMessageCreator;
import org.springframework.integration.adapter.file.TextFileMessageCreator;
import org.springframework.integration.adapter.file.config.CustomMessageCreator;
import org.springframework.integration.adapter.ftp.FtpSource;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FtpSourceParserTests {

	@Test
	public void testFtpSourceAdapterParser() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSourceDefault");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
		Object messageCreator = accessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof FileMessageCreator);
		DirectFieldAccessor messageCreatorAccessor = new DirectFieldAccessor(messageCreator);
		assertEquals(messageCreatorAccessor.getPropertyValue("deleteFileAfterCreation"), false);
	}

	@Test
	public void testFtpSourceTextType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSourceText");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
		Object messageCreator = accessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof TextFileMessageCreator);
		DirectFieldAccessor messageCreatorAccessor = new DirectFieldAccessor(messageCreator);
		assertEquals(messageCreatorAccessor.getPropertyValue("deleteFileAfterCreation"), true);
	}

	@Test
	public void testFtpSourceBinaryType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSourceBinary");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
		Object messageCreator = accessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof ByteArrayFileMessageCreator);
		DirectFieldAccessor messageCreatorAccessor = new DirectFieldAccessor(messageCreator);
		assertEquals(messageCreatorAccessor.getPropertyValue("deleteFileAfterCreation"), true);
	}

	@Test
	public void testFtpSourceFileType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSourceFile");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
		Object messageCreator = accessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof FileMessageCreator);
		DirectFieldAccessor messageCreatorAccessor = new DirectFieldAccessor(messageCreator);
		assertEquals(messageCreatorAccessor.getPropertyValue("deleteFileAfterCreation"), false);
	}

	@Test
	public void testFtpSourceCustomType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceParserTests.xml", this.getClass());
		FtpSource ftpSource = (FtpSource) context.getBean("ftpSourceCustom");
		DirectFieldAccessor accessor = new DirectFieldAccessor(ftpSource);
		assertEquals("testHost", accessor.getPropertyValue("host"));
		assertEquals(2121, accessor.getPropertyValue("port"));
		assertEquals(new File("/local"), accessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", accessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", accessor.getPropertyValue("username"));
		assertEquals("testPassword", accessor.getPropertyValue("password"));
		Object messageCreator = accessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof CustomMessageCreator);
		// not testing for deleteFileAfterCreation - this is completely left up
		// to the implementation
	}

	@Test
	public void testInvalidFtpSource() {
		try {
			ApplicationContext context = new ClassPathXmlApplicationContext("invalidFtpSourceTests.xml", this
					.getClass());
			fail();
		}
		catch (BeanDefinitionStoreException e) {
			assertTrue(e.getCause() instanceof ConfigurationException);
		}
	}

}
