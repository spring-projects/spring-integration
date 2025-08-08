/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FileLockingWithMultipleSourcesIntegrationTests {

	private static File workdir;

	@BeforeClass
	public static void setupWorkDirectory() {
		workdir = new File(new File(System.getProperty("java.io.tmpdir")),
				FileLockingWithMultipleSourcesIntegrationTests.class.getSimpleName());
		workdir.mkdir();
	}

	@Autowired
	@Qualifier("fileSource1")
	private FileReadingMessageSource fileSource1;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource2;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource3;

	@Before
	public void cleanoutWorkDir() {
		for (File file : workdir.listFiles()) {
			file.delete();
		}
	}

	@Test
	public void filePickedUpOnceWithDistinctFilters() throws IOException {
		File testFile = new File(workdir, "test");
		testFile.createNewFile();
		assertThat(this.fileSource1.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		assertThat(this.fileSource2.receive()).isNull();
		FileChannelCache.closeChannelFor(testFile);
	}

	@Test
	public void filePickedUpTwiceWithSharedFilter() throws Exception {
		File testFile = new File(workdir, "test");
		testFile.createNewFile();
		assertThat(this.fileSource1.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		assertThat(this.fileSource3.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		FileChannelCache.closeChannelFor(testFile);
	}

}
