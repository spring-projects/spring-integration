/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.jpa.test.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
	private LocalDateTime readAt;

	public int getRollNumber() {
		return rollNumber;
	}

	public void setRollNumber(int rollNumber) {
		this.rollNumber = rollNumber;
	}

	public LocalDateTime getReadAt() {
		return readAt;
	}

	public void setReadAt(LocalDateTime readAt) {
		this.readAt = readAt;
	}

}
