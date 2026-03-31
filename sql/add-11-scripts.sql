START TRANSACTION;

INSERT INTO script (
    name, category_id, cover, images, description, type, difficulty,
    player_count, duration, price, tags, rating, is_exclusive, status, is_deleted
) VALUES
(
    '幽灵旅馆', 1,
    'http://localhost:8080/generated-covers/ghost-hotel-v2.svg',
    'http://localhost:8080/generated-covers/ghost-hotel-v2.svg',
    '暴雨夜，一间临时停留的旅馆里接连出现诡异异响。玩家将扮演被困的住客，在封锁的走廊与残缺的入住记录中找出真相。',
    1, 2, 6, 3.5, 48.00, '惊悚,搜证,反转', 4.56, 0, 1, 0
),
(
    '血色修道院', 1,
    'http://localhost:8080/generated-covers/crimson-abbey-v2.svg',
    'http://localhost:8080/generated-covers/crimson-abbey-v2.svg',
    '一封迟到多年的忏悔信，将众人重新带回封闭已久的修道院。烛火、祷告与禁忌往事交织，谁才是那场惨案真正的见证者。',
    1, 3, 7, 4.0, 78.00, '哥特,恐怖,剧情', 4.72, 0, 1, 0
),
(
    '消失的证人', 2,
    'http://localhost:8080/generated-covers/missing-witness-v2.svg',
    'http://localhost:8080/generated-covers/missing-witness-v2.svg',
    '原本板上钉钉的案件，在关键证人失踪后变得扑朔迷离。玩家要在矛盾证词与被篡改的线索里拼出完整案情。',
    2, 3, 6, 4.0, 68.00, '推理,证词,法庭', 4.68, 0, 1, 0
),
(
    '密室疑云', 2,
    'http://localhost:8080/generated-covers/locked-room-v2.svg',
    'http://localhost:8080/generated-covers/locked-room-v2.svg',
    '一间从内部反锁的房间、一封来历不明的邀请函、一个不可能完成的作案时间。适合想体验经典密室推理的玩家。',
    2, 2, 5, 3.5, 48.00, '密室,解谜,新手可玩', 4.43, 0, 1, 0
),
(
    '真相只有一个', 2,
    'http://localhost:8080/generated-covers/single-truth-v2.svg',
    'http://localhost:8080/generated-covers/single-truth-v2.svg',
    '每个人都掌握着部分真相，却也都在隐藏自己的动机。玩家需要通过多轮盘问和交叉验证，找出唯一成立的结论。',
    2, 4, 7, 4.5, 88.00, '硬核,侦探,还原', 4.81, 0, 1, 0
),
(
    '告白', 3,
    'http://localhost:8080/generated-covers/confession-v2.svg',
    'http://localhost:8080/generated-covers/confession-v2.svg',
    '多年后重返旧地，那些没有说出口的话终于有了被听见的机会。适合喜欢沉浸式情感体验的玩家。',
    3, 1, 5, 3.0, 18.00, '情感,治愈,校园', 4.36, 0, 1, 0
),
(
    '似水流年', 3,
    'http://localhost:8080/generated-covers/fleeting-years-v2.svg',
    'http://localhost:8080/generated-covers/fleeting-years-v2.svg',
    '一本泛黄相册打开了被尘封的青春岁月。每个角色都要重新面对曾经的遗憾与选择，在回忆里完成自我和解。',
    3, 2, 6, 3.5, 38.00, '青春,回忆,沉浸', 4.59, 0, 1, 0
),
(
    '魔法学院', 4,
    'http://localhost:8080/generated-covers/magic-academy-v2.svg',
    'http://localhost:8080/generated-covers/magic-academy-v2.svg',
    '新学期的魔法学院总会闹出一点乱子。玩家将化身问题学生，在一连串轻松欢乐的事件里完成学院挑战。',
    4, 1, 6, 3.0, 8.00, '欢乐,轻松,新手友好', 4.52, 0, 1, 0
),
(
    '派对之夜', 4,
    'http://localhost:8080/generated-covers/party-night-v2.svg',
    'http://localhost:8080/generated-covers/party-night-v2.svg',
    '生日派对变成了大型社交修罗场，秘密、误会和笑料轮番上演。节奏轻快，适合朋友局和破冰局。',
    4, 1, 8, 3.0, 1.00, '聚会,破冰,欢乐', 4.28, 0, 1, 0
),
(
    '黑暗骑士', 5,
    'http://localhost:8080/generated-covers/dark-knight-v2.svg',
    'http://localhost:8080/generated-covers/dark-knight-v2.svg',
    '王城危机四伏，骑士团内部也并非铁板一块。玩家要在阵营博弈与资源调度间完成自己的胜利条件。',
    5, 4, 7, 4.5, 98.00, '机制,阵营,对抗', 4.75, 0, 1, 0
),
(
    '梦回唐朝', 6,
    'http://localhost:8080/generated-covers/dream-of-tang-v2.svg',
    'http://localhost:8080/generated-covers/dream-of-tang-v2.svg',
    '繁华长安背后暗流涌动，一桩旧案牵出朝堂与坊间的多重纠葛。玩家将在盛唐画卷中完成身份还原与真相追索。',
    6, 3, 7, 4.0, 78.00, '古风,还原,沉浸', 4.66, 1, 1, 0
);

COMMIT;
