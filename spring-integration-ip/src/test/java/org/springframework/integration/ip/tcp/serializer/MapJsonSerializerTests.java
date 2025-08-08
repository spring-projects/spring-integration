/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class MapJsonSerializerTests {

	@Test
	public void multi() throws Exception {
		String json = "{\"headers\":{\"bar\":\"baz\"},\"payload\":\"foo\"}\n";
		String twoJson = json + json;
		MapJsonSerializer deserializer = new MapJsonSerializer();
		ByteArrayInputStream bais = new ByteArrayInputStream(twoJson.getBytes("UTF-8"));
		Map<?, ?> map = deserializer.deserialize(bais);
		assertThat(map).isNotNull();
		map = deserializer.deserialize(bais);
		assertThat(map).isNotNull();
	}

}
