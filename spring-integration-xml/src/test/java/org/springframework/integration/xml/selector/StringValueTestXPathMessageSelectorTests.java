/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.xml.selector;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class StringValueTestXPathMessageSelectorTests {

	@Test
	public void testMatchWithSimpleString() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/one/two", "red");
		assertThat(selector.accept(new GenericMessage<>("<one><two>red</two></one>"))).isTrue();
	}

	@Test
	public void testNoMatchWithSimpleString() {
		StringValueTestXPathMessageSelector selector = new StringValueTestXPathMessageSelector("/one/two", "red");
		assertThat(selector.accept(new GenericMessage<>("<one><two>yellow</two></one>"))).isFalse();
	}

	@Test
	public void testMatchWithSimpleStringAndNamespace() {
		var selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two", "ns1", "www.example.org", "red");
		assertThat(selector
				.accept(new GenericMessage<>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>red</ns1:two></ns1:one>")))
				.isTrue();
	}

	@Test
	public void testCaseSensitiveByDefault() {
		var selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two", "ns1", "www.example.org", "red");
		assertThat(
				selector.accept(
						new GenericMessage<>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>RED</ns1:two></ns1:one>")))
				.isFalse();
	}

	@Test
	public void testNotCaseSensitive() {
		var selector = new StringValueTestXPathMessageSelector("/ns1:one/ns1:two", "ns1", "www.example.org", "red");
		selector.setCaseSensitive(false);
		assertThat(selector
				.accept(new GenericMessage<>("<ns1:one xmlns:ns1='www.example.org'><ns1:two>RED</ns1:two></ns1:one>")))
				.isTrue();
	}

}
