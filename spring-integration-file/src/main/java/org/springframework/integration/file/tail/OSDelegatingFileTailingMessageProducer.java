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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.MessagingException;
import org.springframework.util.Assert;

/**
 * A file tailing message producer that delegates to the OS tail program.
 * This is likely the most efficient mechanism on platforms that support it.
 * Default options are "-F -n 0" (follow file name, no existing records).
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class OSDelegatingFileTailingMessageProducer extends FileTailingMessageProducerSupport
		implements Runnable {

	private volatile Process process;

	private volatile String options = "-F -n 0";

	private volatile String command = "ADAPTER_NOT_INITIALIZED";

	private volatile BufferedReader reader;

	private volatile CountDownLatch readerInitialized;

	public void setOptions(String options) {
		if (options == null) {
			this.options = "";
		}
		else {
			this.options = options;
		}
	}

	@Override
	public String getComponentType() {
		return super.getComponentType() + " (native)";
	}

	@Override
	protected void onInit() {
		Assert.notNull(getFile(), "File cannot be null");
		super.onInit();
		this.command = "tail " + this.options + " " + this.getFile().getAbsolutePath();
	}

	@Override
	protected void doStart() {
		super.doStart();
		destroyProcess();
		this.runExec();
	}

	@Override
	protected void doStop() {
		super.doStop();
		destroyProcess();
	}

	private void destroyProcess() {
		if (this.process != null) {
			this.process.destroy();
			this.process = null;
		}
	}

	/**
	 * Exec the native tail process.
	 */
	private void runExec() {
		try {
			this.readerInitialized = new CountDownLatch(1);
			Process process = Runtime.getRuntime().exec(this.command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.getTaskExecutor().execute(this);
			this.process = process;
			this.startStatusReader();
			this.startProcessMonitor();
			this.reader = reader;
			this.readerInitialized.countDown();
		}
		catch (IOException e) {
			throw new MessagingException("Failed to exec tail command: '" + this.command + "'", e);
		}
	}

	/**
	 * Runs a thread that waits for the Process result.
	 */
	private void startProcessMonitor() {
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				int result = Integer.MIN_VALUE;
				try {
					result = process.waitFor();
					if (logger.isInfoEnabled()) {
						logger.info("tail process terminated with value " + result);
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					logger.error("Interrupted - stopping adapter", e);
					stop();
				}
				finally {
					if (process != null) {
						process.destroy();
						process = null;
					}
				}
				if (isRunning()) {
					logger.info("Restarting tail process");
					runExec();
				}
			}
		});
	}

	/**
	 * Runs a thread that reads stderr - on some platforms status messages
	 * (file not available, rotations etc) are sent to stderr.
	 */
	private void startStatusReader() {
		final BufferedReader errorReader = new BufferedReader(new InputStreamReader(this.process.getErrorStream()));
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				String statusMessage;
				try {
					while ((statusMessage = errorReader.readLine()) != null) {
						publish(statusMessage);
						if (logger.isTraceEnabled()) {
							logger.trace(statusMessage);
						}
					}
				}
				catch (IOException e) {
					logger.error("Exception on tail error reader", e);
				}
				finally {
					try {
						errorReader.close();
					}
					catch (IOException e) {
						logger.error("Exception while closing stderr", e);
					}
				}
			}
		});
	}

	/**
	 * Reads lines from stdout and sends in a message to the output channel.
	 */
	@Override
	public void run() {
		try {
			if (!this.readerInitialized.await(60, TimeUnit.SECONDS)) {
				logger.error("No stdout reader to read");
				throw new IllegalStateException("No stdout reader to read");
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
		String line;
		try {
			while ((line = this.reader.readLine()) != null) {
				this.send(line);
			}
		}
		catch (IOException e) {
			logger.error("Exception on tail error reader", e);
			try {
				this.reader.close();
			}
			catch (IOException e1) {

			}
			this.process.destroy();
			process = null;
		}
	}


}
