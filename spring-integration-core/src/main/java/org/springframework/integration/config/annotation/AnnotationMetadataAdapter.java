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

package org.springframework.integration.config.annotation;

import java.util.Set;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;

/**
 * An {@link AnnotationMetadata} implementation to expose a metadata
 * by the provided {@link java.util.Map} of attributes.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public abstract class AnnotationMetadataAdapter implements AnnotationMetadata {

	private static final RuntimeException UNSUPPORTED_OPERATION =
			new UnsupportedOperationException("The class doesn't support this operation");

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public String getClassName() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public boolean isInterface() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public boolean isAnnotation() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public boolean isAbstract() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public boolean isFinal() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public boolean isIndependent() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public String getEnclosingClassName() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public String getSuperClassName() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public String[] getInterfaceNames() {
		throw UNSUPPORTED_OPERATION;
	}

	@Override
	public String[] getMemberClassNames() {
		throw UNSUPPORTED_OPERATION;
	}

}
