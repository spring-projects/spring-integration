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

package org.springframework.integration.http.management;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.management.ControlBusCommandRegistry;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The REST Controller to provide the management API for Control Bus pattern.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
@RestController
@RequestMapping("/control-bus")
public class ControlBusController implements BeanFactoryAware, InitializingBean {

	private final ControlBusCommandRegistry controlBusCommandRegistry;

	private final FormattingConversionService conversionService;

	private BeanFactory beanFactory;

	private EvaluationContext evaluationContext;

	public ControlBusController(ControlBusCommandRegistry controlBusCommandRegistry,
			FormattingConversionService conversionService) {

		this.controlBusCommandRegistry = controlBusCommandRegistry;
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.evaluationContext = IntegrationContextUtils.getEvaluationContext(this.beanFactory);
	}

	@GetMapping(name = "getCommands")
	public List<ControlBusBean> getCommands() {
		return this.controlBusCommandRegistry.getCommands()
				.entrySet()
				.stream()
				.map((beanEntry) -> createControlBusBean(beanEntry.getKey(), beanEntry.getValue()))
				.toList();
	}

	@PostMapping(name = "invokeCommand", path = "/{command}")
	public Object invokeCommand(@PathVariable String command,
			@RequestBody(required = false) List<CommandArgument> arguments) {

		Class<?>[] parameterTypes = new Class<?>[0];
		if (!CollectionUtils.isEmpty(arguments)) {
			parameterTypes = arguments.stream()
					.map(CommandArgument::parameterType)
					.toArray(Class<?>[]::new);
		}

		Expression commandExpression = this.controlBusCommandRegistry.getExpressionForCommand(command, parameterTypes);

		Object[] parameterValues = null;

		if (!CollectionUtils.isEmpty(arguments)) {
			parameterValues = arguments.stream()
					.map((arg) -> this.conversionService.convert(arg.value, arg.parameterType))
					.toArray(Object[]::new);
		}

		return commandExpression.getValue(this.evaluationContext, parameterValues);
	}

	private static ControlBusBean createControlBusBean(String beanName,
			Map<ControlBusCommandRegistry.CommandMethod, String> commandsForBean) {

		List<ControlBusCommand> commands =
				commandsForBean.keySet()
						.stream()
						.map(ControlBusController::converControlBusCommand)
						.toList();

		return new ControlBusBean(beanName, commands);
	}

	private static ControlBusCommand converControlBusCommand(ControlBusCommandRegistry.CommandMethod commandMethod) {
		return new ControlBusCommand(commandMethod.getBeanName() + '.' + commandMethod.getMethodName(),
				commandMethod.getDescription(),
				Arrays.asList(commandMethod.getParameterTypes()));
	}

	public record ControlBusBean(String beanName, List<ControlBusCommand> commands) {

	}

	public record ControlBusCommand(String command, String description, List<Class<?>> parameterTypes) {

	}

	public record CommandArgument(String value, Class<?> parameterType) {

	}

}
