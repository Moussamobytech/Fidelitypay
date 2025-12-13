CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id VARCHAR(255),
    operator VARCHAR(255),
    status VARCHAR(50),
    cost DOUBLE,
    timestamp DATETIME
);

-- Ajouter d'autres tables similaires pour route, log_entry, country_option