/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.codec.kryo;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Turanski
 * @since 4.2
 */
public class FileKryoRegistrarTests {

	@Test
	public void test() throws IOException {
		PojoCodec pc = new PojoCodec(new FileKryoRegistrar());
		File file = new File("/foo/bar");
		File file2 = pc.decode(pc.encode(file), File.class);
		assertThat(file2).isEqualTo(file);
	}

}
