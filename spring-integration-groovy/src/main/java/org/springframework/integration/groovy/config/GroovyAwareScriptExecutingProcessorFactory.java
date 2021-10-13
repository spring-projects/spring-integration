/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.groovy.config;

import org.springframework.integration.groovy.GroovyScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.scripting.config.ScriptExecutingProcessorFactory;
import org.springframework.scripting.ScriptSource;

/**
 * The factory to create {@link GroovyScriptExecutingMessageProcessor} instances
 * if provided {@code language == "groovy"}, otherwise delegates to the super class.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class GroovyAwareScriptExecutingProcessorFactory extends ScriptExecutingProcessorFactory {

	@Override
	public AbstractScriptExecutingMessageProcessor<?> createMessageProcessor(String language,
			ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {

		if ("groovy".equalsIgnoreCase(language)) {
			return new GroovyScriptExecutingMessageProcessor(scriptSource, scriptVariableGenerator);
		}
		else {
			return super.createMessageProcessor(language, scriptSource, scriptVariableGenerator);
		}
	}

}
