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
package org.springframework.integration.jpa.core;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.jpa.support.parametersource.BeanPropertyParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ExpressionEvaluatingParameterSourceFactory;
import org.springframework.integration.jpa.support.parametersource.ParameterSource;
import org.springframework.integration.jpa.support.parametersource.ParameterSourceFactory;
import org.springframework.util.Assert;

/**
 * Executes Jpa Operations that produces payload objects from the result of the provided:
 * 
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>
 * 
 * When objects are being retrieved, it also possibly to:
 * 
 * <ul>
 *     <li>delete the retrieved object</li>
 * </ul>   

 * @author Gunnar Hillert
 * @since 2.2
 * 
 */
public class JpaExecutor implements InitializingBean {

    private volatile JpaOperations      jpaOperations;
    private volatile List<JpaParameter> jpaParameters;

    private volatile Class<?> entityClass;
    private volatile String   query;
    private volatile boolean  nativeQuery = false;
    private volatile String   namedQuery;
    
    private volatile int maxRows = 0;
    
    private volatile PersistMode persistMode = PersistMode.MERGE;
    
    private volatile ParameterSourceFactory parameterSourceFactory = null;
	private volatile ParameterSource parameterSource;

    private volatile boolean  deleteAfterPoll = false;
    private volatile boolean  deletePerRow = false;
    
    private volatile boolean  expectSingleResult = false;
    
    /**
     * Indicates that whether only the payload of the passed in {@link Message}
     * will be used as a source of parameters. The is 'true' by default because as a
     * default a {@link BeanPropertyJpaParameterSourceFactory} implementation is
     * used for the sqlParameterSourceFactory property.
     */
    private volatile Boolean usePayloadAsParameterSource = null;
    
    //~~~~Constructors~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    /**
     * Constructor taking an {@link EntityManagerFactory} from which the 
     * {@link EntityManager} can be obtained.
     *
     * @param entityManagerFactory Must not be null.
     */
    public JpaExecutor(EntityManagerFactory entityManagerFactory) {
        Assert.notNull(entityManagerFactory, "entityManagerFactory must not be null.");
        
        DefaultJpaOperations defaultJpaOperations = new DefaultJpaOperations();
        defaultJpaOperations.setEntityManagerFactory(entityManagerFactory);
        defaultJpaOperations.afterPropertiesSet();
        
        this.jpaOperations = defaultJpaOperations;
    }

    /**
     * Constructor taking an {@link EntityManager} directly.
     *
     * @param entityManager Must not be null.
     */
    public JpaExecutor(EntityManager entityManager) {
        Assert.notNull(entityManager, "entityManager must not be null.");
        
        DefaultJpaOperations defaultJpaOperations = new DefaultJpaOperations();
        defaultJpaOperations.setEntityManager(entityManager);
        defaultJpaOperations.afterPropertiesSet();
        this.jpaOperations = defaultJpaOperations;
    }

    /**
     * If custom behavior is required a custom implementation of {@link JpaOperations}
     * can be passed in. The implementations themselves typically provide access
     * to the {@link EntityManager}. 
     * 
     * See also {@link DefaultJpaOperations} and {@link AbstractJpaOperations}.
     *
     * @param jpaOperations Must not be null.
     */
    public JpaExecutor(JpaOperations jpaOperations) {
        Assert.notNull(jpaOperations, "jpaOperations must not be null.");
        this.jpaOperations = jpaOperations;
    }
    
    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
    
