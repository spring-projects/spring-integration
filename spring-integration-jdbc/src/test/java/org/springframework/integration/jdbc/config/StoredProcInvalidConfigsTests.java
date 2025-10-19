/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.jdbc.config;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class StoredProcInvalidConfigsTests {

	@Test
	public void testProcedureNameAndExpressionExclusivity() {

		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("nameAndExpressionExclusivity"))
				.withMessageContaining("Exactly one of 'stored-procedure-name' or 'stored-procedure-name-expression' is required");
	}

	@Test
	public void testReturnTypeForInParameter() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("returnTypeForInParameter"))
				.withMessageContaining("'return-type' attribute can't be provided for IN 'sql-parameter-definition' element.");
	}

	@Test
	public void testTypeNameAndScaleExclusivity() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("typeNameAndScaleExclusivity"))
				.withMessageContaining("'type-name' and 'scale' attributes are mutually exclusive for 'sql-parameter-definition' element.");
	}

	@Test
	public void testReturnTypeAndScaleExclusivity() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() -> bootStrap("returnTypeAndScaleExclusivity"))
				.withMessageContaining("'returnType' and 'scale' attributes are mutually exclusive for 'sql-parameter-definition' element.");
	}

	private ApplicationContext bootStrap(String configProperty) throws Exception {
		PropertiesFactoryBean pfb = new PropertiesFactoryBean();
		pfb.setLocation(new ClassPathResource("org/springframework/integration/jdbc/config/stored-proc-invalid-configs.properties"));
		pfb.afterPropertiesSet();
		Properties prop = pfb.getObject();
		StringBuilder buffer = new StringBuilder();
		buffer.append(prop.getProperty("xmlheaders")).append(prop.getProperty(configProperty)).append(prop.getProperty("xmlfooter"));
		ByteArrayInputStream stream = new ByteArrayInputStream(buffer.toString().getBytes());
		GenericApplicationContext ac = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ac);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		ac.refresh();
		return ac;
	}

}
