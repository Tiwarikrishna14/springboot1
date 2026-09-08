CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,

    category VARCHAR(100) NOT NULL,

    customer_sell_code VARCHAR(100) NOT NULL,

    nav_item_code VARCHAR(100) NOT NULL,

    item_description VARCHAR(500) NOT NULL,

    uom VARCHAR(20) NOT NULL,

    unit_rate DECIMAL(17,2) NOT NULL
);
