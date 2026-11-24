/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