    /**
     * 
     * Verifies and sets the parameters. E.g. initializes the to be used 
     * {@link ParameterSourceFactory}.
     * 
     */
    public void afterPropertiesSet() {

        if (this.jpaParameters != null ) {

            if (this.parameterSourceFactory == null) {
                ExpressionEvaluatingParameterSourceFactory expressionSourceFactory =
                                              new ExpressionEvaluatingParameterSourceFactory();
                expressionSourceFactory.setParameters(jpaParameters);
                this.parameterSourceFactory = expressionSourceFactory;

            } else {

                if (!(this.parameterSourceFactory instanceof ExpressionEvaluatingParameterSourceFactory)) {
                    throw new IllegalStateException("You are providing 'JpaParameters'. "
                        + "Was expecting the the provided jpaParameterSourceFactory "
                        + "to be an instance of 'ExpressionEvaluatingJpaParameterSourceFactory', "
                        + "however the provided one is of type '" + this.parameterSourceFactory.getClass().getName() + "'");
                }

            }

            if (this.usePayloadAsParameterSource == null) {
                this.usePayloadAsParameterSource = false;
            }

        } else {

            if (this.parameterSourceFactory == null) {
                this.parameterSourceFactory = new BeanPropertyParameterSourceFactory();
            }

            if (this.usePayloadAsParameterSource == null) {
                this.usePayloadAsParameterSource = true;
            }

        }
        
        /*
         * TODO - Still debating this requirement, it might be desirable to
         * automatically derive the entity class from the Message payload or 
         * set the query through a Message Header.
         * 
         */
        if (this.entityClass == null 
        		&& this.query == null
        		&& this.namedQuery == null) {
        	throw new IllegalArgumentException("Please set one of the following properties: "
        			+ "'entityClass' or 'query' or 'namedQuery'.");
        }

    }

    /**
     * Executes the actual Jpa Operation. Call this method, if you need access to
     * process return values. This methods return a Map that contains either 
     * the number of affected entities or the affected entity itself.
     * 
     * Keep in mind that the number of entities effected by the operation may 
     * not necessarily correlate with the number of rows effected in the database.
     *  
     * @param message
     * @return Either the number of affected entities when using a JPAQL query. When using a merge/persist the updated/inserted itself is returned. 
     */
    public Object executeOutboundJpaOperation(final Message<?> message) {
        
    	final Object result;
    	
        if (this.query != null && !this.nativeQuery) {
        	
        	result = this.jpaOperations.executeUpdate(this.query, parameterSourceFactory.createParameterSource(message));
        	
        } else if (this.query != null && this.nativeQuery) {
        	
        	result = this.jpaOperations.executeUpdateWithNativeQuery(this.query, parameterSourceFactory.createParameterSource(message));
        	
        } else if (this.namedQuery != null) {
        	
        	result = this.jpaOperations.executeUpdateWithNamedQuery(this.query, parameterSourceFactory.createParameterSource(message));

        } else if (entityClass != null) {
        	if (PersistMode.MERGE.equals(this.persistMode)) {
        		this.jpaOperations.persist(message.getPayload());
        		result = message.getPayload();
        	} else if (PersistMode.PERSIST.equals(this.persistMode)) {
        		final Object mergedEntity = this.jpaOperations.merge(message.getPayload());
        		result = mergedEntity;
        	} else if (PersistMode.DELETE.equals(this.persistMode)) {
        		this.jpaOperations.delete(message.getPayload());
        		result = message.getPayload();
        	} else {
        		throw new IllegalStateException(String.format("Unsupported PersistMode: '%s'", this.persistMode.name()));
        	}
        } else {
        	result = null;
        }

        return result;
        
    }

    	
    public Object poll(final Message<?> requestMessage) {
    	
    	final Object payload;
    	
    	final List<?> result;
    	
    	if (requestMessage == null) {
    		result = doPoll(this.parameterSource);
    	} else {
    		result = doPoll(this.parameterSourceFactory.createParameterSource(requestMessage));
    	}

        if (result.isEmpty()) {
        	payload = null;
        } else {

        	if (this.expectSingleResult && result.size() == 1) {
                payload = result.iterator().next();
        	} else if (this.expectSingleResult && result.size() > 1) {

        		throw new MessageHandlingException(requestMessage,
        				"The Jpa operation returned more than "
        		      + "1 result object but expectSingleResult was 'true'.");

        	} else {
        		payload = result;
        	}

        }
		
		if (this.deleteAfterPoll) {
			if (this.deletePerRow) {
				this.jpaOperations.delete(result);
			} else {
				this.jpaOperations.deleteInBatch(result);
			}
			
		}
		
		return payload;
    }
    
	/**
	 * Execute the select query and the update query if provided. Returns the
	 * rows returned by the select query. If a RowMapper has been provided, the
	 * mapped results are returned.
	 */
	public Object poll() {
		return poll(null);
	}	
	
