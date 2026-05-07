package com.myorderapp.data.remote.recipe

object FoodTranslator {

    private val cn2en = mapOf(
        // 肉类
        "鸡" to "chicken", "鸡肉" to "chicken", "鸡胸肉" to "chicken breast",
        "鸡腿" to "chicken leg", "鸡翅" to "chicken wings", "鸡爪" to "chicken feet",
        "鸭" to "duck", "鸭肉" to "duck",
        "猪" to "pork", "猪肉" to "pork", "五花肉" to "pork belly",
        "排骨" to "pork ribs", "猪里脊" to "pork tenderloin", "排骨" to "spare ribs",
        "牛肉" to "beef", "牛腩" to "beef brisket", "牛腱" to "beef shank",
        "羊肉" to "lamb", "羊排" to "lamb chops",
        "肉末" to "minced meat", "肉丝" to "shredded pork",
        "培根" to "bacon", "火腿" to "ham", "午餐肉" to "luncheon meat",
        "香肠" to "sausage", "腊肉" to "cured meat",

        // 水产
        "鱼" to "fish", "鱼头" to "fish head", "鱼片" to "fish fillet",
        "鲈鱼" to "sea bass", "草鱼" to "grass carp", "鲫鱼" to "crucian carp",
        "三文鱼" to "salmon", "虾" to "shrimp", "大虾" to "prawn",
        "虾仁" to "shrimp", "花甲" to "clam", "螃蟹" to "crab",
        "鱿鱼" to "squid", "贝" to "shellfish",

        // 蔬菜
        "番茄" to "tomato", "西红柿" to "tomato", "土豆" to "potato",
        "茄子" to "eggplant", "青椒" to "green pepper", "红椒" to "red pepper",
        "辣椒" to "chili pepper", "干辣椒" to "dried chili",
        "黄瓜" to "cucumber", "白菜" to "cabbage", "大白菜" to "napa cabbage",
        "生菜" to "lettuce", "菠菜" to "spinach", "芹菜" to "celery",
        "西兰花" to "broccoli", "花菜" to "cauliflower", "菜花" to "cauliflower",
        "豆芽" to "bean sprouts", "绿豆芽" to "mung bean sprouts",
        "洋葱" to "onion", "蒜" to "garlic", "姜" to "ginger", "葱" to "green onion",
        "韭菜" to "chives", "香菜" to "cilantro", "蒜苗" to "garlic sprouts",
        "胡萝卜" to "carrot", "萝卜" to "radish", "白萝卜" to "daikon radish",
        "冬瓜" to "winter melon", "南瓜" to "pumpkin", "苦瓜" to "bitter melon",
        "丝瓜" to "luffa", "莲藕" to "lotus root", "藕" to "lotus root",
        "竹笋" to "bamboo shoots", "春笋" to "spring bamboo shoots",
        "木耳" to "wood ear mushroom", "香菇" to "shiitake mushroom",
        "蘑菇" to "mushroom", "金针菇" to "enoki mushroom",
        "玉米" to "corn", "豌豆" to "peas", "毛豆" to "edamame",
        "四季豆" to "green beans", "长豆" to "long beans",

        // 豆制品
        "豆腐" to "tofu", "嫩豆腐" to "silken tofu", "老豆腐" to "firm tofu",
        "豆皮" to "tofu skin", "腐竹" to "dried bean curd",
        "豆豉" to "fermented black beans",

        // 蛋奶
        "鸡蛋" to "egg", "蛋" to "egg", "皮蛋" to "century egg",
        "牛奶" to "milk", "芝士" to "cheese", "奶酪" to "cheese",
        "黄油" to "butter", "奶油" to "cream",

        // 主食
        "米饭" to "rice", "面" to "noodles", "面条" to "noodles",
        "意面" to "pasta", "意大利面" to "pasta", "粉丝" to "vermicelli",
        "粉条" to "glass noodles", "红薯粉" to "sweet potato noodles",
        "面粉" to "flour", "糯米" to "glutinous rice", "大米" to "rice",
        "馒头" to "steamed bun", "饺子" to "dumplings",
        "春卷" to "spring rolls", "寿司" to "sushi",

        // 水果
        "芒果" to "mango", "菠萝" to "pineapple", "柠檬" to "lemon",
        "苹果" to "apple", "香蕉" to "banana", "草莓" to "strawberry",
        "橙" to "orange", "柚子" to "pomelo", "西柚" to "grapefruit",
        "椰" to "coconut", "红枣" to "red dates", "枸杞" to "goji berries",
        "花生" to "peanut", "花生米" to "peanuts", "芝麻" to "sesame",
        "核桃" to "walnut", "杏仁" to "almond",

        // 调味
        "酱油" to "soy sauce", "生抽" to "soy sauce", "老抽" to "dark soy sauce",
        "醋" to "vinegar", "糖" to "sugar", "冰糖" to "rock sugar",
        "盐" to "salt", "料酒" to "cooking wine", "蚝油" to "oyster sauce",
        "豆瓣酱" to "doubanjiang", "甜面酱" to "sweet bean paste",
        "辣椒酱" to "chili sauce", "番茄酱" to "ketchup",
        "麻油" to "sesame oil", "香油" to "sesame oil",
        "花椒" to "sichuan peppercorn", "孜然" to "cumin",
        "咖喱" to "curry", "芥末" to "wasabi", "味噌" to "miso",
        "泡菜" to "kimchi", "酸菜" to "pickled mustard greens",

        // 烹饪方式
        "炒" to "stir fry", "红烧" to "braised", "清蒸" to "steamed",
        "水煮" to "boiled", "干煸" to "dry fried", "干锅" to "dry pot",
        "宫保" to "kung pao", "鱼香" to "yu xiang",
        "糖醋" to "sweet and sour", "酸辣" to "hot and sour",
        "麻婆" to "mapo", "回锅" to "twice cooked",
        "粉蒸" to "steamed with rice flour", "卤" to "braised in soy sauce",
        "白灼" to "blanched", "避风塘" to "typhoon shelter style",
        "剁椒" to "chopped chili", "黄焖" to "braised in soy sauce",
        "蒜蓉" to "garlic", "蒜泥" to "garlic paste",
        "葱油" to "scallion oil", "麻辣" to "spicy",
        "可乐" to "cola", "啤酒" to "beer",
        "三杯" to "three cup", "金汤" to "golden soup",

        // 菜系/类型
        "中餐" to "chinese", "川菜" to "sichuan",
        "粤菜" to "cantonese", "湘菜" to "hunan",
        "日料" to "japanese", "韩餐" to "korean",
        "东南亚" to "southeast asian", "西餐" to "western",
        "甜品" to "dessert", "饮品" to "drink",
        "汤" to "soup", "粥" to "congee", "煲" to "clay pot",
        "凉菜" to "cold dish", "沙拉" to "salad",
        "早餐" to "breakfast", "小吃" to "snack",

        // 其他
        "银耳" to "snow fungus", "紫菜" to "nori",
        "海带" to "kelp", "梅干菜" to "mei gan cai",
        "芽菜" to "ya cai", "泡椒" to "pickled chili",
        "九层塔" to "thai basil", "罗勒" to "basil",
        "薄荷" to "mint", "香茅" to "lemongrass",
        "柠檬叶" to "kaffir lime leaf", "南姜" to "galangal",
        "椰浆" to "coconut milk", "咖喱块" to "curry roux",
        "出汁" to "dashi", "味噌汤" to "miso soup",
        "桂花" to "osmanthus", "糯米粉" to "glutinous rice flour",
        "红糖" to "brown sugar", "黄豆粉" to "soybean powder",
        "西米" to "sago", "双皮奶" to "double skin milk",
        "杨枝甘露" to "mango pomelo sago", "焦糖布丁" to "creme caramel",
        "抹茶" to "matcha", "蜂蜜" to "honey",
    )

    fun translate(query: String): String {
        var result = query.trim()
        // 按 key 长度降序排列，优先匹配长词
        val sortedKeys = cn2en.keys.sortedByDescending { it.length }
        for (cn in sortedKeys) {
            if (result.contains(cn)) {
                result = result.replace(cn, cn2en[cn]!! + " ")
            }
        }
        return result.trim().replace(Regex("\\s+"), " ")
    }

    fun hasEnglishMeaning(query: String): Boolean {
        val sortedKeys = cn2en.keys.sortedByDescending { it.length }
        return sortedKeys.any { query.contains(it) }
    }
}
