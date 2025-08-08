/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.scripting.jsr223;

/**
 * A {@link DefaultScriptExecutor} extension for Ruby scripting support.
 * It is present here only for the reason to populate
 * {@code org.jruby.embed.localvariable.behavior} and
 * {@code org.jruby.embed.localcontext.scope} system properties.
 * May be revised in the future.
 *
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class RubyScriptExecutor extends DefaultScriptExecutor {

	static {
		System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
		System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
	}

	public RubyScriptExecutor() {
		super("ruby");
	}

}
