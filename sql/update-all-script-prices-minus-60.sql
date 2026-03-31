UPDATE script
SET price = GREATEST(price - 60, 1)
WHERE is_deleted = 0;
