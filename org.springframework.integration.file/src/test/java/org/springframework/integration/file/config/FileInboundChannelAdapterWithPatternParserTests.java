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

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.AcceptOnceFileListFilter;
import org.springframework.integration.file.CompositeFileListFilter;
import org.springframework.integration.file.FileListFilter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.PatternMatchingFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithPatternParserTests {

	@Autowired(required=true)
	private ApplicationContext context;

	@Autowired(required=true)
	@Qualifier("adapterWithPattern.adapter")
	private AbstractEndpoint endpoint;

	private DirectFieldAccessor accessor;


	@Autowired(required=true)
	public void setSource(FileReadingMessageSource source) {
		this.accessor = new DirectFieldAccessor(source);
	}

	@Test
	public void channelName() {
		MessageChannel channel = (MessageChannel) context.getBean("adapterWithPattern");
		assertEquals("adapterWithPattern", channel.getName());		
	}

	@Test
	public void autoStartupDisabled() {
		assertFalse(this.endpoint.isRunning());
		Boolean autoStartupValue = (Boolean) new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup");
		assertFalse(autoStartupValue);
	}

	@Test
	public void inputDirectory() {
		assertEquals(System.getProperty("java.io.tmpdir"), ((File) accessor.getPropertyValue("inputDirectory")).getPath());
	}

	@Test
	public void compositeFilterType() {
		assertTrue(accessor.getPropertyValue("filter") instanceof CompositeFileListFilter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void compositeFilterSetSize() {
		Set<FileListFilter> filters = (Set<FileListFilter>) new DirectFieldAccessor(
				accessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		assertEquals(2, filters.size());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void acceptOnceFilter() {
		Set<FileListFilter> filters = (Set<FileListFilter>) new DirectFieldAccessor(
				accessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		boolean hasAcceptOnceFilter = false;
		for (FileListFilter filter : filters) {
			if (filter instanceof AcceptOnceFileListFilter) {
				hasAcceptOnceFilter = true;
			}
		}
		assertTrue("expected AcceptOnceFileListFilter", hasAcceptOnceFilter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternFilter() {
		Set<FileListFilter> filters = (Set<FileListFilter>) new DirectFieldAccessor(
				accessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		Pattern pattern = null;
		for (FileListFilter filter : filters) {
			if (filter instanceof PatternMatchingFileListFilter) {
				pattern = (Pattern) new DirectFieldAccessor(filter).getPropertyValue("pattern");
			}
		}
		assertNotNull("expected PatternMatchingFileListFilter", pattern);
		assertEquals(".*\\.txt", pattern.toString());
		assertFalse(pattern.matcher("foo").matches());
		assertTrue(pattern.matcher("foo.txt").matches());
	}

}
