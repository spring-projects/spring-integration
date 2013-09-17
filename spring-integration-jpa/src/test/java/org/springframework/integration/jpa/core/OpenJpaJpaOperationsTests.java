/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jpa.core;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

import org.junit.Assert;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.lib.util.Options;
import org.apache.openjpa.persistence.InvalidStateException;
import org.apache.openjpa.persistence.PersistenceException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests the functionality of {@link JpaOperations} and {@link DefaultJpaOperations}
 * using the OpenJPA persistence provider.
 *
 * If you want to run these tests from your IDE, please ensure that you execute
 * the tests using a <i>javaagent</i>:
 *
 * <pre>
 * {@code
 * -javaagent:/<path_to>/openjpa-2.1.1.jar
 * }
 * </pre>
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class OpenJpaJpaOperationsTests extends AbstractJpaOperationsTests {

	@Test
	@Override
	public void testExecuteUpdateWithNativeQuery() {

		try {
			super.testExecuteUpdateWithNativeQuery();
		} catch (PersistenceException e) {
			return;
		}

		Assert.fail("Was expecting an Exception as OpenJPA does not support Native SQL Queries with Named Parameters.");
	}

	@Test
	@Override
	public void testExecuteUpdateWithNativeNamedQuery() {

		try {
			super.testExecuteUpdateWithNativeNamedQuery();
		} catch (InvalidStateException e) {
			return;
		}

		Assert.fail("Was expecting an Exception as OpenJPA does not support Native SQL Queries with Named Parameters.");
	}

	/**
	 * Test method for {@link org.springframework.integration.jpa.core.DefaultJpaOperations#persist(java.lang.Object)}.
	 *
	 * http://openjpa.apache.org/builds/1.0.4/apache-openjpa-1.0.4/docs/manual/manual.html#ref_guide_ddl_examples
	 */
	//@Test
	public void testGenerateSchema() {

		String[] arguments = {};

		Options opts = new Options();
		opts.put("schemaAction", "build");
		opts.put("sql", "build/database/openjpa-h2.sql");
		opts.put("org.springframework.integration.jpa.test.entity.Student", "true");

		final String[] args = opts.setFromCmdLine(arguments);

		boolean ret = Configurations.runAgainstAllAnchors(opts,
					new Configurations.Runnable() {
					public boolean run(Options opts) throws IOException, SQLException {
						JDBCConfiguration conf = new JDBCConfigurationImpl();
						conf.setConnectionDriverName("org.h2.Driver");
						conf.setConnectionURL("jdbc:h2:~/test");
						conf.setConnectionUserName("sa");
						conf.setConnectionPassword("");

						try {
							return MappingTool.run(conf, args, opts);
						} finally {
							conf.close();
						}
					}
				});

	}

	@Test
	@Override
	public void testExecuteUpdate() {
		super.testExecuteUpdate();
	}

	@Test
	@Override
	public void testExecuteUpdateWithNamedQuery() {
		super.testExecuteUpdateWithNamedQuery();
	}

	@Test
	@Override
	public void testExecuteSelectWithNativeQueryReturningEntityClass()
			throws ParseException {
		super.testExecuteSelectWithNativeQueryReturningEntityClass();
	}

	@Test
	@Override
	public void testExecuteSelectWithNativeQuery() throws ParseException {
		super.testExecuteSelectWithNativeQuery();
	}

	@Test
	@Override
	public void testMerge() {
		super.testMerge();
	}

	@Test
	@Override
	public void testMergeCollection() {
		super.testMergeCollection();
	}

	@Test
	@Override
	public void testMergeNullCollection() {
		super.testMergeNullCollection();
	}

	@Test
	@Override
	public void testMergeCollectionWithNullElement() {
		super.testMergeCollectionWithNullElement();
	}

	@Test
	@Override
	public void testPersist() {
		super.testPersist();
	}

	@Test
	@Override
	public void testPersistCollection() {
		super.testPersistCollection();
	}

	@Test
	@Override
	public void testPersistNullCollection() {
		super.testPersistNullCollection();
	}

	@Test
	@Override
	public void testPersistCollectionWithNullElement() {
		super.testPersistCollectionWithNullElement();
	}

	@Test
	@Override
	public void testGetAllStudents() {
		super.testGetAllStudents();
	}

	@Test
	@Override
	public void testGetAllStudentsWithMaxResults() {
		super.testGetAllStudentsWithMaxResults();
	}

	@Test
	@Override
	public void testDeleteInBatch() {
		super.testDeleteInBatch();
	}

	@Test
	@Override
	public void testDelete() {
		super.testDelete();
	}

	@Test
	@Override
	public void testDeleteInBatchWithEmptyCollection() {
		super.testDeleteInBatchWithEmptyCollection();
	}
}
