/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.integration.config.IntegrationRegistrar;

/**
 * Add this annotation to an {@code @Configuration} class to have
 * the imported Spring Integration configuration :
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableIntegration
 * &#064;ComponentScan(basePackageClasses = { MyConfiguration.class })
 * public class MyIntegrationConfiguration {
 * }
 * </pre>
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(IntegrationRegistrar.class)
public @interface EnableIntegration {

}
