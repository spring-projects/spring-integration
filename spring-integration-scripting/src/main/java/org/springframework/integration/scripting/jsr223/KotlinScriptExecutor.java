/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.scripting.jsr223;

import javax.script.Bindings;
import javax.script.ScriptEngine;

import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory;


/**
 * An {@link AbstractScriptExecutor} for the Kotlin scripts support.
 * Uses {@link KotlinJsr223JvmLocalScriptEngineFactory} directly since there is
 * no {@code META-INF/services/javax.script.ScriptEngineFactory} file in CLASSPATH.
 * Also sets an {@code idea.use.native.fs.for.win} system property to {@code false}
 * to disable a native engine discovery for Windows: bay be resolved in the future Kotlin versions.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class KotlinScriptExecutor extends AbstractScriptExecutor {

	static {
		System.setProperty("idea.use.native.fs.for.win", "false");
	}

	public KotlinScriptExecutor() {
		super(new KotlinJsr223JvmLocalScriptEngineFactory().getScriptEngine());
	}

	@Override
	protected Object postProcess(Object result, ScriptEngine scriptEngine, String script, Bindings bindings) {
		return result;
	}

}
