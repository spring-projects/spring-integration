/*
 * Copyright 2011 the original author or authors.
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Utility for forking Java processes. Modified from the SGF version for SI
 * 
 * @author Costin Leau
 * @author David Turanski
 * 
 * 
 */
public class ForkUtil {

	public static OutputStream cloneJVM(String argument) {
		String cp = System.getProperty("java.class.path");
		String home = System.getProperty("java.home");

		Process proc = null;
		String java = home + "/bin/java ".replace("\\","/");;
		String argCp = "-cp " + cp;
		String argClass = argument;

		String cmd = java + argCp + " " + argClass;
		try {
			//ProcessBuilder builder = new ProcessBuilder(cmd, argCp, argClass);
			//builder.redirectErrorStream(true);
			proc = Runtime.getRuntime().exec(cmd);
		} catch (IOException ioe) {
			throw new IllegalStateException("Cannot start command " + cmd, ioe);
		}

		System.out.println("Started fork");
		final Process p = proc;

		final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		final AtomicBoolean run = new AtomicBoolean(true);

		Thread reader = new Thread(new Runnable() {

			public void run() {
				try {
					String line = null;
					do {
						while ((line = br.readLine()) != null) {
							System.out.println("[FORK] " + line);
						}
						Thread.sleep(200);
					} while (run.get());
				} catch (Exception ex) {
					// ignore and exit
				}
			}
		});

		reader.start();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Stopping fork...");
				run.set(false);
				if (p != null)
					p.destroy();

				try {
					p.waitFor();
				} catch (InterruptedException e) {
					// ignore
				}
				System.out.println("Fork stopped");
			}
		});

		return proc.getOutputStream();
	}

	public static OutputStream cacheServer() {
		OutputStream os = cloneJVM("org.springframework.integration.gemfire.fork.CacheServerProcess");
		try {
			Thread.sleep(8000);
		} catch (InterruptedException ex) {
			// ignore and move on
		}
		return os;
	}
}