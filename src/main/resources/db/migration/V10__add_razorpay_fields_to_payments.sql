-- V10__add_razorpay_fields_to_payments.sql
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS razorpay_order_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS razorpay_payment_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS razorpay_signature VARCHAR(300);

CREATE INDEX IF NOT EXISTS idx_payments_razorpay_order
    ON payments(razorpay_order_id);