/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xml.config;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Jonas Partner
 */
public class TestTemplatesFactory implements FactoryBean<Templates> {

	public Templates getObject() throws Exception {
		org.springframework.core.io.Resource xslResource = new ClassPathResource("test.xsl", getClass());
		return TransformerFactory.newInstance().newTemplates(new StreamSource(xslResource.getInputStream()));
	}

	public Class<Templates> getObjectType() {
		return Templates.class;
	}

	public boolean isSingleton() {
		return false;
	}

}
