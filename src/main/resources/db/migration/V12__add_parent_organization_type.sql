ALTER TABLE organizations DROP CONSTRAINT IF EXISTS chk_organizations_type;
ALTER TABLE organizations ADD CONSTRAINT chk_organizations_type CHECK (organization_type IN ('PARENT', 'SYSTEM', 'CUSTOMER', 'SUPPLIER'));
