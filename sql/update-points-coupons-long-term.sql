UPDATE coupon
SET
    name = CASE id
        WHEN 1 THEN '满100减10'
        WHEN 2 THEN '满200减25'
        WHEN 3 THEN '满300减40'
        WHEN 4 THEN '满500减80'
        ELSE name
    END,
    type = 1,
    discount_value = CASE id
        WHEN 1 THEN 10.00
        WHEN 2 THEN 25.00
        WHEN 3 THEN 40.00
        WHEN 4 THEN 80.00
        ELSE discount_value
    END,
    min_amount = CASE id
        WHEN 1 THEN 100.00
        WHEN 2 THEN 200.00
        WHEN 3 THEN 300.00
        WHEN 4 THEN 500.00
        ELSE min_amount
    END,
    exchange_points = CASE id
        WHEN 1 THEN 100
        WHEN 2 THEN 250
        WHEN 3 THEN 400
        WHEN 4 THEN 800
        ELSE exchange_points
    END,
    valid_start = '2024-01-01 00:00:00',
    valid_end = '2099-12-31 23:59:59',
    status = 1
WHERE id IN (1, 2, 3, 4);
