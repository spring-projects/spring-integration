/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.selector;

import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.3
 *
 */
public class MetadataStoreSelectorTests {

	@Test
	void lineNumbers() {
		SimpleMetadataStore store = new SimpleMetadataStore();
		store.put("file", "5");
		MetadataStoreSelector selector = new MetadataStoreSelector(
				msg -> "file", msg -> msg.getHeaders().get("lineNum").toString(), store);
		selector.setCompareValues((oldValue, newValue) -> Integer.parseInt(oldValue) < Integer.parseInt(newValue));
		for (int i = 0; i < 6; i++) {
			assertThat(selector.accept(MessageBuilder.withPayload("foo").setHeader("lineNum", i).build()))
					.isEqualTo(Boolean.FALSE);
			assertThat(store.get("file")).isEqualTo("5");
		}
		assertThat(selector.accept(MessageBuilder.withPayload("foo").setHeader("lineNum", 6).build()))
				.isEqualTo(Boolean.TRUE);
		assertThat(store.get("file")).isEqualTo("6");
	}

}
