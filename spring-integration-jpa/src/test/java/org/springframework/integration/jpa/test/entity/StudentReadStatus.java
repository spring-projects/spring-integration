/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.test.entity;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * The Entity for Student read status
 *
 * @author Amol Nayak
 *
 */
@Entity
@Table(name = "StudentReadStatus")
public class StudentReadStatus {

	@Id
	@Column(name = "rollNumber")
	private int rollNumber;

	@Column(name = "readAt")
	@Temporal(TemporalType.TIMESTAMP)
	private Date readAt;

	public int getRollNumber() {
		return rollNumber;
	}

	public void setRollNumber(int rollNumber) {
		this.rollNumber = rollNumber;
	}

	public Date getReadAt() {
		return readAt;
	}

	public void setReadAt(Date readAt) {
		this.readAt = readAt;
	}

}
