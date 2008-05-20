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

import java.io.File;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.file.FileSource;

/**
 * @author Mark Fisher
 */
public class FileSourceParserTests {

	@Test
	public void testFileSource() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceParserTests.xml", this.getClass());
		FileSource fileSource = (FileSource) context.getBean("fileSource");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(fileSource);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
	}

}
