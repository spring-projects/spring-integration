/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.integration.handler.MessageHandler;

/**
 * A strategy for programmatically creating a {@link MessageHandler} based on
 * metadata provided by an annotation.
 * 
 * @author Mark Fisher
 */
public interface AnnotationHandlerCreator {

	MessageHandler createHandler(Object object, Method method, Annotation annotation);

}
