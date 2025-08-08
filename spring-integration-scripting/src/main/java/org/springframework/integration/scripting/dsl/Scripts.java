/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.scripting.dsl;

import org.springframework.core.io.Resource;

/**
 * The factory for Dynamic Language Scripts (Groovy, Ruby, Python, JavaScript etc.).
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class Scripts {

	/**
	 * The factory method to produce {@link ScriptSpec} based on the {@link Resource}.
	 * The {@link Resource} must represent the real file and can be injected like:
	 * <pre class="code">
	 *  &#064;Value("com/my/project/scripts/FilterScript.groovy")
	 *  private Resource filterScript;
	 * </pre>
	 * @param scriptResource the script file {@link Resource}
	 * @return the ScriptSpec instance
	 */
	public static ScriptSpec processor(Resource scriptResource) {
		return new ScriptSpec(scriptResource);
	}

	/**
	 * The factory method to produce {@link ScriptSpec} based on the script file location.
	 * @param scriptLocation the path to the script file.
	 *               {@code file:}, {@code ftp:}, {@code s3:} etc.
	 *               The {@code classpath:} can be omitted.
	 * @return the ScriptSpec instance
	 */
	public static ScriptSpec processor(String scriptLocation) {
		return new ScriptSpec(scriptLocation);
	}

	/**
	 * Factory for the {@link ScriptMessageSourceSpec} based on the {@link Resource}.
	 * The {@link Resource} must represent the real file and can be injected like:
	 * <pre class="code">
	 *  &#064;Value("com/my/project/scripts/FilterScript.groovy")
	 *  private Resource filterScript;
	 * </pre>
	 * @param scriptResource the script {@link Resource}
	 * @return the {@link ScriptMessageSourceSpec}
	 */
	public static ScriptMessageSourceSpec messageSource(Resource scriptResource) {
		return new ScriptMessageSourceSpec(scriptResource);
	}

	/**
	 * Factory for the {@link ScriptMessageSourceSpec} based on the script location.
	 * @param scriptLocation the path to the script file.
	 *         {@code file:}, {@code ftp:}, {@code s3:} etc.
	 *         The {@code classpath:} can be omitted.
	 * @return the {@link ScriptMessageSourceSpec}
	 */
	public static ScriptMessageSourceSpec messageSource(String scriptLocation) {
		return new ScriptMessageSourceSpec(scriptLocation);
	}

	private Scripts() {
	}

}
