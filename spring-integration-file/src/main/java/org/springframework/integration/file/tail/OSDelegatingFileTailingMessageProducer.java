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
import java.util.Date;

import org.springframework.messaging.MessagingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

	private volatile TaskScheduler scheduler;

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
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				runExec();
			}
		});
	}

	@Override
	protected void doStop() {
		super.doStop();
		destroyProcess();
	}

	private void destroyProcess() {
		Process process = this.process;
		if (process != null) {
			process.destroy();
			this.process = null;
		}
	}

	/**
	 * Exec the native tail process.
	 */
	private void runExec() {
		this.destroyProcess();
		if (logger.isInfoEnabled()) {
			logger.info("Starting tail process");
		}
		try {
			Process process = Runtime.getRuntime().exec(this.command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			this.process = process;
			this.startProcessMonitor();
			this.startStatusReader();
			this.reader = reader;
			this.getTaskExecutor().execute(this);
		}
		catch (IOException e) {
			throw new MessagingException("Failed to exec tail command: '" + this.command + "'", e);
		}
	}

	private TaskScheduler getRequiredTaskScheduler() {
		if (this.scheduler == null) {
			TaskScheduler taskScheduler = super.getTaskScheduler();
			if (taskScheduler == null) {
				ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
				scheduler.initialize();
				taskScheduler = scheduler;
			}
			this.scheduler = taskScheduler;
		}
		return this.scheduler;
	}
	/**
	 * Runs a thread that waits for the Process result.
	 */
	private void startProcessMonitor() {
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				Process process = OSDelegatingFileTailingMessageProducer.this.process;
				if (process == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Process destroyed before starting process monitor");
					}
					return;
				}

				int result = Integer.MIN_VALUE;
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Monitoring process " + process);
					}
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
					destroyProcess();
				}
				if (isRunning()) {
					if (logger.isInfoEnabled()) {
						logger.info("Restarting tail process in " + getMissingFileDelay() + " milliseconds");
					}
					getRequiredTaskScheduler().schedule(new Runnable() {

						@Override
						public void run() {
							runExec();
						}
					}, new Date(System.currentTimeMillis() + getMissingFileDelay()));
				}
			}
		});
	}

	/**
	 * Runs a thread that reads stderr - on some platforms status messages
	 * (file not available, rotations etc) are sent to stderr.
	 */
	private void startStatusReader() {
		Process process = this.process;
		if (process == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Process destroyed before starting stderr reader");
			}
			return;
		}
		final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		this.getTaskExecutor().execute(new Runnable() {

			@Override
			public void run() {
				String statusMessage;
				if (logger.isDebugEnabled()) {
					logger.debug("Reading stderr");
				}
				try {
					while ((statusMessage = errorReader.readLine()) != null) {
						publish(statusMessage);
						if (logger.isTraceEnabled()) {
							logger.trace(statusMessage);
						}
					}
				}
				catch (IOException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Exception on tail error reader", e);
					}
				}
				finally {
					try {
						errorReader.close();
					}
					catch (IOException e) {
						if (logger.isDebugEnabled()) {
							logger.debug("Exception while closing stderr", e);
						}
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
		String line;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Reading stdout");
			}
			while ((line = this.reader.readLine()) != null) {
				this.send(line);
			}
		}
		catch (IOException e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Exception on tail reader", e);
			}
			try {
				this.reader.close();
			}
			catch (IOException e1) {
				if (logger.isDebugEnabled()) {
					logger.debug("Exception while closing stdout", e);
				}
			}
			this.destroyProcess();
		}
	}


}
