CREATE TABLE approval_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type VARCHAR(50) NOT NULL,
    scope_id UUID NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    configuration JSONB NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT ux_approval_policies_scope UNIQUE (scope_type, scope_id, policy_type)
);

CREATE INDEX idx_approval_policies_lookup
    ON approval_policies(scope_type, scope_id, policy_type, active);

ALTER TABLE orders ADD COLUMN approval_policy_snapshot JSONB;
ALTER TABLE order_approvers ADD COLUMN approval_level INTEGER NOT NULL DEFAULT 1;
ALTER TABLE order_approvers ADD CONSTRAINT chk_order_approver_level CHECK (approval_level > 0);

