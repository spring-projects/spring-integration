/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.http.outbound;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.integration.config.ExpressionFactoryBean;

/**
 * Factory beasn thta create a wrapper over uri variable expression 
 * which also contains the value of its 'encode' attribute
 * 
 * @author Oleg Zhurakousky
 * @since 2.2
 */
class UriVariableExpressionDelegateFactoryBean extends ExpressionFactoryBean {
	private final boolean encode;
	
	public UriVariableExpressionDelegateFactoryBean(String expressionString, boolean encode){
		super(expressionString);
		this.encode = encode;
	}
	protected Expression createInstance() throws Exception {	
		return new UriVariableExpressionDelegate(super.createInstance());
	}
	
	public class UriVariableExpressionDelegate implements Expression {
		private final Expression expression;
		
		public UriVariableExpressionDelegate(Expression expression) {
			this.expression = expression;
		}

		public boolean isEncode() {
			return encode;
		}

		public Object getValue() throws EvaluationException {
			return this.expression.getValue();
		}

		public Object getValue(Object rootObject) throws EvaluationException {
			return this.expression.getValue(rootObject);
		}

		public <T> T getValue(Class<T> desiredResultType)
				throws EvaluationException {
			return this.expression.getValue(desiredResultType);
		}

		public <T> T getValue(Object rootObject, Class<T> desiredResultType)
				throws EvaluationException {
			return this.expression.getValue(rootObject, desiredResultType);
		}

		public Object getValue(EvaluationContext context)
				throws EvaluationException {
			return this.expression.getValue(context);
		}

		public Object getValue(EvaluationContext context, Object rootObject)
				throws EvaluationException {
			return this.expression.getValue(context, rootObject);
		}

		public <T> T getValue(EvaluationContext context, Class<T> desiredResultType)
				throws EvaluationException {
			return this.expression.getValue(context, desiredResultType);
		}

		public <T> T getValue(EvaluationContext context, Object rootObject,
				Class<T> desiredResultType) throws EvaluationException {
			return this.expression.getValue(context, rootObject, desiredResultType);
		}

		public Class<?> getValueType() throws EvaluationException {
			return this.expression.getValueType();
		}

		public Class<?> getValueType(Object rootObject) throws EvaluationException {
			return this.expression.getValueType(rootObject);
		}

		public Class<?> getValueType(EvaluationContext context)
				throws EvaluationException {
			return this.expression.getValueType(context);
		}

		public Class<?> getValueType(EvaluationContext context, Object rootObject)
				throws EvaluationException {
			return this.expression.getValueType(context, rootObject);
		}

		public TypeDescriptor getValueTypeDescriptor() throws EvaluationException {
			return this.expression.getValueTypeDescriptor();
		}

		public TypeDescriptor getValueTypeDescriptor(Object rootObject)
				throws EvaluationException {
			return this.expression.getValueTypeDescriptor(rootObject);
		}

		public TypeDescriptor getValueTypeDescriptor(EvaluationContext context)
				throws EvaluationException {
			return this.expression.getValueTypeDescriptor(context);
		}

		public TypeDescriptor getValueTypeDescriptor(EvaluationContext context,
				Object rootObject) throws EvaluationException {
			return this.expression.getValueTypeDescriptor(context, rootObject);
		}

		public boolean isWritable(EvaluationContext context)
				throws EvaluationException {
			return this.expression.isWritable(context);
		}

		public boolean isWritable(EvaluationContext context, Object rootObject)
				throws EvaluationException {
			return this.expression.isWritable(context, rootObject);
		}

		public boolean isWritable(Object rootObject) throws EvaluationException {
			return this.expression.isWritable(rootObject);
		}

		public void setValue(EvaluationContext context, Object value)
				throws EvaluationException {
			this.expression.setValue(context, value);
		}

		public void setValue(Object rootObject, Object value)
				throws EvaluationException {
			this.expression.setValue(rootObject, value);
		}

		public void setValue(EvaluationContext context, Object rootObject,
				Object value) throws EvaluationException {
			this.expression.setValue(context, value);
		}

		public String getExpressionString() {
			return this.expression.getExpressionString();
		}
	}
}
