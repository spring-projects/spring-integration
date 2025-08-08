/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
