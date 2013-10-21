/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.xml.selector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.messaging.support.GenericMessage;

/**
 * @author Jonas Partner
 */
public class StringValueTestXPathMessageSelectorTests {

	@Test
	public void testMatchWithSimpleString() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/one/two", "red");
		assertTrue(selector.accept(new GenericMessage<String>("<one><two>red</two></one>")));
	}

	@Test
	public void testNoMatchWithSimpleString() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/one/two", "red");
		assertFalse(selector.accept(new GenericMessage<String>("<one><two>yellow</two></one>")));
	}

	@Test
	public void testMatchWithSimpleStringAndNamespace() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two","ns1","www.example.org", "red");
		assertTrue(selector.accept(new GenericMessage<String>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>red</ns1:two></ns1:one>")));
	}

	@Test
	public void testCaseSensitiveByDefault() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two","ns1","www.example.org", "red");
		assertFalse(selector.accept(new GenericMessage<String>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>RED</ns1:two></ns1:one>")));
	}

	@Test
	public void testNotCaseSensitive() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two","ns1","www.example.org", "red");
		selector.setCaseSensitive(false);
		assertTrue(selector.accept(new GenericMessage<String>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>RED</ns1:two></ns1:one>")));
	}

}
