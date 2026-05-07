"""
批量从 Spoonacular 获取菜谱图片并更新 recipes.json
用法: python tools/fetch_recipe_images.py
"""
import json
import re
import time
import urllib.request
import urllib.error
from pathlib import Path

API_KEY = "c4d077f93cbd4545ae6337c03b1c925a"
BASE = "https://api.spoonacular.com/recipes/complexSearch"

CN2EN = {
    # 肉类
    "宫保鸡丁": "kung pao chicken",
    "番茄炒蛋": "tomato scrambled eggs",
    "红烧排骨": "braised spare ribs",
    "鱼香肉丝": "yu xiang shredded pork",
    "麻婆豆腐": "mapo tofu",
    "糖醋里脊": "sweet sour pork tenderloin",
    "红烧肉": "red braised pork belly",
    "清蒸鲈鱼": "steamed sea bass",
    "酸辣土豆丝": "hot sour shredded potato",
    "回锅肉": "twice cooked pork",
    "水煮牛肉": "sichuan boiled beef",
    "蛋炒饭": "egg fried rice",
    "辣子鸡丁": "spicy diced chicken",
    "蒜蓉西兰花": "garlic broccoli",
    "干煸四季豆": "dry fried green beans",
    "可乐鸡翅": "cola chicken wings",
    "地三鲜": "stir fried potato eggplant pepper",
    "京酱肉丝": "shredded pork beijing sauce",
    "西红柿牛腩": "tomato beef brisket",
    "蚂蚁上树": "ants climbing tree vermicelli",
    "蚝油生菜": "oyster sauce lettuce",
    "酸菜鱼": "pickled mustard fish",
    "皮蛋豆腐": "century egg tofu",
    "拍黄瓜": "smashed cucumber salad",
    "蒜泥白肉": "garlic white pork",
    "红烧鸡翅": "braised chicken wings",
    "糖醋排骨": "sweet sour spare ribs",
    "醋溜白菜": "sour stir fried cabbage",
    "土豆炖牛肉": "potato beef stew",
    "蒜蓉粉丝蒸虾": "garlic vermicelli shrimp",
    "小炒肉": "stir fried pork chili",
    "锅包肉": "guo bao pork",
    "春笋炒肉": "bamboo shoot pork",
    "蒜蓉粉丝娃娃菜": "garlic vermicelli baby cabbage",
    "酸辣粉": "hot sour glass noodles",
    "红烧鱼": "braised fish",
    "皮蛋瘦肉粥": "century egg pork congee",
    "葱油拌面": "scallion oil noodles",
    "孜然羊肉": "cumin lamb",
    "大盘鸡": "big plate chicken",
    "玉米排骨汤": "corn rib soup",
    "三杯鸡": "three cup chicken",
    "上汤娃娃菜": "superior soup baby cabbage",
    "爆炒花甲": "stir fried clams",
    "咕噜肉": "sweet sour pork",
    "韭菜盒子": "chive pockets",
    "黄焖鸡": "braised chicken",
    "猪肉白菜炖粉条": "pork cabbage vermicelli stew",
    "炒合菜": "stir fried mixed vegetables",
    "卤肉饭": "braised pork rice",
    "凉拌木耳": "cold wood ear salad",
    "虎皮青椒": "tiger skin peppers",
    "粉蒸肉": "steamed rice flour pork",
    "干锅花菜": "dry pot cauliflower",
    "金汤肥牛": "golden soup beef",
    "红烧鸡爪": "braised chicken feet",
    "素炒时蔬": "stir fried seasonal vegetables",
    "香煎豆腐": "pan fried tofu",
    "梅菜扣肉": "mei cai pork belly",
    "紫菜蛋花汤": "seaweed egg drop soup",
    "避风塘炒虾": "typhoon shelter shrimp",
    "蛋花汤面": "egg drop noodle soup",
    "泡椒凤爪": "pickled chili chicken feet",
    "牛肉面": "beef noodle soup",
    "菠萝咕咾肉": "pineapple sweet sour pork",
    "剁椒鱼头": "chopped chili fish head",
    "青椒肉丝": "shredded pork green pepper",
    "尖椒鸡蛋": "hot pepper scrambled eggs",
    "肉末茄子": "minced pork eggplant",
    "啤酒鸭": "beer duck",
    "白灼虾": "blanched shrimp",
    "麻辣香锅": "spicy stir fry pot",
    # 西餐
    "意大利肉酱面": "bolognese pasta",
    "凯撒沙拉": "caesar salad",
    # 日料
    "寿司": "sushi",
    "日式咖喱饭": "japanese curry rice",
    "味噌汤": "miso soup",
    # 韩餐
    "韩式泡菜炒饭": "kimchi fried rice",
    "韩式拌饭": "bibimbap",
    # 东南亚
    "冬阴功汤": "tom yum soup",
    "越南春卷": "vietnamese spring rolls",
    # 甜品
    "芒果糯米饭": "mango sticky rice",
    "焦糖布丁": "creme caramel",
    "杨枝甘露": "mango pomelo sago",
    "双皮奶": "double skin milk",
    "桂花糯米藕": "osmanthus lotus root sticky rice",
    "红枣银耳羹": "red date snow fungus soup",
    "红糖糍粑": "brown sugar glutinous rice cake",
    "抹茶拿铁": "matcha latte",
    "柠檬蜂蜜水": "lemon honey water",
}

PROJECT_ROOT = Path(__file__).resolve().parent.parent
RECIPES_PATH = PROJECT_ROOT / "app" / "src" / "main" / "assets" / "recipes.json"


def search_spoonacular(query):
    url = f"{BASE}?apiKey={API_KEY}&query={urllib.parse.quote(query)}&number=1&addRecipeInformation=true"
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "OrderDisk/1.0"})
        with urllib.request.urlopen(req, timeout=10) as resp:
            data = json.loads(resp.read())
        if data.get("results"):
            img = data["results"][0].get("image", "")
            return img.replace("-312x231.", "-636x393.")
    except Exception as e:
        print(f"  Error: {e}")
    return None


def main():
    with open(RECIPES_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)

    recipes = data["recipes"]
    updated = 0
    skipped = 0

    for i, recipe in enumerate(recipes):
        name = recipe["name"]
        en_name = CN2EN.get(name, name)

        if recipe.get("imageUrl"):
            print(f"[{i+1}/{len(recipes)}] {name} -> SKIP (already has image)")
            skipped += 1
            continue

        print(f"[{i+1}/{len(recipes)}] {name} → {en_name} ...", end=" ", flush=True)
        img = search_spoonacular(en_name)
        if img:
            recipe["imageUrl"] = img
            print(f"[OK] {img[:60]}...")
            updated += 1
        else:
            print("[NO IMAGE]")

        # Rate limit: 150 req/day free, so be conservative
        time.sleep(0.6)

    with open(RECIPES_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\n完成: 更新 {updated} 张, 跳过 {skipped} 张, 共 {len(recipes)} 道菜")


if __name__ == "__main__":
    main()
