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

package org.springframework.integration.test.condition;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.test.util.TestUtils.LevelsContainer;

/**
 * JUnit condition that adjusts and reverts log levels before/after each test.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class LogLevelsCondition
		implements ExecutionCondition, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

	private static final String STORE_ANNOTATION_KEY = "logLevelsAnnotation";

	private static final String STORE_CONTAINER_KEY = "logLevelsContainer";

	private static final ConditionEvaluationResult ENABLED =
			ConditionEvaluationResult.enabled("@LogLevels always enabled");

	@Override
	public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
		Optional<AnnotatedElement> element = context.getElement();
		MergedAnnotations annotations = MergedAnnotations.from(element.get(),
				MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		MergedAnnotation<LogLevels> mergedAnnotation = annotations.get(LogLevels.class);
		if (mergedAnnotation.isPresent()) {
			LogLevels loglevels = mergedAnnotation.synthesize();
			Store store = context.getStore(Namespace.create(getClass(), context));
			store.put(STORE_ANNOTATION_KEY, loglevels);
		}
		return ENABLED;
	}

	@Override
	public void beforeEach(ExtensionContext context) {
		Store store = context.getStore(Namespace.create(getClass(), context));
		LogLevels logLevels = store.get(STORE_ANNOTATION_KEY, LogLevels.class);
		if (logLevels == null) {
			ExtensionContext parent = context.getParent().get();
			store = parent.getStore(Namespace.create(getClass(), parent));
			logLevels = store.get(STORE_ANNOTATION_KEY, LogLevels.class);
		}
		store.put(STORE_CONTAINER_KEY, TestUtils.adjustLogLevels(context.getDisplayName(),
				Arrays.asList((logLevels.classes())),
				Arrays.asList(logLevels.categories()),
				Level.toLevel(logLevels.level())));
	}

	@Override
	public void afterEach(ExtensionContext context) {
		Store store = context.getStore(Namespace.create(getClass(), context));
		LevelsContainer container = store.get(STORE_CONTAINER_KEY, LevelsContainer.class);
		boolean parentStore = false;
		if (container == null) {
			ExtensionContext parent = context.getParent().get();
			store = parent.getStore(Namespace.create(getClass(), parent));
			container = store.get(STORE_CONTAINER_KEY, LevelsContainer.class);
			parentStore = true;
		}
		TestUtils.revertLogLevels(context.getDisplayName(), container);
		store.remove(STORE_CONTAINER_KEY);
		if (!parentStore) {
			store.remove(STORE_ANNOTATION_KEY);
		}
	}

	@Override
	public void afterAll(ExtensionContext context) {
		Store store = context.getStore(Namespace.create(getClass(), context));
		store.remove(STORE_ANNOTATION_KEY);
	}

}
