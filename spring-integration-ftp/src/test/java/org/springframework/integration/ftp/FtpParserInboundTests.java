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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.MessagingException;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
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
	@Test
	public void testLocalFilesAutoCreationFalse() throws Exception{
		assertTrue(!new File("target/bar").exists());
		try {
			new ClassPathXmlApplicationContext("FtpParserInboundTests-fail-context.xml", this.getClass());
			fail("BeansException expected.");
		}
		catch (BeansException e) {
			assertThat(e, Matchers.instanceOf(BeanCreationException.class));
			Throwable cause = e.getCause();
			assertThat(cause, Matchers.instanceOf(MessagingException.class));
			cause = cause.getCause();
			assertThat(cause, Matchers.instanceOf(FileNotFoundException.class));
			assertEquals("bar", cause.getMessage());
		}
	}

	@After
	public void cleanUp() throws Exception{
		new File("target/foo").delete();
	}
}
