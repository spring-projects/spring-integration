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

package org.springframework.integration.file.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Gunnar Hillert
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FileInboundChannelAdapterWithQueueSizeTests {

	@Autowired
	FileReadingMessageSource source;

	private DirectFieldAccessor accessor;

	@Before
	public void init() {
		accessor = new DirectFieldAccessor(source);
	}

	@Test
	public void queueSize() {
		Object scanner = accessor.getPropertyValue("scanner");
		assertThat(scanner, is(instanceOf(HeadDirectoryScanner.class)));
	}
}
