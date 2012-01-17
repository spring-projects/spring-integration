/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.sftp.config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import java.io.File;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class InboundChannelAdapterParserTests {
	
	@Before
	public void prepare(){
		new File("foo").delete();
	}
	
	@Test
	public void testAutoStartup() throws Exception{
		ApplicationContext context =
			new ClassPathXmlApplicationContext("SftpInboundAutostartup-context.xml", this.getClass());
		
		SourcePollingChannelAdapter adapter = context.getBean("sftpAutoStartup", SourcePollingChannelAdapter.class);
		assertFalse(adapter.isRunning());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testWithLocalFiles() throws Exception{
		ApplicationContext context =
			new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		assertTrue(new File("src/main/resources").exists());
	
		Object adapter = context.getBean("sftpAdapterAutoCreate");
		assertTrue(adapter instanceof SourcePollingChannelAdapter);
		SftpInboundFileSynchronizingMessageSource source = 
			(SftpInboundFileSynchronizingMessageSource) TestUtils.getPropertyValue(adapter, "source");
		assertNotNull(source);
		Comparator<File> comparator = TestUtils.getPropertyValue(adapter, "source.fileSource.toBeReceived.q.comparator", Comparator.class);
		assertNotNull(comparator);
		SftpInboundFileSynchronizer synchronizer =  (SftpInboundFileSynchronizer) TestUtils.getPropertyValue(source, "synchronizer");
		assertNotNull(TestUtils.getPropertyValue(synchronizer, "localFilenameGeneratorExpression"));
		String remoteFileSeparator = (String) TestUtils.getPropertyValue(synchronizer, "remoteFileSeparator");
		assertEquals(".bar", TestUtils.getPropertyValue(synchronizer, "temporaryFileSuffix", String.class));
		assertNotNull(remoteFileSeparator);
		assertEquals(".", remoteFileSeparator);
		PollableChannel requestChannel = context.getBean("requestChannel", PollableChannel.class);
		assertNotNull(requestChannel.receive(2000));
	}

	@Test
	public void testAutoChannel() {
		ApplicationContext context =
			new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		// Auto-created channel
		MessageChannel autoChannel = context.getBean("autoChannel", MessageChannel.class);
		SourcePollingChannelAdapter autoChannelAdapter = context.getBean("autoChannel.adapter", SourcePollingChannelAdapter.class);
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}

	@Test(expected=BeanDefinitionStoreException.class)
	//exactly one of 'filename-pattern' or 'filter' is allowed on SFTP inbound adapter
	public void testFailWithFilePatternAndFilter() throws Exception{
		assertTrue(!new File("target/bar").exists());
		new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context-fail.xml", this.getClass());
	}

	@Test @Ignore
	public void testLocalFilesAreFound() throws Exception{
		assertTrue(new File("target").exists());
		new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		assertTrue(new File("target").exists());
	}
	
	@Test
	public void testLocalDirAutoCreated() throws Exception{
		assertFalse(new File("foo").exists());
		new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context.xml", this.getClass());
		assertTrue(new File("foo").exists());
	}
	
	@Test(expected=BeanCreationException.class)
	public void testLocalDirAutoCreateFailed() throws Exception{
		new ClassPathXmlApplicationContext("InboundChannelAdapterParserTests-context-fail-autocreate.xml", this.getClass());
	}
	
	@After
	public void cleanUp() throws Exception{
		new File("foo").delete();
	}

}
