insert into Student(firstName,lastName,	gender,	dateOfBirth,lastUpdated)
values
('First One','Last One','M','1980-1-1',NOW());
insert into Student(firstName,lastName,	gender,	dateOfBirth,lastUpdated)
values
('First Two','Last Two','F','1984-1-1',NOW());
insert into StudentReadStatus
select rollNumber,null from Student;