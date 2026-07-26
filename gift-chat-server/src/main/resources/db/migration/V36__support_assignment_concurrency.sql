CREATE TABLE support_assignment_guard (
    id INT PRIMARY KEY,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO support_assignment_guard (id, updated_at)
VALUES (1, CURRENT_TIMESTAMP);

CREATE UNIQUE INDEX ux_support_conversation_customer
    ON support_conversation (customer_user_id);
