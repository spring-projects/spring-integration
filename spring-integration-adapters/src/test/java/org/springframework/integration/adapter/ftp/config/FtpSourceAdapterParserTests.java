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

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.PollingSourceAdapter;
import org.springframework.integration.adapter.ftp.FtpSource;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class FtpSourceAdapterParserTests {

	@Test
	public void testFtpSourceAdapterParser() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ftpSourceAdapterParserTests.xml", this.getClass());
		PollingSourceAdapter ftpAdapter = (PollingSourceAdapter) context.getBean("ftpAdapter");
		DirectFieldAccessor adapterAccessor = new DirectFieldAccessor(ftpAdapter);
		assertEquals(FtpSource.class, adapterAccessor.getPropertyValue("source").getClass());
		assertEquals(context.getBean("testChannel"), adapterAccessor.getPropertyValue("channel"));
		assertEquals(12345L, ((PollingSchedule) adapterAccessor.getPropertyValue("schedule")).getPeriod());
	}

}
