/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.transformer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class PayloadTypeConvertingTransformerTests {

	/**
	 * Test method for
	 * {@link org.springframework.integration.transformer.PayloadTypeConvertingTransformer#transformPayload(java.lang.Object)}
	 * .
	 */
	@Test
	public void testTransformPayloadObject() throws Exception {
		PayloadTypeConvertingTransformer<String, String> tx = new PayloadTypeConvertingTransformer<String, String>();
		tx.setConverter(source -> source.toUpperCase());
		String in = "abcd";
		String out = tx.transformPayload(in);
		assertThat(out).isEqualTo("ABCD");
	}

}


