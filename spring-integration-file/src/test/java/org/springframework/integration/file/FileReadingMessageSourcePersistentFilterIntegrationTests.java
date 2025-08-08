/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 */
public class FileReadingMessageSourcePersistentFilterIntegrationTests {

	AbstractApplicationContext context;

	FileReadingMessageSource pollableFileSource;

	private static File inputDir;

	@AfterClass
	public static void cleanUp() throws Throwable {
		if (inputDir.exists()) {
			inputDir.delete();
		}
	}

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileReadingMessageSourcePersistentFilterIntegrationTests.class.getSimpleName());
		inputDir.mkdir();
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		this.loadContextAndGetMessageSource();
	}

	private void loadContextAndGetMessageSource() {
		this.context = new ClassPathXmlApplicationContext(this.getClass().getSimpleName() + "-context.xml",
				this.getClass());
		this.pollableFileSource = context.getBean(FileReadingMessageSource.class);
	}

	@After
	public void cleanoutInputDir() throws Exception {
		File[] listFiles = inputDir.listFiles();
		for (int i = 0; i < listFiles.length; i++) {
			listFiles[i].delete();
		}
	}

	@AfterClass
	public static void removeInputDir() throws Exception {
		inputDir.delete();
		File persistDir = new File(System.getProperty("java.io.tmpdir") + "/"
				+ FileReadingMessageSourcePersistentFilterIntegrationTests.class.getSimpleName()
				+ ".meta");
		File persist = new File(persistDir, "metadata-store.properties");
		persist.delete();
		persistDir.delete();
	}

	@Test
	public void configured() throws Exception {
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollableFileSource);
		assertThat(accessor.getPropertyValue("directory")).isEqualTo(inputDir);
	}

	@Test
	public void getFiles() throws Exception {
		Message<File> received1 = this.pollableFileSource.receive();
		assertThat(received1).as("This should return the first message").isNotNull();
		Message<File> received2 = this.pollableFileSource.receive();
		assertThat(received2).isNotNull();
		Message<File> received3 = this.pollableFileSource.receive();
		assertThat(received3).isNotNull();
		assertThat(received2.getPayload()).as(received1 + " == " + received2).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received1 + " == " + received3).isNotSameAs(received1.getPayload());
		assertThat(received3.getPayload()).as(received2 + " == " + received3).isNotSameAs(received2.getPayload());
		this.context.close();

		loadContextAndGetMessageSource();
		Message<File> received4 = this.pollableFileSource.receive();
		assertThat(received4).isNull();
		this.context.close();
	}

}
