/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class UUIDConverterTests {

	@Test
	public void testConvertNull() {
		assertThat(UUIDConverter.getUUID(null)).isNull();
	}

	@Test
	public void testConvertUUID() {
		UUID uuid = UUID.randomUUID();
		assertThat(UUIDConverter.getUUID(uuid)).isEqualTo(uuid);
	}

	@Test
	public void testConvertUUIDString() {
		UUID uuid = UUID.randomUUID();
		assertThat(UUIDConverter.getUUID(uuid.toString())).isEqualTo(uuid);
	}

	@Test
	public void testConvertAlmostUUIDString() {
		String name = "1-2-3-4";
		assertThatThrownBy(() -> UUID.fromString(name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid UUID string");
		assertThat(UUIDConverter.getUUID(name)).isNotNull();
	}

	@Test
	public void testConvertRandomString() {
		UUID uuid = UUIDConverter.getUUID("foo");
		assertThat(uuid).isNotNull();
		String uuidString = uuid.toString();
		assertThat(UUIDConverter.getUUID("foo").toString()).isEqualTo(uuidString);
		assertThat(UUIDConverter.getUUID(uuid).toString()).isEqualTo(uuidString);
	}

	@Test
	public void testConvertPrimitive() {
		assertThat(UUIDConverter.getUUID(1L)).isNotNull();
	}

	@Test
	public void testConvertSerializable() {
		assertThat(UUIDConverter.getUUID(new Date())).isNotNull();
	}

	@Test
	public void testConvertNonSerializable() {
		assertThatThrownBy(() -> assertThat(UUIDConverter.getUUID(new Object())).isNotNull())
				.isInstanceOf(IllegalArgumentException.class);
	}

}
