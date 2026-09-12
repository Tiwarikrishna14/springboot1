INSERT INTO roles (name, description, system_role, active)
VALUES ('BRANCH_ADMIN', 'Administrator for branch-level users, customers, products, orders, and inventory', true, true)
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'BRANCH_VIEW', 'BRANCH_UPDATE',
    'CUSTOMER_VIEW', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE',
    'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DISABLE',
    'ROLE_VIEW', 'ROLE_ASSIGN',
    'PRODUCT_VIEW', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'PRODUCT_DELETE',
    'ORDER_VIEW', 'ORDER_CREATE', 'ORDER_UPDATE', 'ORDER_SUBMIT', 'ORDER_REVIEW', 'ORDER_APPROVE', 'ORDER_REJECT',
    'INVENTORY_VIEW', 'INVENTORY_UPDATE',
    'STOCK_REQUEST_VIEW', 'STOCK_REQUEST_CREATE', 'STOCK_REQUEST_APPROVE'
)
WHERE r.name = 'BRANCH_ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;
