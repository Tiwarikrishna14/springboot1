CREATE TABLE order_number_counters (
    business_customer_id UUID PRIMARY KEY REFERENCES business_customers(id),
    company_code VARCHAR(3) NOT NULL,
    next_sequence BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(40) NOT NULL UNIQUE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    branch_id UUID REFERENCES branches(id),
    business_customer_id UUID NOT NULL REFERENCES business_customers(id),
    business_customer_location_id UUID REFERENCES business_customer_locations(id),
    business_customer_code VARCHAR(64) NOT NULL,
    business_customer_name VARCHAR(255) NOT NULL,
    notes VARCHAR(1000),
    remarks VARCHAR(1000),
    priority VARCHAR(32),
    location VARCHAR(255),
    reference_number VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    expected_delivery_date DATE,
    created_by UUID NOT NULL REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_orders_status CHECK (status IN (
        'DRAFT',
        'CREATED',
        'CHANGES_REQUESTED',
        'APPROVED',
        'SUBMITTED',
        'REJECTED',
        'PENDING',
        'CONFIRMED',
        'DELIVERED',
        'ABANDONED'
    ))
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    product_code VARCHAR(100) NOT NULL,
    product_description VARCHAR(500),
    quantity DECIMAL(17,3) NOT NULL,
    unit_price DECIMAL(17,2) NOT NULL,
    line_remark VARCHAR(500),
    line_total DECIMAL(19,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_order_items_quantity CHECK (quantity > 0),
    CONSTRAINT chk_order_items_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ux_order_items_order_product UNIQUE (order_id, product_id)
);

CREATE TABLE order_approvers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    approval_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    remark VARCHAR(1000),
    acted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_order_approvers_order_user UNIQUE (order_id, user_id),
    CONSTRAINT chk_order_approvers_status CHECK (approval_status IN (
        'PENDING',
        'APPROVED',
        'CHANGES_REQUESTED',
        'REJECTED'
    ))
);

CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_business_customer_id ON orders(business_customer_id);
CREATE INDEX idx_orders_branch_id ON orders(branch_id);
CREATE INDEX idx_orders_created_by ON orders(created_by);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_submitted_visibility ON orders(status, business_customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_order_approvers_order_id ON order_approvers(order_id);
CREATE INDEX idx_order_approvers_user_id ON order_approvers(user_id);
