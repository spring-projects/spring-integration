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

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.jspecify.annotations.Nullable;

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

	private volatile @Nullable Tailer tailer;

	private volatile @Nullable ScheduledFuture<?> restartFuture;

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
		runTailer();
	}

	/**
	 * Build and run a new {@link Tailer}, arranging for it to be restarted (after
	 * {@link #getMissingFileDelay()}) should its background thread terminate while this
	 * adapter is still active. The underlying {@code commons-io} {@link Tailer} can leave
	 * its reader in a broken state and let the thread die if a file rotation is observed
	 * concurrently with the replacement file being briefly absent; without a restart, such
	 * an event would otherwise silently stop message production forever.
	 */
	private void runTailer() {
		Tailer theTailer =
				Tailer.builder()
						.setDelayDuration(this.pollingDelay)
						.setTailFromEnd(this.end)
						.setReOpen(this.reopen)
						.setFile(getFile())
						.setTailerListener(this.tailerListener)
						.setStartThread(false)
						.get();
		this.tailer = theTailer;
		getTaskExecutor().execute(() -> {
			theTailer.run();
			if (isActive()) {
				long missingFileDelay = getMissingFileDelay();
				publish("Tailer thread has died; restarting in " + missingFileDelay + " milliseconds");
				this.restartFuture =
						getTaskScheduler().schedule(this::runTailer, Instant.now().plusMillis(missingFileDelay));
			}
		});
	}

	@Override
	protected void doStop() {
		super.doStop();
		ScheduledFuture<?> restartFutureToCancel = this.restartFuture;
		if (restartFutureToCancel != null) {
			restartFutureToCancel.cancel(true);
		}
		Tailer tailerToClose = this.tailer;
		if (tailerToClose != null) {
			tailerToClose.close();
		}
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
			String message = ex.getMessage();
			publish(message != null ? message : ex.toString());
		}

	}

}
