drop table StudentReadStatus;
drop table Student;

create table Student(
	rollNumber integer identity primary key ,
	firstName 	varchar(50),
	lastName	varchar(50),
	gender		varchar(1),
	dateOfBirth	date,
	lastUpdated	timestamp
);
create table StudentReadStatus (
	rollNumber integer,	
	readAt timestamp
);
