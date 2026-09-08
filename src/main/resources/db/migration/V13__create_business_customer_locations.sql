CREATE TABLE business_customer_locations (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), business_customer_id UUID NOT NULL REFERENCES business_customers(id), organization_id UUID NOT NULL REFERENCES organizations(id), branch_id UUID NOT NULL REFERENCES branches(id),
 location_code VARCHAR(64) NOT NULL, location_name VARCHAR(255) NOT NULL, city VARCHAR(120) NOT NULL, state VARCHAR(120), address VARCHAR(500), pincode VARCHAR(20), status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT ux_business_customer_locations_code UNIQUE(business_customer_id,location_code), CONSTRAINT chk_business_customer_locations_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
ALTER TABLE users ADD COLUMN business_customer_location_id UUID REFERENCES business_customer_locations(id);
CREATE INDEX idx_business_customer_locations_customer ON business_customer_locations(business_customer_id);
CREATE INDEX idx_users_business_customer_location ON users(business_customer_location_id);
