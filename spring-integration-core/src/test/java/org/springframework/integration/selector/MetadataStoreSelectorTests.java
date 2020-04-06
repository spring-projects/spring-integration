/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.integration.selector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.support.MessageBuilder;

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
