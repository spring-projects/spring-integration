/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.scripting.config.jsr223;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
@EnabledForJreRange(min = JRE.JAVA_21, disabledReason = "JRuby 10.0.5.0")
public class Int3164Jsr223RefreshTests {

	private static File workDir;

	private static File scriptFile;

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private PollableChannel outputChannel;

	@BeforeAll
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

	@AfterAll
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
