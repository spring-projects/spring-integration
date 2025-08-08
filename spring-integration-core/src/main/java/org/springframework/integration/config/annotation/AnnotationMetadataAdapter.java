/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
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
