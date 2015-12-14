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

package org.springframework.integration.expression;

import org.springframework.expression.EvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * Interface to be implemented by beans that wish to be aware of their
 * owning integration {@link EvaluationContext}, which is the result of
 * {@link org.springframework.integration.config.IntegrationEvaluationContextFactoryBean}
 * <p>
 * The {@link #setIntegrationEvaluationContext} is invoked from
 * the {@code IntegrationEvaluationContextAwareBeanPostProcessor#afterSingletonsInstantiated()},
 * not during standard {@code postProcessBefore(After)Initialization} to avoid any
 * {@code BeanFactory} early access during integration {@link EvaluationContext} retrieval.
 * Therefore, if it is necessary to use {@link EvaluationContext} in the {@code afterPropertiesSet()},
 * the {@code IntegrationContextUtils.getEvaluationContext(this.beanFactory)} should be used instead
 * of this interface implementation.
 *
 * @author Artem Bilan
 * @since 3.0
 * @deprecated since 4.2 in favor of {@link IntegrationContextUtils#getEvaluationContext}
 * direct usage from the {@code afterPropertiesSet} implementation.
 * Will be removed in the next release.
 */
@Deprecated
public interface IntegrationEvaluationContextAware {

	void setIntegrationEvaluationContext(EvaluationContext evaluationContext);

}
