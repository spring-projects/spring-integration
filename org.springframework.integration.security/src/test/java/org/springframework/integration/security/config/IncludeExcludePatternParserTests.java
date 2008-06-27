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
package org.springframework.integration.security.config;

import static org.junit.Assert.*;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class IncludeExcludePatternParserTests {

	IncludeExcludePatternParser patternParser;

	@Before
	public void setUp() {
		patternParser = new IncludeExcludePatternParser();
	}

	@Test
	public void testSimpleIncludeWithIncludeByDefaultFalse() throws Exception {
		NodeList nodeList = getNodeList("<doc><includePattern>includeMe</includePattern><excludePattern>.*</excludePattern></doc>");
		OrderedIncludeExcludeList matcher = patternParser.createFromNodeList(false, nodeList);
		assertTrue("Did not match expected entry includeMe", matcher.isIncluded("includeMe"));
		assertFalse("Matched unexpected entry notMe", matcher.isIncluded("notMe"));
	}

	@Test
	public void testIncludeByDefaultTrue() throws Exception {
		NodeList nodeList = getNodeList("<doc></doc>");
		OrderedIncludeExcludeList matcher = patternParser.createFromNodeList(true, nodeList);
		assertTrue("Did not match expected entry includeMe", matcher.isIncluded("anything"));
	}

	@Test
	public void testIncludeByDefaultTrueButExcluded() throws Exception {
		NodeList nodeList = getNodeList("<doc><excludePattern>ex.*</excludePattern><includePattern>exShouldNotMatter</includePattern></doc>");
		OrderedIncludeExcludeList matcher = patternParser.createFromNodeList(true, nodeList);
		assertFalse("Matched unexpected entry exNotMe", matcher.isIncluded("exNotMe"));
		assertFalse("Matched unexpected entry exShouldNotMatter", matcher.isIncluded("exShouldNotMatter"));
	}

	public NodeList getNodeList(String xmlString) throws Exception {
		StringReader reader = new StringReader(xmlString);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(reader));
		return doc.getDocumentElement().getChildNodes();
	}

}
