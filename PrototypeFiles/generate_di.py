
import json

with open("unique_tags.json") as f:
    tags = json.load(f)

rows = [f"  ('{t['name'].replace(chr(39), chr(39)*2)}')" 
        for t in tags if t['name'].strip()]

sql = """--liquibase formatted sql

--changeset dev:seed-tags
INSERT INTO tags (name) VALUES
{values}
ON CONFLICT (name) DO NOTHING;
--rollback DELETE FROM tags;
""".format(values=",\n".join(rows))

with open("V005__seed_tags.sql", "w") as f:
    f.write(sql)

print(f"Done! {len(rows)} tags")