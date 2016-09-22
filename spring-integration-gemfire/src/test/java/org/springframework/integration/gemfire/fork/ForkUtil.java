/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.integration.gemfire.fork;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility for forking Java processes. Modified from the SGF version for SI
 *
 * @author Costin Leau
 * @author David Turanski
 * @author Gary Russell
 *
 *
 */
public class ForkUtil {

	private static final Log logger = LogFactory.getLog(ForkUtil.class);

	private static String TEMP_DIR = System.getProperty("java.io.tmpdir");

	private ForkUtil() {
		super();
	}

	public static OutputStream cloneJVM(String argument) {
		String cp = System.getProperty("java.class.path");
		String home = System.getProperty("java.home");

		Process proc = null;
		String java = home + "/bin/java".replace("\\", "/");

		String argClass = argument;

		String[] cmdArray = {java, "-cp", cp, argClass};
		try {
			// ProcessBuilder builder = new ProcessBuilder(cmd, argCp,
			// argClass);
			// builder.redirectErrorStream(true);
			proc = Runtime.getRuntime().exec(cmdArray);
		}
		catch (IOException ioe) {
			throw new IllegalStateException("Cannot start command " + cmdArray, ioe);
		}

		logger.info("Started fork");
		final Process p = proc;

		final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final BufferedReader ebr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		final AtomicBoolean run = new AtomicBoolean(true);

		Thread reader = copyStdXxx(br, run, System.out);
		Thread errReader = copyStdXxx(ebr, run, System.err);

		reader.start();
		errReader.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("Stopping fork...");
				run.set(false);
				if (p != null) {
					p.destroy();
				}

				try {
					p.waitFor();
				}
				catch (InterruptedException e) {
					// ignore
				}
				logger.info("Fork stopped");
			}
		});

		return proc.getOutputStream();
	}

	private static Thread copyStdXxx(final BufferedReader br,
			final AtomicBoolean run, final PrintStream out) {
		Thread reader = new Thread(() -> {
			try {
				String line = null;
				do {
					while ((line = br.readLine()) != null) {
						out.println("[FORK] " + line);
					}
				} while (run.get());
			}
			catch (Exception ex) {
				// ignore and exit
			}
		});
		return reader;
	}

	public static OutputStream cacheServer() {
		return startCacheServer("org.springframework.integration.gemfire.fork.CacheServerProcess");
	}

	public static OutputStream cacheServer(String className) {
		return startCacheServer(className);
	}

	private static OutputStream startCacheServer(String className) {

		if (controlFileExists(className)) {
			deleteControlFile(className);
		}
		OutputStream os = cloneJVM(className);
		int maxTime = 60000;
		int time = 0;
		while (!controlFileExists(className) && time < maxTime) {
			try {
				Thread.sleep(500);
				time += 500;
			}
			catch (InterruptedException ex) {
				// ignore and move on
			}
		}
		if (controlFileExists(className)) {
			logger.info("Started cache server");
		}
		else {
			throw new RuntimeException("could not fork cache server");
		}
		return os;
	}

	public static boolean deleteControlFile(String name) {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).delete();
	}

	public static boolean createControlFile(String name) throws IOException {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).createNewFile();
	}

	public static boolean controlFileExists(String name) {
		String path = TEMP_DIR + File.separator + name;
		return new File(path).exists();
	}

}
