/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.scripting.dsl;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.RefreshableResourceScriptSource;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.integration.scripting.config.ScriptExecutingProcessorFactory;
import org.springframework.integration.scripting.jsr223.ScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.jsr223.ScriptExecutorFactory;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The adapter {@link MessageProcessor} around {@link AbstractScriptExecutingMessageProcessor}.
 * Delegates to the {@code GroovyScriptExecutingMessageProcessor}, if provided {@link #lang}
 * matches to {@code groovy} string and {@code spring-integration-groovy} jar is in classpath.
 * Otherwise, to the {@link ScriptExecutingMessageProcessor}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
class DslScriptExecutingMessageProcessor
		implements MessageProcessor<Object>, InitializingBean, ApplicationContextAware, BeanClassLoaderAware {

	private Resource script;

	private String location;

	private String lang;

	private long refreshCheckDelay = -1;

	private ScriptVariableGenerator variableGenerator;

	private ApplicationContext applicationContext;

	private AbstractScriptExecutingMessageProcessor<?> delegate;

	private ClassLoader classLoader;

	DslScriptExecutingMessageProcessor(Resource script) {
		this.script = script;
	}

	DslScriptExecutingMessageProcessor(String location) {
		this.location = location;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setRefreshCheckDelay(Long refreshCheckDelay) {
		this.refreshCheckDelay = refreshCheckDelay;
	}

	public void setVariableGenerator(ScriptVariableGenerator variableGenerator) {
		this.variableGenerator = variableGenerator;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void afterPropertiesSet() {
		if (StringUtils.hasText(this.location)) {
			this.script = this.applicationContext.getResource(this.location);
		}

		if (!StringUtils.hasText(this.lang)) {
			String scriptFilename = this.script.getFilename();
			Assert.hasText(scriptFilename,
					() -> "Either 'lang' or file extension must be provided for script: " + this.script);
			this.lang = ScriptExecutorFactory.deriveLanguageFromFileExtension(scriptFilename);
		}

		ScriptSource scriptSource = new RefreshableResourceScriptSource(this.script, this.refreshCheckDelay);

		if (this.applicationContext.containsBean(ScriptExecutingProcessorFactory.BEAN_NAME)) {
			ScriptExecutingProcessorFactory processorFactory =
					this.applicationContext.getBean(ScriptExecutingProcessorFactory.BEAN_NAME,
							ScriptExecutingProcessorFactory.class);
			this.delegate = processorFactory.createMessageProcessor(this.lang, scriptSource, this.variableGenerator);
		}
		else {
			this.delegate = new ScriptExecutingMessageProcessor(scriptSource, this.variableGenerator,
					ScriptExecutorFactory.getScriptExecutor(this.lang));
		}

		this.delegate.setBeanFactory(this.applicationContext);
		this.delegate.setBeanClassLoader(this.classLoader);
	}

	@Override
	public Object processMessage(Message<?> message) {
		return this.delegate.processMessage(message);
	}

}
