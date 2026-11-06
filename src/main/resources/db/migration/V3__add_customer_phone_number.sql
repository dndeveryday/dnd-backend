ALTER TABLE customer_profiles
    ADD COLUMN phone_number VARCHAR(20);

CREATE UNIQUE INDEX uq_customer_profiles_phone_number
    ON customer_profiles (phone_number)
    WHERE phone_number IS NOT NULL;