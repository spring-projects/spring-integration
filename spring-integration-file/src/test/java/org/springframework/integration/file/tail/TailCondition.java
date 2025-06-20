/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.file.tail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import org.springframework.core.annotation.MergedAnnotations;

/**
 * Ignores tests annotated with {@link TailAvailable} if 'tail' with the requested options
 * does not work on this platform.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Jiandong Ma
 * @author Jooyoung Pyoung
 *
 * @since 6.5
 *
 */
public class TailCondition implements ExecutionCondition {

	private static final Log logger = LogFactory.getLog(TailCondition.class);

	private static final String tmpDir = System.getProperty("java.io.tmpdir");

	private String commandToTest;

	private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled(
			"@TailAvailable is not present");

	public void setOptionsToTest(String optionsToTest) {
		this.commandToTest = "tail " + optionsToTest + " ";
	}

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<AnnotatedElement> element = context.getElement();
		MergedAnnotations annotations = MergedAnnotations.from(element.get(),
				MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		if (annotations.get(TailAvailable.class).isPresent()) {
			TailAvailable tail = annotations.get(TailAvailable.class).synthesize();
			setOptionsToTest(tail.options());
			if (!tailWorksOnThisMachine()) {
				return ConditionEvaluationResult.disabled(
						"Tests Ignored: 'Tail' command does not work on this platform");
			}
		}
		return ENABLED;
	}

	private boolean tailWorksOnThisMachine() {
		File testDir = new File(tmpDir, "FileTailingMessageProducerTests");
		testDir.mkdir();
		final File file = new File(testDir, "foo");
		int result = -99;
		try {
			OutputStream fos = new FileOutputStream(file);
			fos.write("foo".getBytes());
			fos.close();
			final AtomicReference<Integer> c = new AtomicReference<>();
			final CountDownLatch latch = new CountDownLatch(1);
			ExecutorService newSingleThreadExecutor = Executors.newSingleThreadExecutor();
			Future<Process> future =
					newSingleThreadExecutor.submit(() -> {
						ProcessBuilder pb = new ProcessBuilder(commandToTest, file.getAbsolutePath());
						final Process process = pb.start();
						ExecutorService executorService = Executors.newSingleThreadExecutor();
						executorService.execute(() -> {
							try {
								c.set(process.getInputStream().read());
								latch.countDown();
							}
							catch (IOException e) {
								logger.error("Error reading test stream", e);
							}
						});
						executorService.shutdown();
						return process;
					});
			newSingleThreadExecutor.shutdown();
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
}
