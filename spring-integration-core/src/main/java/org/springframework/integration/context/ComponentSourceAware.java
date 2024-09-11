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

package org.springframework.integration.context;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.lang.Nullable;

/**
 * The contract to supply and provide useful information about
 * a bean definition (or singleton) source - the place where this bean is declared.
 * Usually populated from a respective {@link org.springframework.beans.factory.config.BeanDefinition}
 * or via Spring Integration infrastructure.
 * <p>
 * The information from this contract is typically used from exceptions to easy determine
 * the place in the application resources where this bean is declared.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 *
 * @see org.springframework.beans.factory.config.BeanDefinition
 */
public interface ComponentSourceAware extends BeanNameAware {

	/**
	 * Set a configuration source {@code Object} for this bean definition.
	 * For normal {@link org.springframework.beans.factory.config.BeanDefinition} this is supplied
	 * by application context automatically.
	 * Could be useful when bean is registered at runtime via
	 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry#registerSingleton(String, Object)}
	 * @param source the configuration source
	 */
	void setComponentSource(Object source);

	/**
	 * Return the configuration source {@code Object} for this bean (maybe {@code null}).
	 * Usually (if not set explicitly) a {@link org.springframework.beans.factory.config.BeanDefinition#getSource()}.
	 * @return the configuration source for the bean (if any).
	 */
	@Nullable
	Object getComponentSource();

	/**
	 * Set a human-readable description of this bean.
	 * For normal bean definition a {@link org.springframework.beans.factory.config.BeanDefinition#getDescription()}
	 * is used.
	 * @param description the bean description
	 */
	void setComponentDescription(String description);

	/**
	 * Return a human-readable description of this bean.
	 * Usually (if not set explicitly) a {@link org.springframework.beans.factory.config.BeanDefinition#getDescription()}.
	 * @return the bean description (if any).
	 */
	@Nullable
	String getComponentDescription();

	/**
	 * Return the bean name populated by the {@link BeanNameAware#setBeanName(String)}.
	 * @return the bean name.
	 */
	@Nullable
	String getBeanName();

}
