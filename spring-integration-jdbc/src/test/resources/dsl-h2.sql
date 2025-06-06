create table inbound
(
    id     int,
    status int
);

insert into inbound
values (1, 2);

insert into inbound
values (2, 2);

create table outbound
(
    id     varchar(100),
    status int,
    name   varchar(20)
);

create table outbound_gateway
(
    id     int,
    status int
);

insert into outbound_gateway
values (1, 2);

insert into outbound_gateway
values (2, 2);

