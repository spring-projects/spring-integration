/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.transformer;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.TestApplicationContextAware;
import org.springframework.messaging.Message;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alex Peters
 * @author Artem Bilan
 */
public abstract class AbstractFilePayloadTransformerTests<T extends AbstractFilePayloadTransformer<?>>
		implements TestApplicationContextAware {

	static final String DEFAULT_ENCODING = "UTF-8";

	static final String SAMPLE_CONTENT = "HelloWorld\näöüß";

	T transformer;

	Message<File> message;

	File sourceFile;

	@BeforeEach
	public void setUpCommonTestData() throws Exception {
		sourceFile = File.createTempFile("anyFile", ".txt");
		sourceFile.deleteOnExit();
		FileCopyUtils.copy(SAMPLE_CONTENT.getBytes(DEFAULT_ENCODING),
				new FileOutputStream(sourceFile, false));
		message = MessageBuilder.withPayload(sourceFile).build();
	}

	@AfterEach
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
