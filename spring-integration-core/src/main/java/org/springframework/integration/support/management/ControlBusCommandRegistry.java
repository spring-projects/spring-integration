/*
 * Copyright 2024 the original author or authors.
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

package org.springframework.integration.support.management;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CustomizableThreadCreator;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A global component to serve Control Bus command and respective SpEL expression relationships.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see ControlBusMethodFilter
 */
public class ControlBusCommandRegistry
		implements ApplicationContextAware, SmartInitializingSingleton, DestructionAwareBeanPostProcessor {

	private static final Pattern COMMAND_PATTERN =
			Pattern.compile("^@?'?(?<beanName>.[^']+)'?\\.(?<methodName>[a-zA-Z0-9_]+)\\(?\\)?$");

	private static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private static final ControlBusMethodFilter CONTROL_BUS_METHOD_FILTER = new ControlBusMethodFilter();

	private final Map<String, Map<CommandMethod, Expression>> controlBusCommands = new HashMap<>();

	private boolean eagerInitialization;

	private ApplicationContext applicationContext;

	private boolean initialized;

	/**
	 * Set to {@code true} to turn on Control Bus commands loading after application context initialization.
	 * @param eagerInitialization true to initialize this registry eagerly.
	 */
	public void setEagerInitialization(boolean eagerInitialization) {
		this.eagerInitialization = eagerInitialization;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterSingletonsInstantiated() {
		if (this.eagerInitialization) {
			this.applicationContext.getBeansOfType(null)
					.forEach(this::registerControlBusCommands);
			this.initialized = true;
		}
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// Only for dynamically created beans. Others are registered by afterSingletonsInstantiated()
		if (this.initialized) {
			registerControlBusCommands(beanName, bean);
		}
		return bean;
	}

	/**
	 * Manually register Control Bus commands (if any) for a specific bean.
	 * The bean must be in instance of {@link Lifecycle}, or {@link CustomizableThreadCreator},
	 * or marked with the {@link ManagedResource}, or {@link IntegrationManagedResource} annotation.
	 * @param beanName the bean name for registration
	 * @param bean the bean for registration
	 */
	public void registerControlBusCommands(String beanName, Object bean) {
		Class<?> beanClass = bean.getClass();
		if (bean instanceof Lifecycle || bean instanceof CustomizableThreadCreator
				|| AnnotationUtils.findAnnotation(beanClass, ManagedResource.class) != null
				|| AnnotationUtils.findAnnotation(beanClass, IntegrationManagedResource.class) != null) {

			ReflectionUtils.doWithMethods(beanClass, method -> populateExpressionForCommand(beanName, method),
					CONTROL_BUS_METHOD_FILTER);
		}
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		this.controlBusCommands.remove(beanName);
	}

	/**
	 * Return registered Control Bus commands.
	 * @return registered commands.
	 */
	public Map<String, Map<CommandMethod, String>> getCommands() {
		Map<String, Map<CommandMethod, String>> commands = new HashMap<>(this.controlBusCommands.size());
		for (Map.Entry<String, Map<CommandMethod, Expression>> beanEntry : this.controlBusCommands.entrySet()) {
			Map<CommandMethod, String> commandEntries =
					beanEntry.getValue()
							.entrySet()
							.stream()
							.collect(Collectors.toMap(Map.Entry::getKey,
									(commandEntry) -> commandEntry.getValue().getExpressionString()));
			commands.put(beanEntry.getKey(), commandEntries);
		}
		return Collections.unmodifiableMap(commands);
	}

	/**
	 * Obtain a SpEL expression for the command to call with parameter types.
	 * The command must be in format {@code beanName.methodName}.
	 * (Or {@code @beanName.methodName()} for backward compatibility with simple expressions.)
	 * If {@code beanName} is a complex literal, it has to be wrapped into single quotes,
	 * e.g. {@code 'some.complex.bean-name'}.
	 * The target method to call must fit into the {@link ControlBusMethodFilter} requirements
	 * and match with the provided parameter types.
	 * @param command the command to call.
	 * @param parameterTypes the parameter types for the target method to call.
	 * @return the SpEL expression for the provided command.
	 * @throws IllegalArgumentException if provided command does not match to Control Bus requirements
	 * or target method contract.
	 */
	public Expression getExpressionForCommand(String command, Class<?>... parameterTypes) {
		Matcher matcher = COMMAND_PATTERN.matcher(command);
		Assert.isTrue(matcher.matches(),
				"The command must be in format 'beanName.methodName'. " +
						"Arguments must go to the 'controlBusArguments' message header.");

		String beanName = matcher.group("beanName");
		Assert.isTrue(this.applicationContext.containsBean(beanName),
				() -> "There is no registered bean for requested command: " + beanName);

		String methodName = matcher.group("methodName");

		CommandMethod commandMethod = new CommandMethod(beanName, methodName, parameterTypes);
		return populateCommandMethod(commandMethod, (key) -> buildExpressionForMethodToCall(commandMethod));
	}

	private void populateExpressionForCommand(String beanName, Method methodForCommand) {
		CommandMethod commandMethod =
				new CommandMethod(beanName, methodForCommand.getName(), methodForCommand.getParameterTypes());
		populateCommandMethod(commandMethod, (key) -> buildExpressionForMethodToCall(commandMethod, methodForCommand));
	}

	private Expression populateCommandMethod(CommandMethod commandMethod,
			Function<CommandMethod, Expression> mappingFunction) {

		String beanName = commandMethod.beanName;

		Map<CommandMethod, Expression> beanControlBusCommands =
				this.controlBusCommands.computeIfAbsent(beanName, (key) -> new HashMap<>());

		try {
			return beanControlBusCommands.computeIfAbsent(commandMethod, mappingFunction);
		}
		catch (IllegalArgumentException ex) {
			if (beanControlBusCommands.isEmpty()) {
				this.controlBusCommands.remove(beanName);
			}
			throw ex;
		}
	}

	private Expression buildExpressionForMethodToCall(CommandMethod commandMethod) {
		Object bean = this.applicationContext.getBean(commandMethod.beanName);
		Set<Method> candidates = MethodIntrospector.selectMethods(bean.getClass(),
				(ReflectionUtils.MethodFilter) method -> commandMethod.methodName.equals(method.getName()));

		Optional<Method> methodForCommand =
				candidates.stream()
						.filter(method ->
								areParameterTypesEqual(method.getParameterTypes(), commandMethod.parameterTypes))
						.findFirst();

		Assert.isTrue(methodForCommand.isPresent(),
				() -> "No method '%s' found in bean '%s' for parameter types '%s'"
						.formatted(commandMethod.methodName, bean, Arrays.toString(commandMethod.parameterTypes)));

		return buildExpressionForMethodToCall(commandMethod, methodForCommand.get());
	}

	private static Expression buildExpressionForMethodToCall(CommandMethod commandMethod, Method methodForCommand) {
		Assert.isTrue(CONTROL_BUS_METHOD_FILTER.matches(methodForCommand),
				() -> "The method '%s' is not valid Control Bus command".formatted(methodForCommand));

		populateDescriptionIntoCommand(commandMethod, methodForCommand);

		Class<?>[] parameterTypes = methodForCommand.getParameterTypes();
		StringBuilder expressionBuilder =
				new StringBuilder("@'")
						.append(commandMethod.beanName)
						.append("'.")
						.append(methodForCommand.getName())
						.append('(');
		for (int i = 0; i < parameterTypes.length; i++) {
			expressionBuilder.append('[').append(i).append("],");
		}
		if (parameterTypes.length > 0) {
			expressionBuilder.deleteCharAt(expressionBuilder.length() - 1);
		}
		String expression = expressionBuilder.append(')').toString();
		return EXPRESSION_PARSER.parseExpression(expression);
	}

	private static boolean areParameterTypesEqual(Class<?>[] lhsTypes, Class<?>[] rhsTypes) {
		if (lhsTypes.length == rhsTypes.length) {
			for (int i = 0; i < lhsTypes.length; i++) {
				if (!ClassUtils.isAssignable(lhsTypes[i], rhsTypes[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static void populateDescriptionIntoCommand(CommandMethod commandMethod, Method methodForCommand) {
		ManagedOperation managedOperation = AnnotationUtils.findAnnotation(methodForCommand, ManagedOperation.class);
		if (managedOperation != null) {
			commandMethod.description = managedOperation.description();
		}
		else {
			ManagedAttribute managedAttribute = AnnotationUtils.findAnnotation(methodForCommand, ManagedAttribute.class);
			if (managedAttribute != null) {
				commandMethod.description = managedAttribute.description();
			}
		}

		if (!StringUtils.hasText(commandMethod.description)) {
			commandMethod.description = commandMethod.methodName;
		}
	}

	/**
	 * The Java Bean to represent a Control Bus command as a bean method with its parameter types.
	 */
	public static final class CommandMethod {

		private final String beanName;

		private final String methodName;

		private final Class<?>[] parameterTypes;

		private String description;

		private CommandMethod(String beanName, String methodName, Class<?>[] parameterTypes) {
			this.beanName = beanName;
			this.methodName = methodName;
			this.parameterTypes = parameterTypes;
		}

		public String getBeanName() {
			return this.beanName;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public Class<?>[] getParameterTypes() {
			return this.parameterTypes;
		}

		public String getDescription() {
			return this.description;
		}

		@Override
		public int hashCode() {
			int result = Objects.hash(this.beanName, this.methodName);
			result = 31 * result + Arrays.hashCode(this.parameterTypes);
			return result;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			CommandMethod that = (CommandMethod) o;
			return Objects.equals(this.beanName, that.beanName)
					&& Objects.equals(this.methodName, that.methodName)
					&& areParameterTypesEqual(this.parameterTypes, that.parameterTypes);
		}

	}

}
