INSERT INTO organizations (
    id,
    organization_code,
    name,
    organization_type,
    email,
    phone,
    status
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'SYSTEM',
    'System Organization',
    'SYSTEM',
    lower('${initialAdminEmail}'),
    null,
    'ACTIVE'
) ON CONFLICT (organization_code) DO UPDATE
SET email = EXCLUDED.email,
    updated_at = now();

INSERT INTO users (
    id,
    organization_id,
    first_name,
    last_name,
    email,
    phone,
    password_hash,
    status,
    email_verified
) VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001',
    'Super',
    'Admin',
    lower('${initialAdminEmail}'),
    null,
    crypt('${initialAdminPassword}', gen_salt('bf', 12)),
    'ACTIVE',
    true
) ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'SUPER_ADMIN'
WHERE u.email = lower('${initialAdminEmail}')
ON CONFLICT (user_id, role_id) DO NOTHING;
