-- liquibase formatted sql
-- changeset feex:seed-achievements

INSERT INTO achievements (key, title, description, icon_url, target_value, type) VALUES
('first_week', 'First Week of Planning', 'Maintained a streak for 7 days.', 'https://example.com/icons/first_week.png', 7, 'STREAK'),
('month_master', 'Month Master', 'Maintained a streak for 30 days.', 'https://example.com/icons/month_master.png', 30, 'STREAK'),
('half_year_hero', 'Half-Year Hero', 'Maintained a streak for 180 days.', 'https://example.com/icons/half_year.png', 180, 'STREAK'),
('dedicated_planner', 'Dedicated Planner', 'Total active days reached 100.', 'https://example.com/icons/dedicated.png', 100, 'TOTAL_ACTIVE'),
('veteran_planner', 'Veteran Planner', 'Total active days reached 365.', 'https://example.com/icons/veteran.png', 365, 'TOTAL_ACTIVE'),
('first_step', 'First Step', 'Completed your first meal plan day.', 'https://example.com/icons/first_step.png', 1, 'TOTAL_ACTIVE'),
('goal_crusher', 'Goal Crusher', 'Reached your target weight.', 'https://example.com/icons/weight_goal.png', 1, 'WEIGHT'),
('recipe_explorer', 'Recipe Explorer', 'Tried 10 different recipes.', 'https://example.com/icons/recipe_explorer.png', 10, 'RECIPES'),
('master_chef', 'Master Chef', 'Tried 50 different recipes.', 'https://example.com/icons/master_chef.png', 50, 'RECIPES');
