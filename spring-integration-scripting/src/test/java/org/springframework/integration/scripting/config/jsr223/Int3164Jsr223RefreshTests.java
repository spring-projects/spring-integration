/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Int3164Jsr223RefreshTests {

	private static File workDir;

	private static File scriptFile;

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private PollableChannel outputChannel;

	@BeforeClass
	public static void start() throws IOException {
		String basePath = System.getProperty("java.io.tmpdir") + File.separator + "Int3164Jsr223RefreshTests";
		workDir = new File(basePath);
		shutdown();
		workDir.mkdir();
		workDir.deleteOnExit();
		scriptFile = new File(workDir, "int3164.groovy");
		scriptFile.createNewFile();
		FileCopyUtils.copy("1".getBytes(), scriptFile);
	}

	@AfterClass
	public static void shutdown() {
		if (workDir != null && workDir.exists()) {
			for (File file : workDir.listFiles()) {
				file.delete();
			}
			workDir.delete();
		}
	}

	@Test
	public void testRefreshingScript() throws Exception {
		this.referencedScriptInput.send(new GenericMessage<Object>("test"));
		assertThat(this.outputChannel.receive(100).getPayload()).isEqualTo(1);

		FileCopyUtils.copy("2".getBytes(), scriptFile);
		scriptFile.setLastModified(System.currentTimeMillis() + 10000); // force refresh

		this.referencedScriptInput.send(new GenericMessage<Object>("test"));
		assertThat(this.outputChannel.receive(100).getPayload()).isEqualTo(2);
	}

}
