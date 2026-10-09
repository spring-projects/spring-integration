/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.file.inbound;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class FileReadingMessageSourcePersistentFilterIntegrationTests {

	@Autowired
	FileReadingMessageSource pollableFileSource;

	@TempDir
	public static File inputDir;

	@BeforeEach
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@AfterEach
	public void cleanupInputDir() {
		File[] listFiles = inputDir.listFiles();
		for (File listFile : listFiles) {
			listFile.delete();
		}
	}

	@Test
	public void configured() {
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollableFileSource);
		assertThat(accessor.getPropertyValue("directoryExpression.value")).isEqualTo(inputDir);
	}

	@Test
	public void getFiles() {
		Message<File> received1 = this.pollableFileSource.receive();
		assertThat(received1).as("This should return the first message").isNotNull();
		Message<File> received2 = this.pollableFileSource.receive();
		assertThat(received2).isNotNull();
		Message<File> received3 = this.pollableFileSource.receive();
		assertThat(received3).isNotNull();
		assertThat(received2.getPayload()).as(received1 + " == " + received2).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received1 + " == " + received3).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received2 + " == " + received3).isNotSameAs(received2.getPayload());

		Message<File> received4 = this.pollableFileSource.receive();
		assertThat(received4).isNull();
	}

}
