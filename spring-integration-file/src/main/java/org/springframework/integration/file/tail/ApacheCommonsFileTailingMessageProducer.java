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

	private volatile long missingFileDelay = 5000;

	public void setPollingDelay(long pollingDelay) {
		this.pollingDelay = pollingDelay;
	}

	public void setMissingFileDelay(long missingFileDelay) {
		this.missingFileDelay = missingFileDelay;
	}

	@Override
	public String getComponentType() {
		return super.getComponentType() + " (Apache)";
	}

	@Override
	protected void doStart() {
		super.doStart();
		Tailer tailer = new Tailer(this.getFile(), this, this.pollingDelay);
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
			Thread.sleep(this.missingFileDelay);
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
