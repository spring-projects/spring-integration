/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TailRule extends TestWatcher {

	private static final Log logger = LogFactory.getLog(TailRule.class);

	private static final String tmpDir = System.getProperty("java.io.tmpdir");

	private static Boolean tailWorks = tailWorksOnThisMachine();


	@Override
	public Statement apply(Statement base, Description description) {
		if (description.getAnnotation(TailAvailable.class) != null) {
			if (!tailWorks) {
				return new Statement() {

					@Override
					public void evaluate() throws Throwable {
						// skip
					}};
			}
		}
		return super.apply(base, description);
	}

	private static boolean tailWorksOnThisMachine() {
		File testDir = new File(tmpDir, "FileTailingMessageProducerTests");
		testDir.mkdir();
		File file = new File(testDir, "foo");
		int result = -99;
		try {
			OutputStream fos = new FileOutputStream(file);
			fos.write("foo".getBytes());
			fos.close();
			Process process = Runtime.getRuntime().exec("tail " + file.getAbsolutePath());
			result = process.waitFor();
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
