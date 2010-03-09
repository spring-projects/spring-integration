/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.CollectionUtils;

/** TODO: Map of Maps and List of Lists
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ObjectToSpelMapBuilder {
	private final ExpressionParser parser = new SpelExpressionParser();
	private boolean serializeTypeName;
	
	/**
	 * 
	 * @return
	 */
	public Map<String, Object> buildspelMap(Object rootObject){
		Map<String, Object> propertiesMap = new HashMap<String, Object>();
		EvaluationContext context = new StandardEvaluationContext(rootObject);
		this.buildProperties(context, "", propertiesMap, rootObject);
		return propertiesMap;
	}
	/*
	 * 
	 */
	private void buildProperties(EvaluationContext context, String parentPropertyPath, Map<String, Object> propertiesMap, Object object){ 
		BeanWrapperImpl bw = new BeanWrapperImpl(object);
		PropertyDescriptor[] descriptors =  bw.getPropertyDescriptors(); 
		for (PropertyDescriptor propertyDescriptor : descriptors) {
			Class<?> propertyType = propertyDescriptor.getPropertyType();
			String propertyName = propertyDescriptor.getName();
			String propertyPath = parentPropertyPath + propertyName;
			if (propertyType.isArray() || Collection.class.isAssignableFrom(propertyType)){
				this.processArray(context, propertyPath, propertyName, propertyType, propertiesMap);
			} else if (propertyType.isAnonymousClass()){
				throw new IllegalArgumentException("anonymous class property transformation is not supported");
			} else if (propertyType.isAssignableFrom(Map.class)){
				Expression expression = parser.parseExpression(propertyPath);				
				Map<?,?> map = (Map<?,?>) expression.getValue(context);
				if (map != null){
					this.processMap(context, propertiesMap, map, parentPropertyPath, propertyName);
				}	
			} else {
				Expression expression = parser.parseExpression(propertyPath);
				Object propertyValue = expression.getValue(context);
				if (propertyValue != null){
					this.processElementValue(context, propertyValue, propertyName, propertyPath, propertiesMap);		
				}			
			}
		}
	}
	/*
	 * 
	 */
	private void processMap(EvaluationContext context, Map<String, Object> mappedProperties, Map<?,?> mapToTransform, String propertyPath, String propertyName){
		Iterator<?> mapIter = mapToTransform.keySet().iterator();
		while (mapIter.hasNext()) {
			Object keyElement = mapIter.next();
			Object elementValue = mapToTransform.get(keyElement);
			String mapPropertyPath = propertyPath + propertyName + "['" + keyElement.toString() + "']";
			if (elementValue.getClass().isArray() || elementValue instanceof Collection<?>){
				this.processArray(context, mapPropertyPath, "", elementValue.getClass(), mappedProperties);
			} else if (elementValue instanceof Map<?,?>) {
				this.processMap(context, mappedProperties, (Map<?, ?>) elementValue, mapPropertyPath, "");
			} else {
				this.processElementValue(context, elementValue, propertyName, mapPropertyPath, mappedProperties);
			}
		}
	}
	/*
	 * 
	 */
	private void processArray(EvaluationContext context, String propertyPath, String propertyName, Class<?> elementType, Map<String, Object> propertiesMap){
		Expression arrayExp = parser.parseExpression(propertyPath);				
		Object array = arrayExp.getValue(context);
		List<?> arrayElements = null;
		if (elementType.isArray()){
			arrayElements =  CollectionUtils.arrayToList(array); 
		} else {
			arrayElements = (List<?>) array;
		}
		
		int i = 0;
		for (Object arrayElement : arrayElements) {
			String arrayPropertyPath = propertyPath + "[" + i++ + "]";	
			if (arrayElement instanceof Map){
				// last argument is empty because it is not a named property, but a array element
				this.processMap(context, propertiesMap, (Map<?, ?>) arrayElement, arrayPropertyPath, "");
			} else if (arrayElement.getClass().isArray()){
				this.processArray(context, arrayPropertyPath, propertyName, elementType, propertiesMap);
			} else {
				this.processElementValue(context, arrayElement, propertyName, arrayPropertyPath, propertiesMap);
			}
		}
	}
	/*
	 * 
	 */
	private void processElementValue(EvaluationContext context, Object elementValue, String propertyName, String propertyPath, Map<String, Object> propertiesMap){
		// JDK packages considered shared, thus serializable
		if (elementValue.getClass().getPackage().getName().contains("java")){
			if (propertyName.equals("class") && !serializeTypeName){
				return;
			} else {
				propertiesMap.put(propertyPath, elementValue);
			}
		} else {
			this.buildProperties(context, propertyPath+".", propertiesMap, elementValue);		
		}
	}
	/**
	 * 
	 * @return
	 */
	public boolean isSerializeTypeName() {
		return serializeTypeName;
	}
	/**
	 * 
	 * @param serializeTypeName
	 */
	public void setSerializeTypeName(boolean serializeTypeName) {
		this.serializeTypeName = serializeTypeName;
	}
}
