package org.springframework.integration.activiti;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


/**
 * Simple bean that you want to use from Activiti.
 * You've configured it all from Spring and don't want to
 * have to rencode all the configuration inside of Activiti
 */
public class CustomerService {

	private DataSource dataSource ;
	private SimpleJdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new SimpleJdbcTemplate ( dataSource);
	}

	public void createCustomer( String firstName, String lastName , String email ){
		Map<String,Object> parms = new HashMap<String,Object>() ;
		parms.put( "fn",  firstName ) ;
		parms.put( "ln", lastName);
		parms.put( "email", email );
		
		this.jdbcTemplate.update(
				"INSERT INTO CUSTOMER( FIRST_NAME, LAST_NAME, EMAIL) VALUES(:fn,:ln,:email)" ,
				parms );
	}
}


