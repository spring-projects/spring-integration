/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.xml.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;

public class TestXmlApplicationContext extends AbstractXmlApplicationContext {

	private final Resource[] resources;

	public TestXmlApplicationContext(String... xmlStrings) {
		resources = new Resource[xmlStrings.length];
		for (int i = 0; i < xmlStrings.length; i++) {
			resources[i] = new TestResource(xmlStrings[i]);
		}
		refresh();
	}

	@Override
	protected Resource[] getConfigResources() {
		return resources;
	}

	private static class TestResource extends AbstractResource {

		String xmlString;

		TestResource(String xmlString) {
			this.xmlString = xmlString;
		}


		@Override
		public String getDescription() {
			return "test";
		}

		@Override
		public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
		}

	}

}
