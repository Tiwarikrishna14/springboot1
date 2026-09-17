ALTER TABLE business_customer_locations
    ADD COLUMN permanent_address VARCHAR(500),
    ADD COLUMN corresponding_address VARCHAR(500),
    ADD COLUMN same_as_permanent_address BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE business_customer_locations
    ALTER COLUMN branch_id DROP NOT NULL;
