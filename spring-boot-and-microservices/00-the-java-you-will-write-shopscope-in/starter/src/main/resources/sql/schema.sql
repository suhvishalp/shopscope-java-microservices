-- The ShopScope price table, for Demo 0 Lab 8 (plain JDBC). Run against a PostgreSQL started with:
--   docker run --rm -d --name shopscope-pg -e POSTGRES_PASSWORD=shopscope -e POSTGRES_USER=shopscope -e POSTGRES_DB=shopscope -p 5432:5432 postgres:17
--   docker exec -i shopscope-pg psql -U shopscope -d shopscope < src/main/resources/sql/schema.sql

create table if not exists product (
    sku         varchar(32)  primary key,
    name        varchar(200) not null,
    category    varchar(64)  not null,
    description text         not null default ''
);

create table if not exists price (
    id          bigint generated always as identity primary key,
    sku         varchar(32)    not null references product (sku),
    amount      numeric(12, 2) not null check (amount >= 0),
    currency    char(3)        not null,
    scope_type  varchar(16)    not null check (scope_type in ('BASE', 'COUNTRY', 'CHANNEL', 'CUSTOMER_GROUP')),
    scope_value varchar(64)
);

create index if not exists price_sku_currency on price (sku, currency);

insert into product (sku, name, category, description) values
    ('SKU-TEA-001', 'Assam breakfast tea 250g', 'tea', 'Strong, malty, the one for mornings.'),
    ('SKU-MUG-001', 'ShopScope stoneware mug', 'kitchen', 'Holds 350 ml and a lot of opinions.'),
    ('SKU-KET-001', 'Electric kettle 1.7 L', 'kitchen', 'Boils a litre in under three minutes.'),
    ('SKU-BIS-001', 'Oat biscuits 400g', 'snacks', 'Dunk responsibly.')
on conflict do nothing;

insert into price (sku, amount, currency, scope_type, scope_value) values
    ('SKU-TEA-001',  349.00, 'INR', 'BASE', null),
    ('SKU-TEA-001',  299.00, 'INR', 'CUSTOMER_GROUP', 'wholesale'),
    ('SKU-TEA-001',    4.50, 'GBP', 'BASE', null),
    ('SKU-MUG-001',  499.00, 'INR', 'BASE', null),
    ('SKU-MUG-001',  449.00, 'INR', 'CHANNEL', 'app'),
    ('SKU-KET-001', 2499.00, 'INR', 'BASE', null),
    ('SKU-KET-001', 2299.00, 'INR', 'COUNTRY', 'IN'),
    ('SKU-BIS-001',  129.00, 'INR', 'BASE', null);

-- Lab 8, step 3: which price should a wholesale customer in India, on the app, pay for the kettle? Ask SQL first:
-- select sku, amount, scope_type, scope_value from price where sku = 'SKU-KET-001' and currency = 'INR';
