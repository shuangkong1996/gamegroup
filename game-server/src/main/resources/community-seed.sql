-- Community MVP seed data (重庆·两江新区)
-- Execute manually when initializing local/dev data.
-- 幂等插入：重复执行不会重复写入同名小区。

INSERT INTO t_community (
    name,
    city,
    district,
    cover_image,
    listing_avg_price,
    price_change_pct_1y,
    showing_count_recent,
    onsale_count,
    built_year,
    developer,
    property_company,
    building_type,
    plot_ratio,
    greening_rate,
    parking_ratio,
    property_fee,
    subway_distance_meters,
    walk_to_rail_minutes,
    primary_school,
    middle_school,
    status
)
SELECT
    v.name,
    '重庆',
    v.district,
    v.cover_image,
    v.listing_avg_price,
    v.price_change_pct_1y,
    v.showing_count_recent,
    v.onsale_count,
    v.built_year,
    v.developer,
    v.property_company,
    v.building_type,
    v.plot_ratio,
    v.greening_rate,
    v.parking_ratio,
    v.property_fee,
    v.subway_distance_meters,
    v.walk_to_rail_minutes,
    v.primary_school,
    v.middle_school,
    1
FROM (
    SELECT '绿城晓风印月' AS name, '两江新区' AS district, 'https://example.com/community/chongqing-1.jpg' AS cover_image, 11200.00 AS listing_avg_price, -1.50 AS price_change_pct_1y,
           92 AS showing_count_recent, 48 AS onsale_count, 2021 AS built_year, '绿城中国' AS developer, '绿城物业' AS property_company,
           '洋房' AS building_type, 2.30 AS plot_ratio, 35.00 AS greening_rate, 1.10 AS parking_ratio, 3.80 AS property_fee,
           950 AS subway_distance_meters, 12 AS walk_to_rail_minutes, '金山小学' AS primary_school, '礼嘉中学' AS middle_school
    UNION ALL
    SELECT '渝兴嘉悦山水', '两江新区', 'https://example.com/community/chongqing-2.jpg', 20500.00, 0.80,
           64, 41, 2018, '重庆渝兴' , '渝兴物业',
           '高层', 2.60, 32.00, 0.95, 2.60,
           1300, 16, '礼嘉实验小学', '两江巴蜀中学'
    UNION ALL
    SELECT '星悦礼玉金湾', '两江新区', 'https://example.com/community/chongqing-3.jpg', 22800.00, 1.50,
           73, 38, 2020, '星悦置业', '星悦物业',
           '小高层', 2.20, 36.50, 1.05, 3.20,
           780, 10, '礼嘉小学', '礼嘉中学'
    UNION ALL
    SELECT '中诚两江印', '两江新区', 'https://example.com/community/chongqing-4.jpg', 19800.00, 0.30,
           58, 47, 2019, '中诚地产', '中诚物业',
           '高层', 2.80, 30.00, 0.88, 2.40,
           1500, 20, '两江新区金渝学校小学部', '两江新区金渝学校中学部'
    UNION ALL
    SELECT '龙湖九里晴川', '两江新区', 'https://example.com/community/chongqing-5.jpg', 15300.00, -2.20,
           105, 66, 2018, '龙湖集团', '龙湖物业',
           '洋房', 1.90, 34.00, 1.05, 3.10,
           700, 9, '礼嘉小学', '礼嘉中学'
    UNION ALL
    SELECT '中南玖宸知溪', '北碚区(曹家湾)', 'https://example.com/community/chongqing-6.jpg', 12100.00, -1.80,
           55, 39, 2020, '中南置地', '中南物业',
           '洋房', 1.80, 33.00, 0.92, 2.40,
           980, 13, '蔡家岗小学', '兼善中学蔡家校区'
    UNION ALL
    SELECT '中南玖宸晓园', '北碚区(曹家湾)', 'https://example.com/community/chongqing-7.jpg', 11800.00, -1.90,
           51, 35, 2020, '中南置地', '中南物业',
           '洋房', 1.85, 33.50, 0.90, 2.40,
           1050, 14, '蔡家岗小学', '兼善中学蔡家校区'
    UNION ALL
    SELECT '金辉城雅注', '两江新区', 'https://example.com/community/chongqing-8.jpg', 14500.00, -2.60,
           49, 31, 2019, '金辉集团', '金辉物业',
           '小高层', 2.10, 31.00, 0.88, 2.60,
           1200, 15, '金州小学', '礼嘉中学'
    UNION ALL
    SELECT '万科观承', '北碚区(蔡家)', 'https://example.com/community/chongqing-9.jpg', 9860.00, -2.80,
           78, 54, 2019, '万科', '万科物业',
           '洋房', 2.17, 30.00, 0.95, 2.80,
           1100, 14, '蔡家小学', '朝阳中学蔡家校区'
    UNION ALL
    SELECT '龙湖悠山郡', '两江新区(礼嘉)', 'https://example.com/community/chongqing-10.jpg', 14790.00, -2.10,
           66, 42, 2016, '龙湖集团', '龙湖物业',
           '洋房', 1.70, 35.00, 1.00, 3.00,
           900, 12, '礼嘉小学', '礼嘉中学'
    UNION ALL
    SELECT '金科 金辉 嘉琳广场', '北碚区(蔡家)', 'https://example.com/community/chongqing-11.jpg', 10400.00, -3.20,
           43, 29, 2019, '金科&金辉', '金科物业',
           '洋房', 1.95, 32.00, 0.86, 2.80,
           1400, 18, '蔡家岗小学', '兼善中学蔡家校区'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM t_community c WHERE c.name = v.name
);
