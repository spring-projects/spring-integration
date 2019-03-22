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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author Jonas Partner
 */
public class StubDocumentBuilderFactory extends DocumentBuilderFactory {

	@Override
	public Object getAttribute(String name) throws IllegalArgumentException {
		return null;
	}

	@Override
	public boolean getFeature(String name) throws ParserConfigurationException {
		return false;
	}

	@Override
	public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
		return null;
	}

	@Override
	public void setAttribute(String name, Object value) throws IllegalArgumentException {
	}

	@Override
	public void setFeature(String name, boolean value) throws ParserConfigurationException {
	}

}
