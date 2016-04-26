/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.transformer;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;

/**
 * @author Alex Peters
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileToByteArrayTransformerTests extends
		AbstractFilePayloadTransformerTests<FileToByteArrayTransformer> {

	@Before
	public void setUp() {
		transformer = new FileToByteArrayTransformer();
	}

	@Test
	public void transform_withFilePayload_convertedToByteArray() throws Exception {
		Message<?> result = transformer.transform(message);
		assertThat(result, is(notNullValue()));

		assertThat(result, hasPayload(is(instanceOf(byte[].class))));
		assertThat(result, hasPayload(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING)));
	}

}
