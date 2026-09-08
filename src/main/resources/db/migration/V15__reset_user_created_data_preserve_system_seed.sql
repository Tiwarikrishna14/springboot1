-- DEVELOPMENT RESET ONLY.
-- Removes user-created tenant data while preserving the fixed system organization,
-- the fixed super-admin user, global default roles, and permissions.

DELETE FROM audit_logs;
DELETE FROM refresh_tokens;
DELETE FROM password_reset_tokens;

-- Remove organization-specific role assignments and roles.
DELETE FROM role_permissions
WHERE role_id IN (SELECT id FROM roles WHERE organization_id IS NOT NULL);
DELETE FROM user_roles
WHERE user_id <> '00000000-0000-0000-0000-000000000002'
   OR role_id IN (SELECT id FROM roles WHERE organization_id IS NOT NULL);
DELETE FROM roles
WHERE organization_id IS NOT NULL;

-- Delete all users except the system super-admin.
DELETE FROM users
WHERE id <> '00000000-0000-0000-0000-000000000002';

-- Delete branch/customer hierarchy before deleting organizations.
DELETE FROM business_customer_locations;
DELETE FROM business_customers;
DELETE FROM branches;
DELETE FROM organizations
WHERE id <> '00000000-0000-0000-0000-000000000001';