	protected List<?> doPoll(ParameterSource jpaQLParameterSource) {
		
		List<?> payload = null;
		
		if (this.query != null && !this.nativeQuery) {
			payload = jpaOperations.getResultListForQuery(this.query, jpaQLParameterSource, 0, maxRows);
		} else if (this.query != null && this.nativeQuery) {
			payload = jpaOperations.getResultListForNativeQuery(this.query, this.entityClass, jpaQLParameterSource, 0, maxRows);
		} else if (this.namedQuery != null) {
			payload = jpaOperations.getResultListForNamedQuery(this.namedQuery, jpaQLParameterSource, 0, maxRows);
		} else {
			payload = jpaOperations.findAll(this.entityClass);
		}
		
		return payload;
	}
    
    //~~~~Setters~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
	/**
	 * Sets the class type which is being used to poll the database or to also 
	 * update the persistence store. 
	 * 
	 * @param entityClass Must not be null.
	 */
	public void setEntityClass(Class<?> entityClass) {
		Assert.notNull(entityClass, "entityClass must not be null.");
		this.entityClass = entityClass;
	}

	/**
	 * 
	 * @param query The provided query must not be null or empty.
	 */
	public void setQuery(String query) {
		Assert.hasText(query, "query must not be null or empty.");
		this.query = query;
	}

	/**
	 * You can also use native Sql queries to poll data from the database. If set
	 * to 'true', the 'query' property will be interpreted as native SQL. 
	 * 
	 * By default the nativeQuery property is 'false', meaning that a provided query 
	 * is based on the Java Persistence Query Language (JPQL).
	 * 
	 * @param nativeQuery Defaults to false,
	 */
	public void setNativeQuery(boolean nativeQuery) {
		this.nativeQuery = nativeQuery;
	}

	/**
	 * A named query can either refer to a named JPQL based query or a native SQL 
	 * query. 
	 * 
	 * @param namedQuery Must not be null or empty
	 */
	public void setNamedQuery(String namedQuery) {
		Assert.hasText(namedQuery, "namedQuery must not be null or empty.");
		this.namedQuery = namedQuery;
	}

	public void setPersistMode(PersistMode persistMode) {
		this.persistMode = persistMode;
	}

	public void setJpaParameters(List<JpaParameter> jpaParameters) {
		this.jpaParameters = jpaParameters;
	}

	public void setUsePayloadAsParameterSource(Boolean usePayloadAsParameterSource) {
		this.usePayloadAsParameterSource = usePayloadAsParameterSource;
	}

	/**
	 * If not set, this property default to 'true', which means that deletion 
	 * occur on a per object basis.
	 * 
	 * If set to 'false' the elements of the payload are deleted as a batch
	 * operation. Be aware that this exhibit issues in regards to cascaded deletes. //TODO further information needed
	 * 
	 * @param deletePerRow Defaults to 'true'.
	 */
	public void setDeletePerRow(boolean deletePerRow) {
		this.deletePerRow = deletePerRow;
	}
	
	/**
	 * If set to 'true', the retrieved objects are deleted from the database upon
	 * being polled. May not work in all situations, e.g. for Native SQL Queries.
	 * 
	 * @param deleteAfterPoll Defaults to 'false'.
	 */
	public void setDeleteAfterPoll(boolean deleteAfterPoll) {
		this.deleteAfterPoll = deleteAfterPoll;
	}

	/**
	 * 
	 * @param maxRows Must not be negative.
	 */
	public void setMaxRows(int maxRows) {
		Assert.isTrue(maxRows >= 0, "maxRows must not be negative.");
		this.maxRows = maxRows;
	}

	/**
	 * 
	 * @param parameterSourceFactory
	 */
	public void setParameterSourceFactory(
			ParameterSourceFactory parameterSourceFactory) {
		Assert.notNull(parameterSourceFactory, "parameterSourceFactory must not be null.");
		this.parameterSourceFactory = parameterSourceFactory;
	}

	/**
	 * 
	 * @param parameterSource
	 */
	public void setParameterSource(ParameterSource parameterSource) {
		Assert.notNull(parameterSource, "parameterSource must not be null.");
		this.parameterSource = parameterSource;
	}

	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
	}
	
	
}
