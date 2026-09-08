INSERT INTO permissions(code,name,description,module) VALUES
('BRANCH_VIEW','View branches','View branches in the accessible organization scope','BRANCH'),
('BRANCH_CREATE','Create branches','Create branches for organizations','BRANCH'),
('BRANCH_UPDATE','Update branches','Update branch data','BRANCH'),
('CUSTOMER_VIEW','View business customers','View branch business customers','CUSTOMER'),
('CUSTOMER_CREATE','Create business customers','Create branch business customers','CUSTOMER'),
('CUSTOMER_UPDATE','Update business customers','Update branch business customers','CUSTOMER') ON CONFLICT(code) DO NOTHING;
INSERT INTO roles(name,description,system_role,active) VALUES
('CUSTOMER','Business customer user',true,true),('CUSTOMER_ADMIN','Business customer administrator',true,true),('EMPLOYEE','Branch employee',true,true) ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE r.name='SUPER_ADMIN' AND p.code IN ('BRANCH_VIEW','BRANCH_CREATE','BRANCH_UPDATE','CUSTOMER_VIEW','CUSTOMER_CREATE','CUSTOMER_UPDATE') ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('BRANCH_VIEW','CUSTOMER_VIEW','CUSTOMER_CREATE','CUSTOMER_UPDATE','USER_VIEW','USER_CREATE','USER_UPDATE') WHERE r.name='ORGANIZATION_ADMIN' ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('CUSTOMER_VIEW','ORDER_VIEW','ORDER_CREATE','ORDER_UPDATE','ORDER_SUBMIT','PRODUCT_VIEW') WHERE r.name='CUSTOMER' ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('CUSTOMER_VIEW','CUSTOMER_CREATE','CUSTOMER_UPDATE','USER_VIEW','USER_CREATE','USER_UPDATE','ORDER_VIEW','ORDER_CREATE','ORDER_UPDATE','ORDER_SUBMIT','PRODUCT_VIEW') WHERE r.name='CUSTOMER_ADMIN' ON CONFLICT DO NOTHING;
INSERT INTO role_permissions(role_id,permission_id) SELECT r.id,p.id FROM roles r JOIN permissions p ON p.code IN ('BRANCH_VIEW','CUSTOMER_VIEW','ORDER_VIEW','ORDER_CREATE','ORDER_UPDATE','ORDER_SUBMIT','PRODUCT_VIEW','INVENTORY_VIEW') WHERE r.name='EMPLOYEE' ON CONFLICT DO NOTHING;
