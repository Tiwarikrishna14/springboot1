CREATE TABLE product_bulk_upload_checkpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL,
    customer_sell_code VARCHAR(100) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    total_rows INTEGER NOT NULL DEFAULT 0,
    processed_rows INTEGER NOT NULL DEFAULT 0,
    failed_row_number INTEGER,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_product_upload_checkpoint UNIQUE(owner_id, customer_sell_code, file_hash),
    CONSTRAINT chk_product_upload_checkpoint_status CHECK(status IN ('PROCESSING','FAILED','COMPLETED'))
);

