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

package org.springframework.integration.ftp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.message.DefaultMessageCreator;

/**
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
public class FtpInboundChannelAdapterParserTests {

	@Test
	public void ftpSourceWithDefaultMessageCreator() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"ftpInboundChannelAdapterParserTests.xml", this.getClass());
		Object adapter = context.getBean("default.adapter");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(
				new DirectFieldAccessor(adapter).getPropertyValue("source"));
		DirectFieldAccessor poolAccessor = new DirectFieldAccessor(
				sourceAccessor.getPropertyValue("clientPool"));
		assertEquals("testHost", poolAccessor.getPropertyValue("host"));
		assertEquals(2121, poolAccessor.getPropertyValue("port"));
		assertEquals(new File("/local"), sourceAccessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", poolAccessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", poolAccessor.getPropertyValue("username"));
		assertEquals("testPassword", poolAccessor.getPropertyValue("password"));
		Object messageCreator = sourceAccessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof DefaultMessageCreator);
	}

	@Test
	public void ftpSourceWithCustomMessageCreator() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"ftpInboundChannelAdapterParserTests.xml", this.getClass());
		Object adapter = context.getBean("custom.adapter");
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(
				new DirectFieldAccessor(adapter).getPropertyValue("source"));
		DirectFieldAccessor poolAccessor = new DirectFieldAccessor(
				sourceAccessor.getPropertyValue("clientPool"));
		assertEquals("testHost", poolAccessor.getPropertyValue("host"));
		assertEquals(2121, poolAccessor.getPropertyValue("port"));
		assertEquals(new File("/local"), sourceAccessor.getPropertyValue("localWorkingDirectory"));
		assertEquals("/remote", poolAccessor.getPropertyValue("remoteWorkingDirectory"));
		assertEquals("testUser", poolAccessor.getPropertyValue("username"));
		assertEquals("testPassword", poolAccessor.getPropertyValue("password"));
		Object messageCreator = sourceAccessor.getPropertyValue("messageCreator");
		assertTrue(messageCreator instanceof CustomMessageCreator);
	}

}
