/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.groovy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
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
import org.springframework.util.StringUtils;

import groovy.lang.Binding;
import groovy.lang.GString;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * The {@link org.springframework.integration.handler.MessageProcessor} implementation
 * to evaluate Groovy scripts.
 *
 * @author Dave Syer
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Stefan Reuter
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class GroovyScriptExecutingMessageProcessor extends AbstractScriptExecutingMessageProcessor<Object>
		implements InitializingBean {

	private final VariableBindingGroovyObjectCustomizerDecorator customizerDecorator =
			new VariableBindingGroovyObjectCustomizerDecorator();

	private final Lock scriptLock = new ReentrantLock();

	private ScriptSource scriptSource;

	private GroovyClassLoader groovyClassLoader =
			AccessController.doPrivileged((PrivilegedAction<GroovyClassLoader>)
					() -> new GroovyClassLoader(ClassUtils.getDefaultClassLoader()));

	private boolean compileStatic;

	private CompilerConfiguration compilerConfiguration;

	private volatile Class<?> scriptClass;

	/**
	 * Create a processor for the given {@link ScriptSource} that will use a
	 * DefaultScriptVariableGenerator.
	 * @param scriptSource The script source.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource) {
		this.scriptSource = scriptSource;
	}

	/**
	 * Create a processor for the given {@link ScriptSource} that will use the provided
	 * ScriptVariableGenerator.
	 * @param scriptSource The script source.
	 * @param scriptVariableGenerator The variable generator.
	 */
	public GroovyScriptExecutingMessageProcessor(ScriptSource scriptSource,
			ScriptVariableGenerator scriptVariableGenerator) {
		super(scriptVariableGenerator);
		this.scriptSource = scriptSource;
	}

	/**
	 * Sets a {@link GroovyObjectCustomizer} for this processor.
	 * @param customizer The customizer.
	 */
	public void setCustomizer(GroovyObjectCustomizer customizer) {
		this.customizerDecorator.setCustomizer(customizer);
	}

	/**
	 * Specify the {@code boolean} flag to indicate if the {@link GroovyClassLoader}'s compiler
	 * should be customised for the {@link CompileStatic} hint for the provided script.
	 * <p> More compiler options can be provided via {@link #setCompilerConfiguration(CompilerConfiguration)}
	 * overriding this flag.
	 * @param compileStatic the compile static {@code boolean} flag.
	 * @see CompileStatic
	 * @since 4.3
	 */
	public void setCompileStatic(boolean compileStatic) {
		this.compileStatic = compileStatic;
	}

	/**
	 * Specify the {@link CompilerConfiguration} options to customize the Groovy script compilation.
	 * For example the {@link CompileStatic} and {@link org.codehaus.groovy.control.customizers.ImportCustomizer}
	 * are the most popular options.
	 * @param compilerConfiguration the Groovy script compiler options to use.
	 * @see CompileStatic
	 * @see GroovyClassLoader
	 * @since 4.3
	 */
	public void setCompilerConfiguration(CompilerConfiguration compilerConfiguration) {
		this.compilerConfiguration = compilerConfiguration;
	}

	@Override
	protected ScriptSource getScriptSource(Message<?> message) {
		return this.scriptSource;
	}

	@Override
	public void afterPropertiesSet() {
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
		}

		CompilerConfiguration compilerConfig;
		if (this.compilerConfiguration == null && this.compileStatic) {
			compilerConfig = new CompilerConfiguration();
			compilerConfig.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic.class));
		}
		else {
			compilerConfig = this.compilerConfiguration;
		}

		this.groovyClassLoader =
				AccessController.doPrivileged((PrivilegedAction<GroovyClassLoader>)
						() -> new GroovyClassLoader(getBeanClassLoader(), compilerConfig));
	}

	@Override
	protected Object executeScript(ScriptSource scriptSource, Map<String, Object> variables) {
		Assert.notNull(scriptSource, "scriptSource must not be null");
		parseScriptIfNecessary(scriptSource);
		Object result = execute(variables);
		return (result instanceof GString) ? result.toString() : result;
	}


	private void parseScriptIfNecessary(ScriptSource scriptSource) {
		if (this.scriptClass == null || scriptSource.isModified()) {
			this.scriptLock.lock();
			try {
				// synchronized double check
				if (this.scriptClass == null || scriptSource.isModified()) {
					String className = scriptSource.suggestedClassName();
					try {
						String scriptAsString = scriptSource.getScriptAsString();
						if (StringUtils.hasText(className)) {
							this.scriptClass = this.groovyClassLoader.parseClass(scriptAsString, className);
						}
						else {
							this.scriptClass = this.groovyClassLoader.parseClass(scriptAsString);
						}
					}
					catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
			finally {
				this.scriptLock.unlock();
			}
		}
	}

	private Object execute(Map<String, Object> variables) throws ScriptCompilationException {
		GroovyObject goo = (GroovyObject) BeanUtils.instantiateClass(this.scriptClass);

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

	private final class BeanFactoryFallbackBinding extends Binding {

		BeanFactoryFallbackBinding(Map<?, ?> variables) {
			super(variables);
		}

		@Override
		public Object getVariable(String name) {
			try {
				return super.getVariable(name);
			}
			catch (MissingPropertyException ex) {
				// Original {@link Binding} doesn't have 'variable' for the given 'name'.
				// Try to resolve it as 'bean' from the given <code>beanFactory</code>.
			}

			BeanFactory beanFactory = getBeanFactory();
			if (beanFactory == null) {
				throw new MissingPropertyException(name, getClass());
			}
			try {
				return beanFactory.getBean(name);
			}
			catch (NoSuchBeanDefinitionException e) {
				throw new MissingPropertyException(name, getClass(), e);
			}
		}

	}

}
