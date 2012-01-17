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
package org.springframework.integration.jpa.support;

import org.springframework.util.Assert;

/**
 * Abstraction of Jpa parameters allowing to provide static parameters 
 * and SpEl Expression based parameters. 
 *
 * TODO Should we combine ProcedureParameter class and this class?
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class JpaParameter {

    private String name;
    private Object value;
    private String expression;

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Object getValue() {
        return this.value;
    }
    public void setValue(Object value) {
        this.value = value;
    }
    public String getExpression() {
        return this.expression;
    }
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Instantiates a new Jpa Parameter. 
     * 
     * @param name Name of the procedure parameter, must not be null or empty
     * @param value If null, the expression property must be set
     * @param expression If null, the value property must be set
     */
    public JpaParameter(String name, Object value, String expression) {
        super();
        
        Assert.hasText(name, "'name' must not be empty.");

        this.name = name;
        this.value = value;
        this.expression = expression;
    }

    /**
     * Instantiates a new Jpa Parameter without a name. This is useful for specifying 
     * positional Jpa parameters.
     * 
     * @param value If null, the expression property must be set
     * @param expression If null, the value property must be set
     */
    public JpaParameter(Object value, String expression) {
        super();
        this.value      = value;
        this.expression = expression;
    }
    
	/**
	 * Default constructor.
	 */
	public JpaParameter() {
		super();
	}
	
	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JpaParameter [name=").append(this.name)
                                .append(", value=").append(this.value)
                                .append(", expression=").append(this.expression)
                                .append("]");
        return builder.toString();
    }
    
}
