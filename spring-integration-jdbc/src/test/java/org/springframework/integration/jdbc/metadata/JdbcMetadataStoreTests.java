/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.jdbc.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Bojan Vukasovic
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext // close at the end after class
@Transactional
public class JdbcMetadataStoreTests {

	@Autowired
	private DataSource dataSource;


	private JdbcMetadataStore metadataStore;

	@BeforeEach
	public void init() {
		metadataStore = new JdbcMetadataStore(dataSource);
		metadataStore.afterPropertiesSet();
	}

	@Test
	public void keyAndValuesArePreservedOnPut() {
		metadataStore.put("foo", "bar");
		metadataStore.put("foo", "bar1");
		metadataStore.put("foo2", "bar2");
		String bar1 = metadataStore.get("foo");
		String bar2 = metadataStore.get("foo2");
		assertThat(bar1).isEqualTo("bar1");
		assertThat(bar2).isEqualTo("bar2");
	}

	@Test
	public void keyAndValuesAreNotPreservedOnRemove() {
		metadataStore.put("foo", "bar");
		metadataStore.put("foo2", "bar2");
		metadataStore.remove("foo");
		String bar = metadataStore.get("foo");
		metadataStore.remove("foo2");
		String bar2 = metadataStore.get("foo2");
		assertThat(bar).isNull();
		assertThat(bar2).isNull();
	}

	@Test
	public void keyAndValuesAreNotOverwrittenOnPutIfAbsent() {
		metadataStore.put("foo", "bar");
		metadataStore.putIfAbsent("foo", "bar1");
		String bar = metadataStore.get("foo");
		assertThat(bar).isEqualTo("bar");
	}

	@Test
	public void nonExistentKeyIsNotRemoved() {
		metadataStore.remove("non-existent");
		String ne = metadataStore.get("non-existent");
		assertThat(ne).isNull();
	}

	@Test
	public void existingKeyValueIsReplacedWithNewValueWhenOldValueMatches() {
		metadataStore.put("foo", "bar");
		metadataStore.replace("foo", "bar", "bar2");
		String bar2 = metadataStore.get("foo");
		assertThat(bar2).isEqualTo("bar2");
	}

	@Test
	public void existingKeyValueIsNotReplacedWithNewValueWhenOldValueDoesNotMatch() {
		metadataStore.put("foo", "bar");
		metadataStore.replace("foo", "bar1", "bar2");
		String bar = metadataStore.get("foo");
		assertThat(bar).isEqualTo("bar");
	}

}
