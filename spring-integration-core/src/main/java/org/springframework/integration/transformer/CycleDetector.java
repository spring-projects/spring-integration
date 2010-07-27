/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.transformer;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class CycleDetector {
	private final ExpressionParser parser = new SpelExpressionParser();
	private final String[] defaultIgnorePakages = new String[]{
			"com.apple",
			"com.sun",
			"java.awt",
			"java",
			"javax",
			"org.jcp",
			"org.omg",
			"sun"
	};
	private boolean ignoreClassAttribute = true;
	private boolean ignoreDefaultPackages = true;
	/**
	 * 
	 * @param target
	 * @param ignorePakages
	 */
	public void detectCycle(Object target, String... ignorePakages){
		Map<Object, Set<Object>> objectReferenceMap = new HashMap<Object, Set<Object>>();
		this.doDetect(target, objectReferenceMap, ignorePakages);
	}
	/*
	 * 
	 */
	private void doDetect(Object target, Map<Object, Set<Object>> objectReferenceMap, String[] ignorePakages){
		if (propertyInIgnoredPackage(target, ignorePakages)){
			return;
		}
		if (collectionMapOrArray(target)){
			Iterable<?> iterable = null;
			if (target instanceof Collection<?>) {
				iterable = (Iterable<?>) target;
			} else if (target instanceof Map<?, ?>){
				iterable = ((Map<?, ?>) target).values();
			} else if (target.getClass().isArray()){
				iterable = CollectionUtils.arrayToList(target);
			}	
			for (Object value : iterable) {
				this.doDetect(value, objectReferenceMap, ignorePakages);
			}
		} else {
			if (!objectReferenceMap.containsKey(target)){
				objectReferenceMap.put(target, new HashSet<Object>());	
			}
			EvaluationContext context = new StandardEvaluationContext(target);
			BeanWrapperImpl bw = new BeanWrapperImpl(target);
			PropertyDescriptor[] descriptors =  bw.getPropertyDescriptors(); 
			for (PropertyDescriptor propertyDescriptor : descriptors) {
				String propertyName = propertyDescriptor.getName();
				if (propertyName.equals("class") && ignoreClassAttribute){
					continue; // no need to process
				}
				Expression expression = parser.parseExpression(propertyName);
				Object propertyValue = null;
				try {
					propertyValue = expression.getValue(context);
				} catch (Exception e) {/*nothing to do, might only happen when 'ignoreClassAttribute' is set to false ('true' by default)*/}
					
				if (propertyValue != null){
					if (!collectionMapOrArray(propertyValue)){
						Set<Object> references = objectReferenceMap.get(target);
						if (!references.contains(propertyValue)){
							references.add(propertyValue);
						}
						if (objectReferenceMap.containsKey(propertyValue)){
							references = objectReferenceMap.get(propertyValue);
							if (references.contains(target)){
								throw new MessageTransformationException("Cyclic reference detected between: " + 
										propertyValue.getClass().getSimpleName() + " - " + target.getClass().getSimpleName());
							}
						}
					}
					this.doDetect(propertyValue, objectReferenceMap, ignorePakages);
				}
			}
		}
	}
	/*
	 * 
	 */
	private boolean collectionMapOrArray(Object elementValue){
		return (elementValue instanceof Map<?,?> ||
			    elementValue instanceof Collection<?> ||
			    elementValue.getClass().isArray());
	}
	/*
	 * 
	 */
	private boolean propertyInIgnoredPackage(Object elementValue, String[] ignorePakagess){
		if (this.collectionMapOrArray(elementValue)){
			return false;
		}
		for (String packagePattern : ignorePakagess) {
			if (elementValue.getClass().getPackage().getName().startsWith(packagePattern)){
				return true;
			}
		}
		if (ignoreDefaultPackages){
			for (String packagePattern : defaultIgnorePakages) {
				if (elementValue.getClass().getPackage().getName().startsWith(packagePattern)){
					return true;
				}
			}
		}
		return false;
	}
	public boolean isIgnoreClassAttribute() {
		return ignoreClassAttribute;
	}

	public void setIgnoreClassAttribute(boolean ignoreClassAttribute) {
		this.ignoreClassAttribute = ignoreClassAttribute;
	}
	
	public boolean isIgnoreDefaultPackages() {
		return ignoreDefaultPackages;
	}

	public void setIgnoreDefaultPackages(boolean ignoreDefaultPackages) {
		this.ignoreDefaultPackages = ignoreDefaultPackages;
	}
}
