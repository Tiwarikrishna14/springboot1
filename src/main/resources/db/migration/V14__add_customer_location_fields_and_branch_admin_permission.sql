ALTER TABLE business_customers ADD COLUMN city VARCHAR(120);
ALTER TABLE business_customers ADD COLUMN state VARCHAR(120);
ALTER TABLE business_customers ADD COLUMN address VARCHAR(500);
ALTER TABLE business_customers ADD COLUMN pincode VARCHAR(20);

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('BRANCH_CREATE', 'BRANCH_UPDATE')
WHERE r.name = 'ORGANIZATION_ADMIN'
ON CONFLICT DO NOTHING;
