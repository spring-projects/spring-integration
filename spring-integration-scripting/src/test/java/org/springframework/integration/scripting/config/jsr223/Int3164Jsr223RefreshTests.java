/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.scripting.config.jsr223;

import static org.junit.Assert.assertEquals;

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
		assertEquals(1, this.outputChannel.receive(100).getPayload());

		FileCopyUtils.copy("2".getBytes(), scriptFile);
		scriptFile.setLastModified(System.currentTimeMillis() + 10000); // force refresh

		this.referencedScriptInput.send(new GenericMessage<Object>("test"));
		assertEquals(2, this.outputChannel.receive(100).getPayload());
	}

}
