CREATE TABLE products(
    id varchar(36) primary key,
    name varchar(200) not null ,
    description TEXT,
    price decimal(10,2) not null ,
    stock_quantity int not null ,
    category varchar(100),
    image_url varchar(500),
    tenant_id varchar(36) not null ,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at timestamp default current_timestamp,
    updated_at timestamp,
    constraint fk_product_tenant foreign key (tenant_id) references tenants(id)
);