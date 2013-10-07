/*
 * Copyright 2013 the original author or authors.
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
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.integration.scripting.ScriptExecutor;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;
import org.springframework.util.ClassUtils;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.Script;

/**
 * {@link ScriptExecutor} implementation for Groovy scripts to parse and execute scripts via Groovy own engine.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class GroovyScriptExecutor implements ScriptExecutor, BeanFactoryAware, BeanClassLoaderAware {

	private final GroovyObjectCustomizer groovyObjectCustomizer;

	private final Lock scriptLock = new ReentrantLock();

	private volatile GroovyClassLoader groovyClassLoader = new GroovyClassLoader(ClassUtils.getDefaultClassLoader());

	private volatile Class<?> scriptClass;


	public GroovyScriptExecutor() {
		this(null);
	}

	public GroovyScriptExecutor(GroovyObjectCustomizer groovyObjectCustomizer) {
		this.groovyObjectCustomizer = groovyObjectCustomizer;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.groovyClassLoader = new GroovyClassLoader(classLoader);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
		}
	}

	@Override
	public Object executeScript(ScriptSource scriptSource) {
		return this.executeScript(scriptSource, null);
	}

	@Override
	public Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		try {
			this.parseScriptIfNecessary(scriptSource);

			return this.execute(this.scriptClass, variables);
		}
		catch (Exception e) {
			throw new ScriptCompilationException(
					scriptSource, "Could not execute Groovy script: " + this.scriptClass.getName(), e);
		}
	}

	private void parseScriptIfNecessary(ScriptSource scriptSource) throws Exception {
		if (this.scriptClass == null || scriptSource.isModified()) {
			// For double check in the locking block
			this.scriptClass = null;
			try {
				this.scriptLock.lockInterruptibly();
				try {
					// Synchronized double check
					if (this.scriptClass == null) {
						this.scriptClass = this.groovyClassLoader.parseClass(
								scriptSource.getScriptAsString(), scriptSource.suggestedClassName());
					}
				}
				finally {
					this.scriptLock.unlock();
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw e;
			}
		}
	}

	private Object execute(Class scriptClass, Map<String, Object> variables) throws Exception {
		GroovyObject goo = (GroovyObject) scriptClass.newInstance();

		GroovyObjectCustomizer groovyObjectCustomizer = this.groovyObjectCustomizer;
		if (variables != null) {
			// Override empty Script.Binding with new one with 'variables'
			groovyObjectCustomizer = new BindingOverwriteGroovyObjectCustomizerDecorator(new Binding(variables));
			((VariableBindingGroovyObjectCustomizerDecorator) groovyObjectCustomizer).setCustomizer(this.groovyObjectCustomizer);
		}

		if (groovyObjectCustomizer != null) {
			// Allow metaclass and other customization.
			groovyObjectCustomizer.customize(goo);
		}

		if (goo instanceof Script) {
			// A Groovy script, probably creating an instance: let's execute it.
			return ((Script) goo).run();
		}
		else {
			// An instance of the scripted class: let's return it as-is.
			return goo;
		}
	}
}
