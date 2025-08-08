/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.tail;

import java.time.Duration;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;

/**
 * File tailer that delegates to the Apache Commons Tailer.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class ApacheCommonsFileTailingMessageProducer extends FileTailingMessageProducerSupport {

	private final TailerListener tailerListener = new IntegrationTailerListener();

	private Duration pollingDelay = Duration.ofSeconds(1);

	private boolean end = true;

	private boolean reopen = false;

	private volatile Tailer tailer;

	/**
	 * The delay between checks of the file for new content in milliseconds.
	 * @param pollingDelay The delay.
	 */
	public void setPollingDelay(long pollingDelay) {
		setPollingDelayDuration(Duration.ofMillis(pollingDelay));
	}

	/**
	 * The delay between checks of the file for new content in {@link Duration}.
	 * @param pollingDelay The delay duration.
	 * @since 6.2
	 */
	public void setPollingDelayDuration(Duration pollingDelay) {
		this.pollingDelay = pollingDelay;
	}

	/**
	 * If true, tail from the end of the file, otherwise
	 * include all lines from the beginning. Default true.
	 * @param end true or false
	 */
	public void setEnd(boolean end) {
		this.end = end;
	}

	/**
	 * If true, close and reopen the file between reading chunks;
	 * default false.
	 * @param reopen true or false.
	 */
	public void setReopen(boolean reopen) {
		this.reopen = reopen;
	}

	@Override
	public String getComponentType() {
		return super.getComponentType() + " (Apache)";
	}

	@Override
	protected void doStart() {
		super.doStart();
		Tailer theTailer =
				Tailer.builder()
						.setDelayDuration(this.pollingDelay)
						.setTailFromEnd(this.end)
						.setReOpen(this.reopen)
						.setFile(getFile())
						.setTailerListener(this.tailerListener)
						.setStartThread(false)
						.get();
		getTaskExecutor().execute(theTailer);
		this.tailer = theTailer;
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.tailer.close();
	}

	private class IntegrationTailerListener extends TailerListenerAdapter {

		IntegrationTailerListener() {
		}

		@Override
		public void fileNotFound() {
			publish("File not found: " + getFile().getAbsolutePath());
			try {
				Thread.sleep(getMissingFileDelay());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void fileRotated() {
			publish("File rotated: " + getFile().getAbsolutePath());
		}

		@Override
		public void handle(String line) {
			send(line);
		}

		@Override
		public void handle(Exception ex) {
			publish(ex.getMessage());
		}

	}

}
