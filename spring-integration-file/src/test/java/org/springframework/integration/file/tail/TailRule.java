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

package org.springframework.integration.file.tail;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Ignores tests annotated with {@link TailAvailable} if 'tail' with the requested options
 * does not work on this platform.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TailRule extends TestWatcher {

	private static final Log logger = LogFactory.getLog(TailRule.class);

	private static final String tmpDir = System.getProperty("java.io.tmpdir");

	private final String commandToTest;

	public TailRule(String optionsToTest) {
		this.commandToTest = "tail " + optionsToTest + " ";
	}

	@Override
	public Statement apply(Statement base, Description description) {
		if (description.getAnnotation(TailAvailable.class) != null) {
			if (!tailWorksOnThisMachine()) {
				return new Statement() {

					@Override
					public void evaluate() throws Throwable {
						// skip
					}
				};
			}
		}
		return super.apply(base, description);
	}

	private boolean tailWorksOnThisMachine() {
		if (tmpDir.contains(":")) {
			return false;
		}
		File testDir = new File(tmpDir, "FileTailingMessageProducerTests");
		testDir.mkdir();
		final File file = new File(testDir, "foo");
		int result = -99;
		try {
			OutputStream fos = new FileOutputStream(file);
			fos.write("foo".getBytes());
			fos.close();
			final AtomicReference<Integer> c = new AtomicReference<Integer>();
			final CountDownLatch latch = new CountDownLatch(1);
			Future<Process> future = Executors.newSingleThreadExecutor().submit(new Callable<Process>() {

				@Override
				public Process call() throws Exception {
					final Process process = Runtime.getRuntime().exec(commandToTest + " " + file.getAbsolutePath());
					Executors.newSingleThreadExecutor().execute(new Runnable() {

						@Override
						public void run() {
							try {
								c.set(process.getInputStream().read());
								latch.countDown();
							}
							catch (IOException e) {
								logger.error("Error reading test stream", e);
							}
						}
					});
					return process;
				}
			});
			try {
				Process process = future.get(10, TimeUnit.SECONDS);
				if (latch.await(10, TimeUnit.SECONDS)) {
					Integer read = c.get();
					if (read != null && read == 'f') {
						result = 0;
					}
				}
				process.destroy();
			}
			catch (ExecutionException e) {
				result = -999;
			}
			file.delete();
		}
		catch (Exception e) {
			logger.error("failed to test tail", e);
		}
		if (result != 0) {
			logger.warn("tail command is not available on this platform; result:" + result);
		}
		return result == 0;
	}

	public static class TestRule {

		@Test
		public void test1() {
			TailRule rule = new TailRule("-BLAH");
			assertFalse(rule.tailWorksOnThisMachine());
		}
	}
}
