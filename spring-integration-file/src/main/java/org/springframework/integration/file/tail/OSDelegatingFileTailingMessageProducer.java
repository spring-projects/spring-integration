/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.util.Assert;

/**
 * A file tailing message producer that delegates to the OS tail program.
 * This is likely the most efficient mechanism on platforms that support it.
 * Default options are "-F -n 0" (follow file name, no existing records).
 *
 * @author Gary Russell
 * @author Gavin Gray
 * @author Ali Shahbour
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 3.0
 *
 */
public class OSDelegatingFileTailingMessageProducer extends FileTailingMessageProducerSupport
		implements SchedulingAwareRunnable {

	private boolean enableStatusReader = true;

	private String options = "-F -n 0";

	private volatile String command = "ADAPTER_NOT_INITIALIZED";

	private volatile String[] tailCommand;

	private volatile Process nativeTailProcess;

	private volatile BufferedReader stdOutReader;

	public void setOptions(String options) {
		this.options = options == null ? "" : options;
	}

	/**
	 * If false, thread for capturing stderr will not be started
	 * and stderr output will be ignored
	 * @param enableStatusReader true or false
	 * @since 4.3.6
	 */
	public void setEnableStatusReader(boolean enableStatusReader) {
		this.enableStatusReader = enableStatusReader;
	}

	public String getCommand() {
		return this.command;
	}

	@Override
	public String getComponentType() {
		return super.getComponentType() + " (native)";
	}

	@Override
	public boolean isLongLived() {
		return true;
	}

	@Override
	protected void onInit() {
		super.onInit();
		Assert.notNull(getFile(), "File cannot be null");
	}

	@Override
	protected void doStart() {
		super.doStart();
		destroyProcess();
		String[] tailOptions = this.options.split("\\s+");
		this.tailCommand = new String[tailOptions.length + 2];
		this.tailCommand[0] = "tail";
		this.tailCommand[this.tailCommand.length - 1] = getFile().getAbsolutePath();
		System.arraycopy(tailOptions, 0, this.tailCommand, 1, tailOptions.length);
		this.command = String.join(" ", this.tailCommand);
		getTaskExecutor().execute(this::runExec);
	}

	@Override
	protected void doStop() {
		super.doStop();
		destroyProcess();
	}

	private void destroyProcess() {
		Process process = this.nativeTailProcess;
		if (process != null) {
			process.destroy();
			this.nativeTailProcess = null;
		}
	}

	/**
	 * Exec the native tail process.
	 */
	private void runExec() {
		destroyProcess();
		logger.info("Starting tail process");
		try {
			Process process = Runtime.getRuntime().exec(this.tailCommand);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.nativeTailProcess = process;
			startProcessMonitor();
			if (this.enableStatusReader) {
				startStatusReader();
			}
			this.stdOutReader = reader;
			getTaskExecutor().execute(this);
		}
		catch (IOException e) {
			throw new MessagingException("Failed to exec tail command: '" + this.command + "'", e);
		}
	}


	/**
	 * Runs a thread that waits for the Process result.
	 */
	private void startProcessMonitor() {
		getTaskExecutor()
				.execute(() -> {
					Process process = OSDelegatingFileTailingMessageProducer.this.nativeTailProcess;
					if (process == null) {
						logger.debug("Process destroyed before starting process monitor");
						return;
					}

					int result;
					try {
						logger.debug(() -> "Monitoring process " + process);
						result = process.waitFor();
						logger.info(() -> "tail process terminated with value " + result);
					}
					catch (InterruptedException ex) {
						Thread.currentThread().interrupt();
						logger.error(ex, "Interrupted - stopping adapter");
						stop();
					}
					finally {
						destroyProcess();
					}
					if (isRunning()) {
						logger.info(() -> "Restarting tail process in " + getMissingFileDelay() + " milliseconds");
						getTaskScheduler()
								.schedule(this::runExec, new Date(System.currentTimeMillis() + getMissingFileDelay()));
					}
				});
	}

	/**
	 * Runs a thread that reads stderr - on some platforms status messages
	 * (file not available, rotations etc) are sent to stderr.
	 */
	private void startStatusReader() {
		Process process = this.nativeTailProcess;
		if (process == null) {
			logger.debug("Process destroyed before starting stderr reader");
			return;
		}
		getTaskExecutor()
				.execute(() -> {
					BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String statusMessage;
					logger.debug("Reading stderr");
					try {
						while ((statusMessage = errorReader.readLine()) != null) {
							publish(statusMessage);
							logger.trace(statusMessage);
						}
					}
					catch (IOException e1) {
						logger.debug(e1, "Exception on tail error reader");
					}
					finally {
						try {
							errorReader.close();
						}
						catch (IOException e2) {
							logger.debug(e2, "Exception while closing stderr");
						}
					}
				});
	}

	/**
	 * Reads lines from stdout and sends in a message to the output channel.
	 */
	@Override
	public void run() {
		String line;
		try {
			logger.debug("Reading stdout");
			while ((line = this.stdOutReader.readLine()) != null) {
				send(line);
			}
		}
		catch (IOException ex) {
			logger.debug(ex, "Exception on tail reader");
			try {
				this.stdOutReader.close();
			}
			catch (IOException e1) {
				logger.debug(e1, "Exception while closing stdout");
			}
			destroyProcess();
		}
	}

}
