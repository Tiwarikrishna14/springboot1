INSERT INTO roles (name, description, system_role, active) VALUES
('SUPER_ADMIN', 'Global platform administrator', true, true),
('ORGANIZATION_ADMIN', 'Administrator for one customer or supplier organization', true, true),
('ORDER_CREATOR', 'Creates and maintains draft orders', true, true),
('ORDER_REVIEWER', 'Reviews submitted orders before approval', true, true),
('MID_APPROVER', 'Performs mid-level order approvals', true, true),
('FINAL_APPROVER', 'Performs final order approvals', true, true),
('INVENTORY_MANAGER', 'Manages inventory records and stock requests', true, true),
('CUSTOMER_USER', 'Default customer user role', true, true)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DISABLE',
    'ROLE_VIEW', 'ROLE_CREATE', 'ROLE_UPDATE', 'ROLE_ASSIGN',
    'ORGANIZATION_VIEW', 'ORGANIZATION_UPDATE',
    'PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
    'ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_SUBMIT', 'ORDER_REVIEW', 'ORDER_APPROVE', 'ORDER_REJECT',
    'INVENTORY_VIEW', 'INVENTORY_UPDATE',
    'STOCK_REQUEST_VIEW', 'STOCK_REQUEST_CREATE', 'STOCK_REQUEST_APPROVE',
    'AUDIT_VIEW'
)
WHERE r.name = 'ORGANIZATION_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_SUBMIT')
WHERE r.name = 'ORDER_CREATOR'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ORDER_VIEW', 'ORDER_REVIEW')
WHERE r.name = 'ORDER_REVIEWER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ORDER_VIEW', 'ORDER_APPROVE', 'ORDER_REJECT')
WHERE r.name = 'MID_APPROVER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ORDER_VIEW', 'ORDER_APPROVE', 'ORDER_REJECT')
WHERE r.name = 'FINAL_APPROVER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
    'INVENTORY_VIEW', 'INVENTORY_UPDATE',
    'STOCK_REQUEST_VIEW', 'STOCK_REQUEST_CREATE', 'STOCK_REQUEST_APPROVE'
)
WHERE r.name = 'INVENTORY_MANAGER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_SUBMIT', 'PRODUCT_VIEW')
WHERE r.name = 'CUSTOMER_USER'
ON CONFLICT (role_id, permission_id) DO NOTHING;
