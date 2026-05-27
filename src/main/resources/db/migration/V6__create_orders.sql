CREATE TABLE orders(
id varchar(36) primary key,
customer_id varchar(36) not null,
customer_email varchar(150) not null,
tenant_id varchar(36) not null,
status varchar(20) not null default 'PENDING',
total_amount decimal(10,2) not null,
created_at timestamp default current_timestamp,
updated_at timestamp,
constraint fk_order_customer foreign key(customer_id) references users(id),
constraint fk_order_tenant foreign key (tenant_id) references tenants(id)
);

CREATE TABLE order_items (
id varchar(36) primary key,
order_id varchar(36) not null,
product_id varchar(36) not null,
product_name varchar(200) not null,
quantity INT not null,
unit_price decimal(10,2) not null,
total_price decimal(10,2) not null,
CONSTRAINT fk_order_item_order FOREIGN KEY(order_id) references orders(id) on delete cascade,
CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) references products(id)
);