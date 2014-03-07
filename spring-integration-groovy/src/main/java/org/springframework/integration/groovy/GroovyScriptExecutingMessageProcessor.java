/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.groovy;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.scripting.AbstractScriptExecutingMessageProcessor;
import org.springframework.integration.scripting.ScriptVariableGenerator;
import org.springframework.messaging.Message;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import groovy.lang.Binding;
import groovy.lang.GString;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

/**
 * The {@link org.springframework.integration.handler.MessageProcessor} implementation
 * to evaluate Groovy scripts.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Stefan Reuter
 * @author Artem Bilan
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object> {

	private final VariableBindingGroovyObjectCustomizerDecorator customizerDecorator =
			new VariableBindingGroovyObjectCustomizerDecorator();

	private final Lock scriptLock = new ReentrantLock();

	private volatile ScriptSource scriptSource;

	private volatile GroovyClassLoader groovyClassLoader = new GroovyClassLoader(ClassUtils.getDefaultClassLoader());

	private volatile Class<?> scriptClass;

	/**
	 * Create a processor for the given {@link ScriptSource} that will use a
	 * DefaultScriptVariableGenerator.
	 *
	 * @param scriptSource The script source.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		super();
		this.scriptSource = scriptSource;
	}

	/**
	 * Create a processor for the given {@link ScriptSource} that will use the provided
	 * ScriptVariableGenerator.
	 *
	 * @param scriptSource The script source.
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource, ScriptVariableGenerator scriptVariableGenerator) {
		super(scriptVariableGenerator);
		this.scriptSource = scriptSource;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		super.setBeanClassLoader(classLoader);
		this.groovyClassLoader = new GroovyClassLoader(classLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		super.setBeanFactory(beanFactory);
		if (beanFactory != null && beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
		}
	}

	/**
	 * Sets a {@link GroovyObjectCustomizer} for this processor.
	 *
	 * @param customizer The customizer.
	 */
	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizerDecorator.setCustomizer(customizer);
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		return this.scriptSource;
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) throws Exception {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		this.parseScriptIfNecessary(scriptSource);
		Object result = this.execute(variables);
		return (result instanceof GString) ? result.toString() : result;
	}


	private void parseScriptIfNecessary(ScriptSource scriptSource) throws Exception {
		if (this.scriptClass == null || scriptSource.isModified()) {
			this.scriptLock.lockInterruptibly();
			try {
				// synchronized double check
				if (this.scriptClass == null || scriptSource.isModified()) {
					this.scriptClass = this.groovyClassLoader.parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());
				}
			}
			finally {
				this.scriptLock.unlock();
			}
		}
	}

	private Object execute(Map<String, Object> variables) throws ScriptCompilationException {
		try {
			GroovyObject goo = (GroovyObject) this.scriptClass.newInstance();

			VariableBindingGroovyObjectCustomizerDecorator groovyObjectCustomizer =
					new BindingOverwriteGroovyObjectCustomizerDecorator(new BeanFactoryFallbackBinding(variables));
			groovyObjectCustomizer.setCustomizer(this.customizerDecorator);

			if (goo instanceof Script) {
				// Allow metaclass and other customization.
				groovyObjectCustomizer.customize(goo);
				// A Groovy script, probably creating an instance: let's execute it.
				return ((Script) goo).run();
			}
			else {
				// An instance of the scripted class: let's return it as-is.
				return goo;
			}
		}
		catch (InstantiationException ex) {
			throw new ScriptCompilationException(
					this.scriptSource, "Could not instantiate Groovy script class: " + this.scriptClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new ScriptCompilationException(
					this.scriptSource, "Could not access Groovy script constructor: " + this.scriptClass.getName(), ex);
		}
	}

	private class BeanFactoryFallbackBinding extends Binding {

		private BeanFactoryFallbackBinding(Map<?, ?> variables) {
			super(variables);
		}

		@Override
		public Object getVariable(String name) {
			try {
				return super.getVariable(name);
			}
			catch (MissingPropertyException e) {
			// Original {@link Binding} doesn't have 'variable' for the given 'name'.
			// Try to resolve it as 'bean' from the given <code>beanFactory</code>.
			}

			if (GroovyScriptExecutingMessageProcessor.this.beanFactory == null) {
				throw new MissingPropertyException(name, this.getClass());
			}

			try {
				return GroovyScriptExecutingMessageProcessor.this.beanFactory.getBean(name);
			}
			catch (NoSuchBeanDefinitionException e) {
				throw new MissingPropertyException(name, this.getClass(), e);
			}
		}

	}

}
