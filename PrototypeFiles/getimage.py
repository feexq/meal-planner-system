import json
import time
import re
import random
import requests
from pathlib import Path
from azure.storage.blob import BlobServiceClient, PublicAccess, ContentSettings
from ddgs import DDGS


AZURE_CONNECTION_STRING = "YOUR_CONNECTION_STRING_HERE"

CONTAINER_NAME = "ingredients-images"
JSON_FILE = "normalized_ingredients_extended.json"
MAPPING_FILE = "ingredient_image_mapping.json"
PROCESSED_LOG = "processed_ingredients.log"

MAX_TO_PROCESS = 80


blob_service = BlobServiceClient.from_connection_string(AZURE_CONNECTION_STRING)
container_client = blob_service.get_container_client(CONTAINER_NAME)

with open(JSON_FILE, encoding="utf-8") as f:
    ingredients = json.load(f)

print(f"✅ Завантажено {len(ingredients)} інгредієнтів")

processed = set()
if Path(PROCESSED_LOG).exists():
    with open(PROCESSED_LOG, encoding="utf-8") as f:
        processed = {line.strip() for line in f if line.strip()}

mapping = {}
if Path(MAPPING_FILE).exists():
    with open(MAPPING_FILE, encoding="utf-8") as f:
        mapping = json.load(f)

ddgs = DDGS()

def sanitize(name: str) -> str:
    name = re.sub(r'[^\w\s-]', '', name).strip().lower()
    return re.sub(r'[\s_-]+', '-', name)

count = 0
for ing in ingredients:
    if ing in processed or ing in mapping:
        continue

    print(f"[{count+1}/{MAX_TO_PROCESS}] 🔍 {ing}")

    success = False
    
    queries = [
        f"{ing} product packaging",
        f"{ing} supermarket package",
        f"{ing} retail package",
        f"{ing} bottle OR can OR jar OR box OR packet",
        f"{ing} store shelf"
    ]

    for q in queries:
        if success:
            break

        try:
            results = list(ddgs.images(query=q, max_results=8))

            for res in results[:5]:
                try:
                    img_url = res['image']
                    response = requests.get(img_url, timeout=20)
                    response.raise_for_status()

                    if len(response.content) < 25000:   
                        continue

                    ct = response.headers.get('content-type', 'image/jpeg')
                    ext = '.jpg' if 'jpeg' in ct or 'jpg' in ct else '.png' if 'png' in ct else '.webp' if 'webp' in ct else '.jpg'

                    filename = sanitize(ing) + ext

                    blob_client = container_client.get_blob_client(filename)
                    blob_client.upload_blob(
                        response.content,
                        overwrite=True,
                        content_settings=ContentSettings(content_type=ct)
                    )

                    mapping[ing] = blob_client.url
                    print(f"   ✅ Збережено → {filename}")

                    with open(PROCESSED_LOG, "a", encoding="utf-8") as f:
                        f.write(ing + "\n")

                    success = True
                    break

                except Exception:
                    continue

        except Exception as e:
            if "202" in str(e) or "ratelimit" in str(e).lower():
                print("   ⏳ Rate limit...")
                time.sleep(random.uniform(15, 30))
            continue

    if not success:
        print(f"   ⚠️ Не вдалося знайти упаковку для {ing}")

    time.sleep(random.uniform(9, 15))

    count += 1
    if count >= MAX_TO_PROCESS:
        break

with open(MAPPING_FILE, "w", encoding="utf-8") as f:
    json.dump(mapping, f, ensure_ascii=False, indent=2)

print(f"\n🎉 Готово! Оброблено {count} інгредієнтів")