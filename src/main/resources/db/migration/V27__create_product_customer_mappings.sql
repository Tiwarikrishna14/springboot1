-- Keep one product master per NAV item code and move customer-specific data to a mapping table.
CREATE TABLE product_customer_mappings (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    business_customer_id UUID NOT NULL REFERENCES business_customers(id) ON DELETE CASCADE,
    product_name VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_product_customer_mapping UNIQUE (product_id, business_customer_id)
);

-- Existing rows become mappings before duplicate master rows are removed.
INSERT INTO product_customer_mappings (product_id, business_customer_id, product_name, status)
SELECT p.id, c.id, p.item_description, COALESCE(p.status, 'ACTIVE')
FROM products p
JOIN business_customers c ON upper(c.customer_code) = upper(p.customer_sell_code)
ON CONFLICT (product_id, business_customer_id) DO NOTHING;

-- Point historical order lines at the first master row for each NAV item code.
WITH duplicate_lines AS (
    SELECT oi.id,
           first_value(oi.id) OVER (
               PARTITION BY oi.order_id, upper(p.nav_item_code)
               ORDER BY oi.id
           ) AS keep_id
    FROM order_items oi
    JOIN products p ON p.id = oi.product_id
)
DELETE FROM order_items oi
USING duplicate_lines duplicate
WHERE oi.id = duplicate.id
  AND duplicate.id <> duplicate.keep_id;

WITH canonical AS (
    SELECT id, MIN(id) OVER (PARTITION BY upper(nav_item_code)) AS canonical_id
    FROM products
)
UPDATE order_items oi
SET product_id = canonical.canonical_id
FROM canonical
WHERE oi.product_id = canonical.id
  AND canonical.id <> canonical.canonical_id;

DELETE FROM products duplicate
USING products canonical
WHERE duplicate.id > canonical.id
  AND upper(duplicate.nav_item_code) = upper(canonical.nav_item_code);

ALTER TABLE products DROP COLUMN customer_sell_code;
CREATE UNIQUE INDEX ux_products_nav_item_code_ci ON products (upper(nav_item_code));
CREATE INDEX idx_product_customer_mappings_customer ON product_customer_mappings (business_customer_id);
