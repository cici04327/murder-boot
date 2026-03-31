START TRANSACTION;

UPDATE store
SET
    name = CASE id
        WHEN 1 THEN '探案馆·人民广场店'
        WHEN 2 THEN '悬疑社·虹桥枢纽店'
        WHEN 3 THEN '密室逃脱·松江大学城店'
        WHEN 4 THEN '剧本空间·临港滴水湖店'
        WHEN 5 THEN '推理馆·宝山顾村店'
        WHEN 6 THEN '恐怖屋·崇明南门店'
        ELSE name
    END,
    address = CASE id
        WHEN 1 THEN '上海市黄浦区南京东路100号新世界大厦3楼'
        WHEN 2 THEN '上海市闵行区申长路688号虹桥天地购物中心4楼'
        WHEN 3 THEN '上海市松江区文汇路955号松江万达广场3楼'
        WHEN 4 THEN '上海市浦东新区环湖西一路88号临港天地5楼'
        WHEN 5 THEN '上海市宝山区陆翔路111弄绿地缤纷城4楼'
        WHEN 6 THEN '上海市崇明区南门路258号八一路商圈2楼'
        ELSE address
    END,
    latitude = CASE id
        WHEN 1 THEN 31.231706
        WHEN 2 THEN 31.197340
        WHEN 3 THEN 31.060420
        WHEN 4 THEN 30.899820
        WHEN 5 THEN 31.346920
        WHEN 6 THEN 31.622860
        ELSE latitude
    END,
    longitude = CASE id
        WHEN 1 THEN 121.473701
        WHEN 2 THEN 121.316960
        WHEN 3 THEN 121.226510
        WHEN 4 THEN 121.928940
        WHEN 5 THEN 121.364570
        WHEN 6 THEN 121.397140
        ELSE longitude
    END
WHERE id IN (1, 2, 3, 4, 5, 6);

COMMIT;
