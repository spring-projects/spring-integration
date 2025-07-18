/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.aggregator.MethodInvokingReleaseStrategy;
import org.springframework.integration.aggregator.ReleaseStrategy;
import org.springframework.integration.aggregator.SimpleSequenceSizeReleaseStrategy;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.util.StringUtils;

/**
 * Convenience factory for XML configuration of a {@link ReleaseStrategy}.
 * Encapsulates the knowledge of the default strategy and search algorithms for POJO and annotated methods.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author Artem Bilan
 *
 */
public class ReleaseStrategyFactoryBean implements FactoryBean<ReleaseStrategy>, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(ReleaseStrategyFactoryBean.class);

	private @Nullable Object target;

	private @Nullable String methodName;

	private ReleaseStrategy strategy = new SimpleSequenceSizeReleaseStrategy();

	public void setTarget(Object target) {
		this.target = target;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.target instanceof ReleaseStrategy && !StringUtils.hasText(this.methodName)) {
			this.strategy = (ReleaseStrategy) this.target;
			return;
		}
		if (this.target != null) {
			if (StringUtils.hasText(this.methodName)) {
				this.strategy = new MethodInvokingReleaseStrategy(this.target, this.methodName);
			}
			else {
				Method method = MessagingAnnotationUtils.findAnnotatedMethod(this.target,
						org.springframework.integration.annotation.ReleaseStrategy.class);
				if (method != null) {
					this.strategy = new MethodInvokingReleaseStrategy(this.target, method);
				}
				else {
					if (LOGGER.isWarnEnabled()) {
						LOGGER.warn("No ReleaseStrategy annotated method found on "
								+ this.target.getClass().getSimpleName()
								+ "; falling back to SimpleSequenceSizeReleaseStrategy, target: "
								+ this.target + ", methodName: " + this.methodName);
					}
				}
			}
		}
		else {
			LOGGER.warn("No target supplied; falling back to SimpleSequenceSizeReleaseStrategy");
		}
	}

	@NonNull
	@Override
	public ReleaseStrategy getObject() {
		return this.strategy;
	}

	@Override
	public Class<?> getObjectType() {
		return ReleaseStrategy.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
