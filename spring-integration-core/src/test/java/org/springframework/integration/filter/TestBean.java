/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.filter;

import org.springframework.context.Lifecycle;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestBean implements Lifecycle {

	private boolean running;

	public boolean acceptStringWithMoreThanThreeChars(String s) {
		return s.length() > 3;
	}

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

}
