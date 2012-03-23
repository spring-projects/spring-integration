/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.outbound;

import java.util.List;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.test.JpaTestUtils;
import org.springframework.integration.jpa.test.entity.Student;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *  
 * @author Gunnar Hillert
 * @since 2.2
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TransactionConfiguration(transactionManager="transactionManager", defaultRollback=true)
public class JpaMessageHandlerTests {
    
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	DataSource dataSource;
	
	@Autowired
	private PlatformTransactionManager transactionManager;
	
	@Test
	@DirtiesContext
	public void saveEntityWithMerge() throws InterruptedException {

		List<?> results1 = new JdbcTemplate(dataSource).queryForList("Select * from Student");
		Assert.assertNotNull(results1);
		Assert.assertTrue(results1.size() == 2);
		
		JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(Student.class);
		jpaExecutor.afterPropertiesSet();
		
		JpaMessageHandler jpaMessageHandler = new JpaMessageHandler(jpaExecutor);
		
		Student testStudent = JpaTestUtils.getTestStudent();
		Message<Student> message = MessageBuilder.withPayload(testStudent).build();
		
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		
		TransactionStatus status = transactionManager.getTransaction(def);
		jpaMessageHandler.handleMessage(message);
		transactionManager.commit(status);
		
		List<?> results2 = new JdbcTemplate(dataSource).queryForList("Select * from Student");
		Assert.assertNotNull(results2);
		Assert.assertTrue(results2.size() == 3);
		
		Assert.assertNotNull(testStudent.getRollNumber());
		
		Assert.assertNotNull(testStudent.getRollNumber());
	}
	
	@Test
	public void saveEntityWithPersist() throws InterruptedException {

		List<?> results1 = new JdbcTemplate(dataSource).queryForList("Select * from Student");
		Assert.assertNotNull(results1);
		Assert.assertTrue(results1.size() == 2);
		
		JpaExecutor jpaExecutor = new JpaExecutor(entityManager);
		jpaExecutor.setEntityClass(Student.class);
		jpaExecutor.setPersistMode(PersistMode.PERSIST);
		jpaExecutor.afterPropertiesSet();
		
		JpaMessageHandler jpaMessageHandler = new JpaMessageHandler(jpaExecutor);
		
		Student testStudent = JpaTestUtils.getTestStudent();
		
		Assert.assertNull(testStudent.getRollNumber());
		
		Message<Student> message = MessageBuilder.withPayload(testStudent).build();
		
		jpaMessageHandler.afterPropertiesSet();
		
		DefaultTransactionDefinition def = new DefaultTransactionDefinition();
		// explicitly setting the transaction name is something that can only be done programmatically
		def.setName("SomeTxName");
		def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
		
		TransactionStatus status = transactionManager.getTransaction(def);
		jpaMessageHandler.handleMessage(message);
		transactionManager.commit(status);
		
		List<?> results2 = new JdbcTemplate(dataSource).queryForList("Select * from Student");
		Assert.assertNotNull(results2);
		Assert.assertTrue(results2.size() == 3);
		
		Assert.assertNull(testStudent.getRollNumber());
	}	
}