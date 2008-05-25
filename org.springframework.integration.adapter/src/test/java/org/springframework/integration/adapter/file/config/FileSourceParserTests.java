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
import org.springframework.integration.adapter.file.FileSource;
import org.springframework.integration.adapter.file.TextFileMessageCreator;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class FileSourceParserTests {

	@Test
	public void testFileSourceDefaultType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSourceDefault");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		Object messageCreator =  sourceAccessor.getPropertyValue("messageCreator");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
		assertTrue(messageCreator instanceof FileMessageCreator);
	}

	@Test
	public void testFileSourceTextType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSourceText");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		Object messageCreator =  sourceAccessor.getPropertyValue("messageCreator");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
		assertTrue(messageCreator instanceof TextFileMessageCreator);
	}

	@Test
	public void testFileSourceBinaryType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSourceBinary");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		Object messageCreator =  sourceAccessor.getPropertyValue("messageCreator");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
		assertTrue(messageCreator instanceof ByteArrayFileMessageCreator);
	}

	@Test
	public void testFileSourceFileType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSourceFile");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		Object messageCreator =  sourceAccessor.getPropertyValue("messageCreator");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
		assertTrue(messageCreator instanceof FileMessageCreator);
	}

	@Test
	public void testFileSourceCustomType() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSourceCustom");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		Object messageCreator =  sourceAccessor.getPropertyValue("messageCreator");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
		assertTrue(messageCreator instanceof CustomMessageCreator);
	}

	@Test
	public void testInvalidFileSource() {
		try {
			new ClassPathXmlApplicationContext("invalidFileSourceTests.xml", this.getClass());
			fail();
		} catch (BeanDefinitionStoreException e) {
			assertTrue(e.getCause() instanceof ConfigurationException);
		}
	}

}
