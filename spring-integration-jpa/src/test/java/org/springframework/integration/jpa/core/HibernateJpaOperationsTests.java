/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jpa.core;

import java.text.ParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class HibernateJpaOperationsTests extends AbstractJpaOperationsTests {

	@Test
	@Override
	public void testExecuteUpdate() {
		super.testExecuteUpdate();
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
	public void testExecuteUpdateWithNamedQuery() {
		super.testExecuteUpdateWithNamedQuery();
	}

	@Test
	@Override
	public void testExecuteUpdateWithNativeQuery() {
		super.testExecuteUpdateWithNativeQuery();
	}

	@Test
	@Override
	public void testExecuteSelectWithNativeQueryReturningEntityClass() throws ParseException {
		super.testExecuteSelectWithNativeQueryReturningEntityClass();
	}

	@Test
	@Override
	public void testExecuteSelectWithNativeQuery() throws ParseException {
		super.testExecuteSelectWithNativeQuery();
	}

	@Test
	@Override
	public void testExecuteUpdateWithNativeNamedQuery() {
		super.testExecuteUpdateWithNativeNamedQuery();
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
