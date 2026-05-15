CREATE TABLE users(
    id varchar(36) not null,
    name varchar(100) not null,
    email varchar(150) not null unique,
    password varchar(255) not null,
    role varchar(20) not null default 'CUSTOMER',
    tenant_id varchar(36) not null,
    created_at timestamp default current_timestamp,
    constraint fk_tenant foreign key(tenant_id) references tenants(id)
)