START TRANSACTION;

DELETE FROM script_favorite
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM script_review
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM review
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM recommendation_log
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM hot_ranking
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM script_role
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM script_tag
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM group_order
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM reservation_order
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM script_schedule
WHERE script_id IN (
    SELECT id FROM (
        SELECT id
        FROM script
        WHERE name IN (
            '幽灵旅馆',
            '消失的证人',
            '密室疑云',
            '真相只有一个',
            '告白',
            '似水流年',
            '魔法学院',
            '派对之夜',
            '黑暗骑士',
            '梦回唐朝'
        )
    ) AS target_scripts
);

DELETE FROM script
WHERE name IN (
    '幽灵旅馆',
    '消失的证人',
    '密室疑云',
    '真相只有一个',
    '告白',
    '似水流年',
    '魔法学院',
    '派对之夜',
    '黑暗骑士',
    '梦回唐朝'
);

COMMIT;
