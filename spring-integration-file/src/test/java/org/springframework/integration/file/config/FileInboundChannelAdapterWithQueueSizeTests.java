/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FileInboundChannelAdapterWithQueueSizeTests {

	private static final String PATHNAME = System.getProperty("java.io.tmpdir") + "/"
			+ FileInboundChannelAdapterWithQueueSizeTests.class.getSimpleName();

	private static File inputDir;

	@Autowired
	@Qualifier("inputDirPoller.adapter.source")
	FileReadingMessageSource source1;

	@Autowired
	@Qualifier("inputDirPollerSimpleFilter.adapter.source")
	FileReadingMessageSource source2;

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(PATHNAME);
		inputDir.mkdir();
		clean();
	}

	@AfterClass
	public static void tearDown() {
		clean();
		inputDir.delete();
	}

	private static void clean() {
		File[] files = inputDir.listFiles();
		for (File file : files) {
			file.delete();
		}
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@Test
	public void queueSize() {
		HeadDirectoryScanner scanner1 = TestUtils.getPropertyValue(source1, "scanner", HeadDirectoryScanner.class);
		HeadDirectoryScanner scanner2 = TestUtils.getPropertyValue(source2, "scanner", HeadDirectoryScanner.class);
		List<File> files = scanner1.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(2);
		files = scanner2.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(2);
		files.get(0).delete();
		files.get(1).delete();
		files = scanner1.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(1);
		files = scanner2.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(1);
	}

}
