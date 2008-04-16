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
import org.springframework.integration.adapter.PollingSourceAdapter;
import org.springframework.integration.adapter.file.FileSource;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class FileSourceAdapterParserTests {

	@Test
	public void testFileSourceAdapterParser() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileSourceAdapterParserTests.xml", this.getClass());
		PollingSourceAdapter adapter = (PollingSourceAdapter) context.getBean("adapter");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(adapter);
		PollingSchedule schedule = (PollingSchedule) adapterAccessor.getPropertyValue("schedule");
		assertEquals(1234, schedule.getPeriod());
		MessageChannel channel = (MessageChannel) context.getBean("testChannel");
		assertEquals(channel, adapterAccessor.getPropertyValue("channel"));
		Object source = adapterAccessor.getPropertyValue("source");
		assertEquals(FileSource.class, source.getClass());
		DirectFieldAccessor sourceAccessor = new DirectFieldAccessor(source);
		File directory = (File) sourceAccessor.getPropertyValue("directory");
		assertEquals(System.getProperty("java.io.tmpdir"), directory.getAbsolutePath());
	}

}
