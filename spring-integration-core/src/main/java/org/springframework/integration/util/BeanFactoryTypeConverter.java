/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.util;

import java.beans.PropertyEditor;

import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.integration.history.MessageHistory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Soby Chacko
 * @author Artem Bilan
 */
public class BeanFactoryTypeConverter implements TypeConverter, BeanFactoryAware {

	private SimpleTypeConverter delegate = new SimpleTypeConverter();

	private ConversionService conversionService;

	private volatile boolean haveCalledDelegateGetDefaultEditor;


	public BeanFactoryTypeConverter() {
		this.conversionService = DefaultConversionService.getSharedInstance();
	}

	public BeanFactoryTypeConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			Object typeConverter = ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
			if (typeConverter instanceof SimpleTypeConverter) {
				this.delegate = (SimpleTypeConverter) typeConverter;
			}
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		if (this.conversionService.canConvert(sourceType, targetType)) {
			return true;
		}
		if (!String.class.isAssignableFrom(sourceType) && !String.class.isAssignableFrom(targetType)) {
			// PropertyEditor cannot convert non-Strings
			return false;
		}
		if (!String.class.isAssignableFrom(sourceType)) {
			return this.delegate.findCustomEditor(sourceType, null) != null || this.getDefaultEditor(sourceType) != null;
		}
		return this.delegate.findCustomEditor(targetType, null) != null || this.getDefaultEditor(targetType) != null;
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceTypeDescriptor, TypeDescriptor targetTypeDescriptor) {
		if (this.conversionService.canConvert(sourceTypeDescriptor, targetTypeDescriptor)) {
			return true;
		}
		// TODO: what does this mean? This method is not used in SpEL so probably ignorable?
		Class<?> sourceType = sourceTypeDescriptor.getObjectType();
		Class<?> targetType = targetTypeDescriptor.getObjectType();
		return canConvert(sourceType, targetType);
	}

	@Override // NOSONAR
	public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// Echoes org.springframework.expression.common.ExpressionUtils.convertTypedValue()
		if ((targetType.getType() == Void.class || targetType.getType() == Void.TYPE) && value == null) {
			return null;
		}
		/*
		 *  INT-2630 Spring 3.1 now converts ALL arguments; we know we don't need to convert MessageHeaders
		 *  or MessageHistory; the MapToMap converter requires a no-arg constructor.
		 *  Also INT-2650 - don't convert large byte[]
		 */
		if (sourceType != null) {
			Class<?> sourceClass = sourceType.getType();
			Class<?> targetClass = targetType.getType();
			if ((sourceClass == MessageHeaders.class && targetClass == MessageHeaders.class) || // NOSONAR
					(sourceClass == MessageHistory.class && targetClass == MessageHistory.class) ||
					(sourceType.isAssignableTo(targetType) && ClassUtils.isPrimitiveArray(sourceClass))) {
				return value;
			}
		}
		if (this.conversionService.canConvert(sourceType, targetType)) {
			return this.conversionService.convert(value, sourceType, targetType);
		}

		Object editorResult = valueFromEditorIfAny(value, sourceType.getType(), targetType);

		if (editorResult == null) {
			synchronized (this.delegate) {
				return this.delegate.convertIfNecessary(value, targetType.getType());
			}
		}
		else {
			return editorResult;
		}
	}

	private PropertyEditor getDefaultEditor(Class<?> sourceType) {
		PropertyEditor defaultEditor;
		if (this.haveCalledDelegateGetDefaultEditor) {
			defaultEditor = this.delegate.getDefaultEditor(sourceType);
		}
		else {
			synchronized (this) {
				// not thread-safe - it builds the defaultEditors field in-place (SPR-10191)
				defaultEditor = this.delegate.getDefaultEditor(sourceType);
			}
			this.haveCalledDelegateGetDefaultEditor = true;
		}
		return defaultEditor;
	}

	@Nullable
	private Object valueFromEditorIfAny(Object value, Class<?> sourceClass, TypeDescriptor targetType) {
		if (!String.class.isAssignableFrom(sourceClass)) {
			PropertyEditor editor = this.delegate.findCustomEditor(sourceClass, null);
			if (editor == null) {
				editor = getDefaultEditor(sourceClass);
			}
			if (editor != null) { // INT-1441
				String text;
				synchronized (editor) {
					editor.setValue(value);
					text = editor.getAsText();
				}

				if (String.class.isAssignableFrom(targetType.getType())) {
					return text;
				}

				return convertValue(text, TypeDescriptor.valueOf(String.class), targetType);
			}
		}
		return null;
	}

}
