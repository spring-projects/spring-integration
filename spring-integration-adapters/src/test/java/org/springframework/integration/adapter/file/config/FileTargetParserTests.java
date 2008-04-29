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

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.adapter.file.FileTarget;
import org.springframework.integration.message.Target;

/**
 * @author Mark Fisher
 */
public class FileTargetParserTests {

	@Test
	public void testFileTarget() {
		ApplicationContext context = new ClassPathXmlApplicationContext("fileTargetParserTests.xml", this.getClass());
		Target target = (Target) context.getBean("target");
		assertEquals(FileTarget.class, target.getClass());
	}

}
