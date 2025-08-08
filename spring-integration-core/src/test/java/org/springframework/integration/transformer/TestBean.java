/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.context.Lifecycle;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestBean implements Lifecycle {

	private boolean running;

	public String upperCase(String input) {
		return input.toUpperCase();
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
