/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 *
 */
public class FtpParserInboundTests {
	@Before
	public void prepare(){
		new File("target/foo").delete();
	}

	@Test
	public void testLocalFilesAutoCreationTrue() throws Exception{
		assertTrue(!new File("target/foo").exists());
		new ClassPathXmlApplicationContext("FtpParserInboundTests-context.xml", this.getClass());
		assertTrue(new File("target/foo").exists());
		assertTrue(!new File("target/bar").exists());
	}
	@Test(expected=BeanCreationException.class)
	public void testLocalFilesAutoCreationFalse() throws Exception{
		assertTrue(!new File("target/bar").exists());
		new ClassPathXmlApplicationContext("FtpParserInboundTests-fail-context.xml", this.getClass());
	}

	@After
	public void cleanUp() throws Exception{
		new File("target/foo").delete();
	}
}