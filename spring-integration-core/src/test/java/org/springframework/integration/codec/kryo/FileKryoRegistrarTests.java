/*
 * Copyright 2015-2025 the original author or authors.
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

package org.springframework.integration.codec.kryo;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

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
