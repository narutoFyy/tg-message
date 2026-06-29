INSERT INTO app_user (id, username, email, phone, password_hash, role_code, status_code, created_at, updated_at)
VALUES
('user-1', 'cardnova_user', 'demo@cardnova.app', '13800138000', '{noop}demo12345', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user-2', 'gift_hunter', NULL, '+234 809 000 1234', '{noop}demo12345', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user-3', 'tradewithmina', NULL, '+44 7400 221199', '{noop}demo12345', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user-4', 'spam_rate_88', NULL, '+86 135 0000 2222', '{noop}demo12345', 'USER', 'BLOCKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('agent-1', 'support_luna', 'luna@cardnova.app', NULL, '{noop}demo12345', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('agent-2', 'support_angela', 'angela@cardnova.app', NULL, '{noop}demo12345', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('agent-3', 'support_kai', 'kai@cardnova.app', NULL, '{noop}demo12345', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('agent-4', 'support_mina', 'mina@cardnova.app', NULL, '{noop}demo12345', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('agent-5', 'support_zoe', 'zoe@cardnova.app', NULL, '{noop}demo12345', 'AGENT', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('admin-1', 'admin_mia', 'admin@cardnova.app', NULL, '{noop}demo12345', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO gift_card_rate (id, card_name, region_code, rate_value, status_code, updated_at, updated_by)
VALUES
('rate-1', 'Apple(itunes)', 'NG', '1$ ≈ ₦1051.75', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-2', 'Steam', 'NG', '1$ ≈ ₦886.88', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-3', 'Razer Gold', 'NG', '1$ ≈ ₦1076.56', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-4', 'Zelle', 'NG', '1$ ≈ ₦552.77', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-5', 'Chime', 'NG', '1$ ≈ ₦1203.35', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-6', 'Xbox', 'NG', '1$ ≈ ₦890.67', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-7', 'eBay', 'NG', '1$ ≈ ₦740.96', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-8', 'Sephora', 'NG', '1$ ≈ ₦1053.64', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-9', 'Google', 'NG', '1$ ≈ ₦397.96', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-10', 'Vanilla', 'NG', '1$ ≈ ₦568.51', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-11', 'American Express', 'NG', '1$ ≈ ₦473.76', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-12', 'Apple(itunes)', 'IN', '1$ ≈ ₹82.40', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-13', 'Steam', 'IN', '1$ ≈ ₹78.10', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-14', 'Razer Gold', 'IN', '1$ ≈ ₹83.75', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-15', 'Apple(itunes)', 'CM', '1$ ≈ XAF 605.00', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-16', 'Steam', 'CM', '1$ ≈ XAF 588.00', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-17', 'Razer Gold', 'CM', '1$ ≈ XAF 615.00', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-18', 'Apple(itunes)', 'GH', '1$ ≈ GH₵14.20', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-19', 'Steam', 'GH', '1$ ≈ GH₵13.85', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1'),
('rate-20', 'Razer Gold', 'GH', '1$ ≈ GH₵14.70', 'ACTIVE', CURRENT_TIMESTAMP, 'admin-1');

INSERT INTO support_conversation (id, customer_user_id, assigned_agent_id, assignment_status, created_at, updated_at)
VALUES
('support-1', 'user-1', 'agent-1', 'MANUALLY_ASSIGNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO support_message (id, conversation_id, sender_user_id, sender_role, message_type, content, created_at)
VALUES
('support-msg-1', 'support-1', 'agent-1', 'SUPPORT', 'TEXT', 'Welcome to CardBrother. I am Luna from support and I can help you with rates or payout questions.', CURRENT_TIMESTAMP),
('support-msg-2', 'support-1', 'agent-1', 'SUPPORT', 'TEXT', 'This prototype already supports rates, direct chat, support chat, and blacklist lookup.', CURRENT_TIMESTAMP);

INSERT INTO friendship (id, requester_user_id, addressee_user_id, status_code, created_at, updated_at)
VALUES
('friendship-1', 'user-1', 'user-2', 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('friendship-2', 'user-1', 'user-3', 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO direct_message (id, friendship_id, sender_user_id, message_type, content, created_at)
VALUES
('dm-1', 'friendship-1', 'user-2', 'TEXT', 'I saw you trade Apple cards too. Can we compare rates later today?', CURRENT_TIMESTAMP),
('dm-2', 'friendship-1', 'user-1', 'TEXT', 'Sure. I will sort today''s rate sheet and send it over.', CURRENT_TIMESTAMP),
('dm-3', 'friendship-2', 'user-3', 'IMAGE', '[image placeholder]', CURRENT_TIMESTAMP);

INSERT INTO trade_order (id, order_no, owner_user_id, counterparty_user_id, friendship_id, card_name, face_value, payout_amount, status_code, note, created_at, updated_at)
VALUES
('trade-1', 'CB240527-001', 'user-1', 'user-2', 'friendship-1', 'Apple(itunes)', '$200', 'NGN 198500', 'PENDING', 'Waiting for card screenshots and receipt confirmation.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('trade-2', 'CB240527-002', 'user-1', 'user-3', 'friendship-2', 'Steam', '$100', 'NGN 88400', 'PROCESSING', 'Counterparty is checking the code balance before payout.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('trade-3', 'CB240527-003', 'user-2', 'user-1', 'friendship-1', 'Razer Gold', '$50', 'NGN 53200', 'COMPLETED', 'Settled successfully this morning.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO blacklist_entry (id, owner_user_id, blocked_user_id, blocked_phone_snapshot, reason_note, created_at)
VALUES
('black-1', 'user-1', 'user-4', '+86 135 0000 2222', 'Repeated payout spam and abusive messages.', CURRENT_TIMESTAMP);
