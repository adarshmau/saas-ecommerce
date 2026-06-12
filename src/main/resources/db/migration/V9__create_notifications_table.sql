Create Table notifications(
     id varchar(36) primary key,
     user_id varchar(36) not null,
     tenant_id varchar(36) not null,
     order_id varchar(36),
     type varchar(50) not null,
     message TEXT not null,
     is_read boolean not null default false,
     read_at timestamp,
     created_at timestamp not null,
     updated_at timestamp
   );

create index idx_notifications_user on notifications(user_id,tenant_id);
create index idx_notifications_unread on notifications(user_id,tenant_id,is_read);
