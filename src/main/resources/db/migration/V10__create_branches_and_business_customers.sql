CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_code VARCHAR(64) NOT NULL, name VARCHAR(255) NOT NULL, city VARCHAR(120), address VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by UUID, updated_by UUID,
    CONSTRAINT ux_branches_org_code UNIQUE (organization_id, branch_code), CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX idx_branches_organization_id ON branches(organization_id);
CREATE TABLE business_customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), organization_id UUID NOT NULL REFERENCES organizations(id), branch_id UUID NOT NULL REFERENCES branches(id),
    customer_code VARCHAR(64) NOT NULL, name VARCHAR(255) NOT NULL, email VARCHAR(320), phone VARCHAR(40), status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now(), created_by UUID, updated_by UUID,
    CONSTRAINT ux_business_customers_branch_code UNIQUE(branch_id, customer_code), CONSTRAINT chk_business_customers_status CHECK(status IN ('ACTIVE','INACTIVE'))
);
CREATE INDEX idx_business_customers_branch_id ON business_customers(branch_id);
ALTER TABLE users ADD COLUMN branch_id UUID REFERENCES branches(id);
ALTER TABLE users ADD COLUMN business_customer_id UUID REFERENCES business_customers(id);
CREATE INDEX idx_users_branch_id ON users(branch_id);
CREATE INDEX idx_users_business_customer_id ON users(business_customer_id);
