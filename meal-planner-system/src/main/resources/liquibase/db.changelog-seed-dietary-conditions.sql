--liquibase formatted sql

--changeset dev:seed-dietary-conditions
INSERT INTO dietary_conditions (id, name, description, type) VALUES
  ('gastritis', 'Gastritis', 'Inflammation of the stomach lining. Avoid spicy, acidic, fried foods, alcohol, and caffeine.', 'CONTRAINDICATION'),
  ('diabetes', 'Diabetes', 'Metabolic disorder affecting blood sugar levels. Limit refined carbohydrates, sugars, and high-glycemic foods.', 'CONTRAINDICATION'),
  ('hypertension', 'Hypertension (High Blood Pressure)', 'Elevated blood pressure. Reduce sodium intake, processed meats, and salty foods.', 'CONTRAINDICATION'),
  ('high_cholesterol', 'High Cholesterol', 'Elevated blood cholesterol levels. Limit saturated fats, trans fats, and cholesterol-rich foods.', 'CONTRAINDICATION'),
  ('celiac_disease', 'Celiac Disease', 'Autoimmune disorder triggered by gluten. Avoid all foods containing wheat, barley, and rye.', 'CONTRAINDICATION'),
  ('lactose_intolerance', 'Lactose Intolerance', 'Inability to digest lactose in dairy products. Avoid milk, cheese, yogurt, and dairy-based ingredients.', 'CONTRAINDICATION'),
  ('nut_allergy', 'Nut Allergy', 'Allergic reaction to tree nuts and peanuts. Avoid all nut products and foods processed in facilities with nuts.', 'CONTRAINDICATION'),
  ('shellfish_allergy', 'Shellfish Allergy', 'Allergic reaction to crustaceans and mollusks. Avoid shrimp, crab, lobster, clams, and other shellfish.', 'CONTRAINDICATION'),
  ('fish_allergy', 'Fish Allergy', 'Allergic reaction to finned fish. Avoid all fish species and fish-derived ingredients.', 'CONTRAINDICATION'),
  ('kidney_disease', 'Kidney Disease', 'Impaired kidney function. Limit sodium, potassium, phosphorus, and protein intake.', 'CONTRAINDICATION'),
  ('gout', 'Gout', 'Arthritis caused by uric acid crystals. Avoid organ meats, certain seafood, alcohol, and high-purine foods.', 'CONTRAINDICATION'),
  ('pancreatitis', 'Pancreatitis', 'Inflammation of the pancreas. Avoid fatty foods, alcohol, and fried items.', 'CONTRAINDICATION'),
  ('gerd', 'GERD (Gastroesophageal Reflux Disease)', 'Chronic acid reflux. Avoid acidic foods, spicy foods, chocolate, mint, and caffeine.', 'CONTRAINDICATION'),
  ('ibs', 'IBS (Irritable Bowel Syndrome)', 'Digestive disorder causing abdominal pain and altered bowel habits. Limit high-FODMAP foods, beans, and certain vegetables.', 'CONTRAINDICATION'),
  ('vegetarian', 'Vegetarian', 'Plant-based diet that excludes meat, poultry, and seafood but may include dairy and eggs.', 'DIET'),
  ('vegan', 'Vegan', 'Strict plant-based diet that excludes all animal products including dairy, eggs, and honey.', 'DIET'),
  ('keto', 'Ketogenic (Keto)', 'Very low-carb, high-fat diet that puts the body into ketosis for fat burning.', 'DIET'),
  ('paleo', 'Paleo', 'Diet based on foods presumed to be available to Paleolithic humans. Excludes grains, legumes, dairy, and processed foods.', 'DIET'),
  ('mediterranean', 'Mediterranean', 'Diet inspired by eating patterns of Mediterranean countries. Emphasizes fruits, vegetables, whole grains, olive oil, and fish.', 'DIET'),
  ('low_calorie', 'Low Calorie', 'Diet focused on reducing caloric intake for weight management. Emphasizes nutrient-dense, low-calorie foods.', 'DIET'),
  ('gluten_free', 'Gluten-Free', 'Diet that excludes gluten protein found in wheat, barley, and rye. Essential for celiac disease management.', 'DIET')
ON CONFLICT (id) DO NOTHING;
--rollback DELETE FROM dietary_conditions;
