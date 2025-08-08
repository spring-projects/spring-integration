/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class PooledDeserializationTests {

	@Test
	public void testCRLF() throws IOException {
		ByteArrayCrLfSerializer deser = new ByteArrayCrLfSerializer();
		deser.setPoolSize(2);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo\r\n".getBytes());
		for (int i = 0; i < 5; i++) {
			bais.reset();
			byte[] bytes = deser.deserialize(bais);
			assertThat(new String(bytes)).isEqualTo("foo");
		}
		try {
			deser.deserialize(bais);
			fail("Expected SoftEndOfStreamException");
		}
		catch (SoftEndOfStreamException e) {
			// expected
		}
		assertThat(TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).size()).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(deser, "pool.inUse", Set.class).size()).isEqualTo(0);
	}

	@Test
	public void testRawMaxMessageSizeEqualDontReturnPooledItem() throws IOException {
		ByteArrayRawSerializer deser = new ByteArrayRawSerializer();
		deser.setPoolSize(2);
		deser.setMaxMessageSize(3);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
		byte[] bytes = deser.deserialize(bais);
		assertThat(new String(bytes)).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).size()).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(deser, "pool.inUse", Set.class).size()).isEqualTo(0);
		assertThat(TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).iterator().next()).isNotSameAs(bytes);
	}

}
