import json, re
from urllib.parse import unquote

def to_slug(name):
    return re.sub(r'[^a-z0-9]+', '-', name.lower()).strip('-')

def esc(s):
    return s.replace("'", "''")

def clean(s):
    s = unquote(s)
    s = re.sub(r'[®™©]', '', s)
    s = re.sub(r'%[a-fA-F0-9]{0,2}', '', s)
    s = re.sub(r'\s+', ' ', s).strip()
    return s

with open("resources/ingredient_normalization_map_extended.json", encoding="utf-8") as f:
    norm_map = json.load(f)

clean_map = {}
for raw, normalized in norm_map.items():
    clean_raw = clean(raw)
    clean_normalized = clean(normalized)
    if clean_raw and clean_normalized:
        clean_map[clean_raw] = clean_normalized

unique_normalized = sorted(set(clean_map.values()))


slug_count = {}
name_to_slug = {}
for name in unique_normalized:
    base_slug = to_slug(name)
    if base_slug in slug_count:
        slug_count[base_slug] += 1
        name_to_slug[name] = f"{base_slug}-{slug_count[base_slug]}"
    else:
        slug_count[base_slug] = 0
        name_to_slug[name] = base_slug

ingredient_rows = []
for name in unique_normalized:
    slug = name_to_slug[name]
    ingredient_rows.append(f"  ('{esc(name)}', '{esc(slug)}')")

alias_rows = []
for raw, normalized in sorted(clean_map.items()):
    alias_rows.append(
        f"  ('{esc(raw)}', (SELECT id FROM ingredients WHERE normalized_name = '{esc(normalized)}'))"
    )

sql = """--liquibase formatted sql

--changeset dev:seed-ingredients splitStatements:false
INSERT INTO ingredients (normalized_name, slug) VALUES
{ingredient_values}
ON CONFLICT (normalized_name) DO NOTHING;

INSERT INTO ingredient_aliases (raw_name, ingredient_id) VALUES
{alias_values}
ON CONFLICT (raw_name) DO NOTHING;
--rollback DELETE FROM ingredient_aliases; DELETE FROM ingredients;
""".format(
    ingredient_values=",\n".join(ingredient_rows),
    alias_values=",\n".join(alias_rows)
)

with open("V002__seed_ingredients.sql", "w", encoding="utf-8") as f:
    f.write(sql)

print(f"Ingredients: {len(unique_normalized)}")
print(f"Aliases: {len(clean_map)}")


dupes = {k: v for k, v in slug_count.items() if v > 0}
print(f"Дублікати slug-ів: {len(dupes)}")
for slug, count in sorted(dupes.items()):
    print(f"  {slug} ({count+1} варіанти)")