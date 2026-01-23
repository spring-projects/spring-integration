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

import java.sql.Types;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.1
 */
public class StoredProcPollingChannelAdapterParserTests {

	private ConfigurableApplicationContext context;

	private SourcePollingChannelAdapter pollingAdapter;

	@Test
	public void testProcedureNameIsSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Expression storedProcedureName = (Expression) accessor.getPropertyValue("storedProcedureNameExpression");
		assertThat(storedProcedureName.getValue()).as("Wrong stored procedure name").isEqualTo("GET_PRIME_NUMBERS");
	}

	@Test
	public void testProcedureNameExpressionIsSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest2.xml", getClass());

		Expression storedProcedureNameExpression =
				TestUtils.getPropertyValue(this.pollingAdapter, "source.executor.storedProcedureNameExpression");

		assertThat(storedProcedureNameExpression.getExpressionString()).as("Wrong stored procedure name")
				.isEqualTo("'GET_PRIME_NUMBERS'");
	}

	@Test
	public void testDefaultJdbcCallOperationsCacheSizeIsSet() {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		Integer cacheSize =
				TestUtils.getPropertyValue(this.pollingAdapter, "source.executor.jdbcCallOperationsCacheSize");

		assertThat(cacheSize).as("Wrong Default JdbcCallOperations Cache Size").isEqualTo(Integer.valueOf(10));
	}

	@Test
	public void testJdbcCallOperationsCacheSizeIsSet() {
		setUp("storedProcPollingChannelAdapterParserTest2.xml", getClass());

		Integer cacheSize = TestUtils.getPropertyValue(this.pollingAdapter, "source.executor.jdbcCallOperationsCacheSize");

		assertThat(cacheSize).as("Wrong JdbcCallOperations Cache Size").isEqualTo(Integer.valueOf(77));
	}

	@Test
	public void testSkipUndeclaredResultsAttributeSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean skipUndeclaredResults = (Boolean) accessor.getPropertyValue("skipUndeclaredResults");
		assertThat(skipUndeclaredResults).as("skipUndeclaredResults was not set and should default to 'true'").isTrue();
	}

	@Test
	public void testReturnValueRequiredAttributeSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean returnValueRequired = (Boolean) accessor.getPropertyValue("returnValueRequired");
		assertThat(returnValueRequired).isTrue();
	}

	@Test
	public void testIsFunctionAttributeSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean isFunction = (Boolean) accessor.getPropertyValue("isFunction");
		assertThat(isFunction).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedureParametersAreSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object procedureParameters = accessor.getPropertyValue("procedureParameters");
		assertThat(procedureParameters).isNotNull();
		assertThat(procedureParameters instanceof List).isTrue();

		List<ProcedureParameter> procedureParametersAsList = (List<ProcedureParameter>) procedureParameters;

		assertThat(procedureParametersAsList.size() == 4).isTrue();

		ProcedureParameter parameter1 = procedureParametersAsList.get(0);
		ProcedureParameter parameter2 = procedureParametersAsList.get(1);
		ProcedureParameter parameter3 = procedureParametersAsList.get(2);
		ProcedureParameter parameter4 = procedureParametersAsList.get(3);

		assertThat(parameter1.getName()).isEqualTo("username");
		assertThat(parameter2.getName()).isEqualTo("description");
		assertThat(parameter3.getName()).isEqualTo("password");
		assertThat(parameter4.getName()).isEqualTo("age");

		assertThat(parameter1.getValue()).isEqualTo("kenny");
		assertThat(parameter2.getValue()).isEqualTo("Who killed Kenny?");
		assertThat(parameter3.getValue()).isNull();
		assertThat(parameter4.getValue()).isEqualTo(30);

		assertThat(parameter1.getExpression()).isNull();
		assertThat(parameter2.getExpression()).isNull();
		assertThat(parameter3.getExpression()).isEqualTo("payload.username");
		assertThat(parameter4.getExpression()).isNull();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReturningResultSetRowMappersAreSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object returningResultSetRowMappers = accessor.getPropertyValue("returningResultSetRowMappers");
		assertThat(returningResultSetRowMappers).isNotNull();
		assertThat(returningResultSetRowMappers instanceof Map).isTrue();

		Map<String, RowMapper<?>> returningResultSetRowMappersAsMap =
				(Map<String, RowMapper<?>>) returningResultSetRowMappers;

		assertThat(returningResultSetRowMappersAsMap.size() == 2)
				.as("The rowmapper was not set. Expected returningResultSetRowMappersAsMap.size() == 2").isTrue();

		Iterator<Entry<String, RowMapper<?>>> iterator = returningResultSetRowMappersAsMap.entrySet().iterator();

		Entry<String, ?> mapEntry = iterator.next();
		assertThat(mapEntry.getKey()).isEqualTo("out");
		assertThat(mapEntry.getValue() instanceof PrimeMapper).isTrue();

		mapEntry = iterator.next();
		assertThat(mapEntry.getKey()).isEqualTo("out2");
		assertThat(mapEntry.getValue() instanceof SingleColumnRowMapper).isTrue();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSqlParametersAreSet() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.pollingAdapter);
		Object source = accessor.getPropertyValue("source");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object sqlParameters = accessor.getPropertyValue("sqlParameters");
		assertThat(sqlParameters).isNotNull();
		assertThat(sqlParameters instanceof List).isTrue();

		List<SqlParameter> sqlParametersAsList = (List<SqlParameter>) sqlParameters;

		assertThat(sqlParametersAsList.size() == 4).isTrue();

		SqlParameter parameter1 = sqlParametersAsList.get(0);
		SqlParameter parameter2 = sqlParametersAsList.get(1);
		SqlParameter parameter3 = sqlParametersAsList.get(2);
		SqlParameter parameter4 = sqlParametersAsList.get(3);

		assertThat(parameter1.getName()).isEqualTo("username");
		assertThat(parameter2.getName()).isEqualTo("password");
		assertThat(parameter3.getName()).isEqualTo("age");
		assertThat(parameter4.getName()).isEqualTo("description");

		assertThat(parameter1.getScale()).as("Expect that the scale is null.").isNull();
		assertThat(parameter2.getScale()).as("Expect that the scale is null.").isNull();
		assertThat(parameter3.getScale()).as("Expect that the scale is 5.").isEqualTo(Integer.valueOf(5));
		assertThat(parameter4.getScale()).as("Expect that the scale is null.").isNull();

		assertThat(parameter1.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);
		assertThat(parameter2.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);
		assertThat(parameter3.getSqlType()).as("SqlType is ").isEqualTo(Types.INTEGER);
		assertThat(parameter4.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);

		assertThat(parameter2 instanceof SqlOutParameter).isTrue();
		assertThat(parameter3 instanceof SqlInOutParameter).isTrue();

	}

	@Test
	public void testAutoChannel() throws Exception {
		setUp("storedProcPollingChannelAdapterParserTest.xml", getClass());
		MessageChannel autoChannel = context.getBean("autoChannel", MessageChannel.class);
		SourcePollingChannelAdapter autoChannelAdapter =
				context.getBean("autoChannel.adapter", SourcePollingChannelAdapter.class);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(autoChannelAdapter, "outputChannel")).isSameAs(autoChannel);
		assertThat(TestUtils.<Boolean>getPropertyValue(autoChannelAdapter, "source.executor.returnValueRequired"))
				.isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(autoChannelAdapter, "source.executor.isFunction"))
				.isFalse();
		autoChannelAdapter.stop();
	}

	@AfterEach
	public void tearDown() {
		this.pollingAdapter.stop();
		ThreadPoolTaskScheduler taskScheduler = context.getBean(ThreadPoolTaskScheduler.class);
		taskScheduler.setAwaitTerminationSeconds(10);
		taskScheduler.destroy();
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		this.context = new ClassPathXmlApplicationContext(name, cls);
		this.pollingAdapter =
				this.context.getBean("storedProcedurePollingChannelAdapter", SourcePollingChannelAdapter.class);
	}

}
