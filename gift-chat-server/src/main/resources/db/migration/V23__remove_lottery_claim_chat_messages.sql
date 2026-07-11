DELETE FROM support_message
WHERE UPPER(sender_role) = 'SYSTEM'
  AND (
      content LIKE 'Physical prize fulfillment %'
      OR content LIKE 'Lottery withdrawal request %'
  );
