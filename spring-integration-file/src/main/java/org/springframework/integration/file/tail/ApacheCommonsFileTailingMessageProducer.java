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

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

/**
 * File tailer that delegates to the Apache Commons Tailer.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ApacheCommonsFileTailingMessageProducer extends FileTailingMessageProducerSupport
		implements TailerListener {

	private volatile Tailer tailer;

	private volatile long pollingDelay = 1000;

	private volatile boolean end = true;

	private volatile boolean reopen = false;

	/**
	 * The delay between checks of the file for new content in milliseconds.
	 * @param pollingDelay The delay.
	 */
	public void setPollingDelay(long pollingDelay) {
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
		Tailer tailer = new Tailer(this.getFile(), this, this.pollingDelay, this.end, this.reopen);
		this.getTaskExecutor().execute(tailer);
		this.tailer = tailer;
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.tailer.stop();
	}

	@Override
	public void init(Tailer tailer) {
	}

	@Override
	public void fileNotFound() {
		this.publish("File not found:" + this.getFile().getAbsolutePath());
		try {
			Thread.sleep(this.getMissingFileDelay());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void fileRotated() {
		this.publish("File rotated:" + this.getFile().getAbsolutePath());
	}

	@Override
	public void handle(String line) {
		send(line);
	}

	@Override
	public void handle(Exception ex) {
		this.publish(ex.getMessage());
	}

}
