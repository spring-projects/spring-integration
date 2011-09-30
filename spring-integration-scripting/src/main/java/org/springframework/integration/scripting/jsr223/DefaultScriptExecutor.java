/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.scripting.jsr223;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.ClassUtils;

/**
 * Executes JSR223 scripts
 * 
 * @author David Turanski
 * @since 2.1
 */
public class DefaultScriptExecutor implements ScriptExecutor {

	static {
		if (ClassUtils.isPresent("org.jruby.embed.jsr223.JRubyEngine", System.class.getClassLoader())) {
			System.setProperty("org.jruby.embed.localvariable.behavior", "transient");
			System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");
		}
	}

	private static Log log = LogFactory.getLog(DefaultScriptExecutor.class);

	private final ScriptEngine scriptEngine;

	/**
	 * Set the language name (JSR233 alias)
	 * @param language
	 */
	public DefaultScriptExecutor(String language) {
		ScriptEngineManager manager = new ScriptEngineManager();

		scriptEngine = manager.getEngineByName(language);
		if (log.isDebugEnabled()) {
			log.debug("using script engine : " + scriptEngine.getFactory().getEngineName());
		}
	}

	public Object executeScript(ScriptSource scriptSource) {
		return this.executeScript(scriptSource, null);
	}

	public synchronized Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Object obj = null;
		try {

			if (variables != null) {
				for (Entry<String, Object> entry : variables.entrySet()) {
					scriptEngine.put(entry.getKey(), entry.getValue());
				}
			}

			String script = scriptSource.getScriptAsString();

			Date start = new Date();
			if (log.isDebugEnabled()) {
				log.debug("executing script: " + script + " on instance " + scriptEngine.hashCode());
			}

			obj = scriptEngine.eval(script);

			if (log.isDebugEnabled()) {
				log.debug("script executed in " + (new Date().getTime() - start.getTime()) + " ms");
			}

		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		catch (ScriptException e) {
			throw new RuntimeException(e);
		}

		return obj;
	}

}
