/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file.transformer;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Peters
 * @author Artem Bilan
 */
public abstract class AbstractFilePayloadTransformerTests<T extends AbstractFilePayloadTransformer<?>> {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\näöüß";

	T transformer;

	Message<File> message;

	File sourceFile;

	@Before
	public void setUpCommonTestData() throws Exception {
		sourceFile = File.createTempFile("anyFile", ".txt");
		sourceFile.deleteOnExit();
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING),
				new FileOutputStream(sourceFile, false));
		message = MessageBuilder.withPayload(sourceFile).build();
	}

	@After
	public void tearDownCommonTestData() {
		if (sourceFile.exists()) {
			sourceFile.delete();
		}
	}

	@Test
	public void transform_withSourceHeaderValues_copiedToResult() {
		String anyKey = "foo1";
		String anyValue = "bar1";
		message = MessageBuilder.fromMessage(message).setHeader(anyKey, anyValue).build();
		Message<?> result = transformer.transform(message);
		assertThat(result).isNotNull();
		assertThat(result.getHeaders()).containsEntry(anyKey, anyValue);
	}

	@Test
	public void transform_withFilePayload_filenameInHeaders() {
		Message<?> result = transformer.transform(message);
		assertThat(result).isNotNull();
		assertThat(result.getHeaders()).containsEntry(FileHeaders.FILENAME, sourceFile.getName());
	}

	@Test
	public void transform_withDefaultSettings_fileNotDeleted() {
		transformer.transform(message);
		assertThat(sourceFile.exists()).isTrue();
	}

	@Test
	public void transform_withDeleteSetting_doesNotExistAtOldLocation() {
		transformer.setDeleteFiles(true);
		transformer.transform(message);
		assertThat(sourceFile.exists()).as("exists at: " + sourceFile.getAbsolutePath()).isFalse();
	}

}
