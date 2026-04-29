# FoodMart — Meal Planner System

Full-stack meal planning platform with algorithmic diet generation, AI-powered food logging, an integrated grocery store, and Stripe-based checkout.

![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot 4.0.3](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?logo=springboot)
![React 19](https://img.shields.io/badge/React-19-blue?logo=react)
![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)

---

## Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [API Reference](#-api-reference)
- [Project Structure](#-project-structure)
- [Screenshots](#-screenshots)

---

## Overview

FoodMart generates personalised weekly meal plans by matching a database of 12,000+ recipes against each user's caloric targets, dietary restrictions, allergies, and budget preferences. The scoring algorithm considers macro-nutrient balance, cooking complexity, and ingredient diversity, then produces a 7-day plan with per-slot calorie budgets and adaptive rebalancing when meals are skipped or extra food is logged.

A companion Python microservice handles NLP tasks: parsing free-text food entries (e.g. "big Snickers bar") into structured nutrition data via LLM calls with Redis caching, classifying ingredients against dietary conditions, and finalising meal plans into a ready-to-eat schedule.

The frontend is a full grocery e-commerce UI — users can browse a product catalog, add recipe ingredients to a cart in one click, and pay via Stripe with Nova Poshta delivery integration.

---

## Key Features

- **Algorithmic Meal Plan Generation** — A multi-stage scoring pipeline filters and ranks recipes per meal slot based on caloric fit, macro targets, dietary tags, budget, and complexity. Supports zig-zag calorie cycling.
- **Adaptive Weekly Rebalancing** — When a user marks a meal as eaten with different calories or logs extra food, the remaining daily targets adjust automatically to stay within the weekly budget.
- **AI Food Logging** — Free-text food input (e.g. "200g chicken breast with rice") is parsed by an LLM into structured nutrition data. Results are cached in Redis to avoid repeated API calls.
- **Dietary Condition Filtering** — Ingredients are tagged with dietary statuses (allowed / soft-forbidden / hard-forbidden) across conditions like diabetes, lactose intolerance, and keto. Recipes containing forbidden ingredients are excluded from plans.
- **Recipe Slot Swapping** — Users can swap the main or side dish of any meal slot. The backend re-runs the scoring algorithm against the candidate pool for that slot.
- **Grocery Store with Cart** — A product catalog with hierarchical categories, search, and pagination. The "Add recipe to cart" endpoint resolves recipe ingredients to purchasable products and adds them in one action.
- **Stripe Payments & Order Tracking** — Full checkout flow with Stripe Payment Intents, webhook processing for payment confirmation, and order status tracking (pending → paid → in transit → delivered).
- **Nova Poshta Delivery** — City and warehouse search via the Nova Poshta API for Ukrainian delivery addresses.
- **User Profile & Streaks** — Weight tracking with history charts, goal progress percentage, achievement system, and streak tracking (strict/casual modes with freeze days).
- **Nutrition Heatmap** — Daily nutrition summaries visualised as a GitHub-style contribution heatmap for the current year.
- **OAuth2 Login** — Authentication via Google and GitHub in addition to email/password with JWT + refresh tokens.
- **Avatar Uploads** — Profile images stored in Azure Blob Storage.
- **OpenAPI Documentation** — Auto-generated Swagger UI available at `/swagger-ui/index.html`.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java (Eclipse Temurin) | 17 |
| | Spring Boot | 4.0.3 |
| | Spring Data JPA / Hibernate | via Spring Boot BOM |
| | Spring Security + OAuth2 | via Spring Boot BOM |
| | Liquibase | 5.0.1 |
| | Stripe Java SDK | 32.1.0 |
| | MapStruct | 1.6.3 |
| | JJWT | 0.13.0 |
| | SpringDoc OpenAPI | 3.0.2 |
| **Frontend** | React | 19.2.4 |
| | Vite | 8.0.1 |
| | React Router | 7.13.2 |
| | Chart.js | 4.5.1 |
| | Stripe React | 6.1.0 |
| **AI Service** | Python | 3.11 |
| | FastAPI | 0.115.0 |
| | Google GenAI SDK | 1.66.0 |
| | ingredient-parser-nlp | 2.5.0 |
| **Infrastructure** | PostgreSQL | 15 |
| | Redis | 7 (Alpine) |
| | Docker Compose | v2 |
| | Stripe CLI | latest |

---

## Architecture

```
┌──────────────────────┐
│  React Frontend :3000│
└──────────┬───────────┘
           │ HTTP REST (JSON)
           ▼
┌──────────────────────┐       ┌────────────────────────┐
│ Spring Boot API :8080│──────►│ Python AI Service :8000│
│                      │       │ (normalize, classify,  │
│  • Meal Plan Engine  │       │  parse-food, finalize,  │
│  • Grocery Store     │       │  swap-slot)             │
│  • Auth (JWT/OAuth2) │       └───────────┬────────────┘
│  • Stripe Payments   │                   │
└───┬──────────┬───────┘                   │
    │          │                           │
    ▼          ▼                           ▼
┌────────┐ ┌───────┐                 ┌───────────┐
│PostgreSQL│ │ Redis │◄────────────────│Redis Cache│
│  :5432  │ │ :6379 │                 └───────────┘
└────────┘ └───────┘

┌──────────────────────┐
│  Stripe CLI Webhook  │──► forwards to :8080/api/webhooks/stripe
└──────────────────────┘
```

The root `docker-compose.yaml` uses the `include` directive to compose two sub-files: the backend stack (`meal-planner-system/docker-compose.yaml` with 5 services) and the frontend (`meal-planner-system-frontend/docker-compose.yaml`). The backend communicates with the Python normalizer via synchronous HTTP calls. Both the backend and the normalizer share the same Redis instance — the backend uses it for session/cache and the normalizer for LLM response caching. PostgreSQL is initialised from a pre-compressed SQL dump (`db-init/init.sql.gz`) on first start.

---

## Quick Start

### Prerequisites

- **Docker** and **Docker Compose** v2 installed
- API keys for the services listed below (see `.env.example`)

### Setup

```bash
git clone https://github.com/feexq/meal-planner-system.git
cd meal-planner-system/meal-planner-system
cp .env.example .env
# Fill in your actual API keys in .env
```

### Environment Variables

| Variable | Description | Required |
|----------|-------------|:--------:|
| `DB_USER` | PostgreSQL username | Yes |
| `DB_PASSWORD` | PostgreSQL password | Yes |
| `GEMINI_API_KEY` | Google Gemini API key for LLM features | Yes |
| `GROQ_API_KEY` | Groq API key (LLM fallback) | Yes |
| `JWT_SECRET` | Base64-encoded secret for JWT signing | Yes |
| `STRIPE_SECRET_KEY` | Stripe secret key (test mode) | Yes |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | Yes |
| `NOVA_API_KEY` | Nova Poshta API key for delivery | Yes |
| `AZURE_STORAGE_CONNECTION_STRING` | Azure Blob connection string for avatars | No |
| `IMAGE_CONTAINER_NAME` | Azure Blob container name | No |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | No |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | No |
| `GITHUB_CLIENT_ID` | GitHub OAuth2 client ID | No |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth2 client secret | No |

### Run

From the **repository root**:

```bash
docker-compose up -d --build
```

> The database initialises from a compressed SQL dump on first launch. All 12,000+ recipes and product data are loaded automatically — no manual seeding required.

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | `http://localhost:3000` | React SPA |
| Backend API | `http://localhost:8080` | Spring Boot REST API |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | Interactive API docs |
| AI Normalizer | `http://localhost:8000` | Python FastAPI service |
| PostgreSQL | `localhost:5432` | Database |
| Redis | `localhost:6379` | Cache |

---

## API Reference

The API uses JWT Bearer authentication. Obtain a token via `/api/auth/login` or `/api/auth/register`, then pass it as `Authorization: Bearer <token>`.

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/auth/register` | | Register a new user |
| POST | `/api/auth/login` | | Authenticate and receive JWT tokens |
| POST | `/api/auth/refresh` | | Refresh an expired access token |
| POST | `/api/auth/logout` | 🔒 | Invalidate the current token |

### User Preferences

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/user/me` | 🔒 | Get current user info |
| GET | `/api/user/preference` | 🔒 | Get dietary preferences |
| POST | `/api/user/preference` | 🔒 | Submit initial preferences (survey) |
| PUT | `/api/user/preference` | 🔒 | Update existing preferences |
| DELETE | `/api/user/preference` | 🔒 | Delete preferences |
| GET | `/api/user/preference/exists` | 🔒 | Check if user has submitted the survey |

### Profile

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/profile/me` | 🔒 | Get profile summary (weight, streaks, averages) |
| PUT | `/api/profile/me` | 🔒 | Update profile (bio, timezone, target weight) |
| POST | `/api/profile/avatar` | 🔒 | Upload avatar image |
| DELETE | `/api/profile/avatar` | 🔒 | Delete avatar |
| POST | `/api/profile/weight` | 🔒 | Log today's weight |
| POST | `/api/profile/weight/{date}` | 🔒 | Log weight for a specific date |
| DELETE | `/api/profile/weight/{date}` | 🔒 | Delete a weight entry |
| GET | `/api/profile/weight/history` | 🔒 | Get weight history (with date range and limit) |
| GET | `/api/profile/streak` | 🔒 | Get streak data |
| PUT | `/api/profile/streak/type` | 🔒 | Change streak mode (strict/casual) |
| GET | `/api/profile/achievements` | 🔒 | Get achievement list and status |
| GET | `/api/profile/statistics/nutrition-heatmap` | 🔒 | Get daily nutrition heatmap data |
| GET | `/api/profile/statistics/top-recipes` | 🔒 | Get most frequently eaten recipes |

### Meal Plan

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/meal-plan/generate` | 🔒 | Generate a weekly meal plan preview |
| POST | `/api/meal-plan/generate/{email}` | 🔒 | Generate a plan for a specific user |
| POST | `/api/meal-plan/generate/final` | 🔒 | Finalise and persist the meal plan |
| POST | `/api/meal-plan/generate/final/{email}` | 🔒 | Finalise a plan for a specific user |
| GET | `/api/meal-plan/status` | 🔒 | Get current plan status with all days and slots |
| POST | `/api/meal-plan/mark-eaten` | 🔒 | Mark a meal slot as eaten (triggers rebalancing) |
| POST | `/api/meal-plan/log-food` | 🔒 | Log free-text food intake (parsed by AI) |
| POST | `/api/meal-plan/swap-slot/{slotId}` | 🔒 | Swap a meal slot (auto-selects main/side) |
| POST | `/api/meal-plan/swap-slot/main/{slotId}` | 🔒 | Swap only the main dish |
| POST | `/api/meal-plan/swap-slot/side/{slotId}` | 🔒 | Swap only the side dish |

### Recipes

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/recipes` | | List recipes with filters (meal type, cook time, complexity, budget, tags) |
| GET | `/api/recipes/{id}` | | Get recipe details with ingredients, steps, and nutrition |
| GET | `/api/recipes/slug/{slug}` | | Get recipe by URL slug |
| GET | `/api/recipes/filters` | | Marketplace recipe listing with multi-value filters |
| GET | `/api/recipes/details` | | Detailed recipe listing with all fields |
| GET | `/api/recipes/by-ingredient/{ingredientId}` | | Find recipes containing a specific ingredient |
| GET | `/api/recipes/search/by-ingredients` | | Search recipes by multiple ingredient IDs (match percentage) |

### Products

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/products` | | List products (search, category filter, availability, pagination) |
| GET | `/api/products/{id}` | | Get product details |
| GET | `/api/products/slug/{slug}` | | Get product by slug |
| GET | `/api/products/by-ingredients` | | Get products matching ingredient IDs |
| POST | `/api/products` | 🔒 | Create a product |
| PUT | `/api/products/{id}` | 🔒 | Update a product |
| DELETE | `/api/products/{id}` | 🔒 | Delete a product |

### Categories

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/categories` | | Get root categories with children |
| GET | `/api/categories/all` | | Get flat list of all categories |
| GET | `/api/categories/{id}` | | Get category by ID |
| GET | `/api/categories/slug/{slug}` | | Get category by slug |
| POST | `/api/categories` | 🔒 | Create a category |
| PUT | `/api/categories/{id}` | 🔒 | Update a category |
| DELETE | `/api/categories/{id}` | 🔒 | Delete a category |

### Ingredients

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/ingredients` | | List ingredients (search, category, availability, pagination) |
| GET | `/api/ingredients/{id}` | | Get ingredient details |
| GET | `/api/ingredients/slug/{slug}` | | Get ingredient by slug |
| GET | `/api/ingredients/{id}/dietary-tags` | | Get dietary tags for an ingredient |
| POST | `/api/ingredients` | 🔒 | Create an ingredient |
| PUT | `/api/ingredients/{id}` | 🔒 | Update an ingredient |
| PUT | `/api/ingredients/{id}/dietary-tags` | 🔒 | Update dietary tag statuses |
| DELETE | `/api/ingredients/{id}` | 🔒 | Delete an ingredient |

### Cart

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/cart` | 🔒 | Get current cart contents |
| POST | `/api/cart/items` | 🔒 | Add an item to the cart |
| POST | `/api/cart/add-recipe/{recipeId}` | 🔒 | Add all recipe ingredients to the cart |
| PUT | `/api/cart/items/{ingredientId}` | 🔒 | Update item quantity |
| DELETE | `/api/cart/items/{ingredientId}` | 🔒 | Remove an item |
| DELETE | `/api/cart` | 🔒 | Clear the entire cart |

### Orders

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/orders` | 🔒 | List user's orders |
| GET | `/api/orders/{id}` | 🔒 | Get order details with items |
| POST | `/api/orders/checkout` | 🔒 | Create a Stripe checkout session |
| POST | `/api/orders/checkout-intent` | 🔒 | Create a Stripe payment intent |

### Delivery

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/delivery/cities` | | Search cities via Nova Poshta API |
| GET | `/api/delivery/warehouses` | | Get warehouses for a city |

### Dietary Conditions

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/dietary/conditions` | | List all dietary conditions |
| GET | `/api/dietary/conditions/type/{type}` | | Get conditions by type (CONTRAINDICATION or DIET) |
| POST | `/api/dietary-conditions/trigger-classification` | 🔒 | Trigger AI classification of ingredient dietary tags |

### Tags

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| GET | `/api/v1/tags` | | List all tags |
| GET | `/api/v1/tags/{id}` | | Get tag by ID |
| GET | `/api/v1/tags/slug/{slug}` | | Get tag by slug |
| POST | `/api/v1/tags` | 🔒 | Create a tag |
| PATCH | `/api/v1/tags/{id}` | 🔒 | Update a tag |
| DELETE | `/api/v1/tags/{id}` | 🔒 | Delete a tag |
| GET | `/api/v1/tags-recipes` | | List recipe tags |
| GET | `/api/v1/tags-recipes/{id}` | | Get recipe tag by ID |
| POST | `/api/v1/tags-recipes` | 🔒 | Create a recipe tag |
| PUT | `/api/v1/tags-recipes/{id}` | 🔒 | Update a recipe tag |
| DELETE | `/api/v1/{id}` | 🔒 | Delete a recipe tag |
| GET | `/api/v1/ingredients/{ingredientId}/tags` | | Get tags for an ingredient |
| POST | `/api/v1/ingredients/{ingredientId}/tags/{tagId}` | 🔒 | Add tag to ingredient |
| DELETE | `/api/v1/ingredients/{ingredientId}/tags/{tagId}` | 🔒 | Remove tag from ingredient |

### Webhooks

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| POST | `/api/webhooks/stripe` | | Stripe webhook handler (signature verified) |

---

## Project Structure

```
meal-planner-system/
├── meal-planner-system/           # Spring Boot backend
│   ├── src/main/java/             # Application source code
│   │   └── com/feex/mealplannersystem/
│   │       ├── web/               # REST controllers (16 controllers)
│   │       ├── service/           # Business logic layer
│   │       ├── mealplan/          # Meal plan engine (scoring, filtering, rebalancing)
│   │       ├── repository/        # Spring Data JPA repositories
│   │       ├── domain/            # Domain models
│   │       ├── dto/               # Request/response DTOs
│   │       ├── security/          # JWT filters, OAuth2 config
│   │       ├── config/            # App configuration, data loaders
│   │       └── util/              # Utility classes
│   ├── src/main/resources/
│   │   └── liquibase/             # Database migrations (schema + seed data)
│   ├── normalizer/                # Python FastAPI AI microservice
│   │   ├── routers/               # API routes (normalize, classify, food, finalize, swap)
│   │   ├── services/              # LLM integration with 4-tier model fallback
│   │   └── core/                  # Redis cache, config
│   ├── db-init/                   # Pre-compressed SQL dump (init.sql.gz)
│   ├── docker-compose.yaml        # Backend services (postgres, redis, normalizer, stripe, backend)
│   ├── Dockerfile                 # Multi-stage Java build
│   └── build.gradle               # Gradle build config
│
├── meal-planner-system-frontend/  # React + Vite frontend
│   ├── src/                       # Components, pages, API clients
│   ├── docker-compose.yaml        # Frontend service
│   └── Dockerfile                 # Node.js container
│
├── docker-compose.yaml            # Root compose (includes backend + frontend)
└── assets/                        # Screenshots for README
```

---

## Screenshots

<p align="center">
  <img src="assets/homepage.png" width="45%" alt="Homepage — product catalog with categories and promotional banners" />
  <img src="assets/meal-tracker.png" width="45%" alt="Meal tracker — daily plan with calorie targets, meal slots, and AI food logging" />
</p>
<p align="center">
  <em>Homepage with product catalog</em>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <em>Weekly meal tracker with macro breakdown</em>
</p>

<br/>

<p align="center">
  <img src="assets/profile.png" width="45%" alt="User profile — streak tracking, weight chart, nutrition heatmap, and achievements" />
  <img src="assets/statistics.png" width="45%" alt="Statistics — weight dynamics chart, goal progress, and weekly nutrition analysis" />
</p>
<p align="center">
  <em>User profile with streaks and heatmap</em>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <em>Analytics dashboard with weight tracking</em>
</p>

<br/>

<p align="center">
  <img src="assets/cart.png" width="45%" alt="Shopping cart with Nova Poshta delivery selection" />
  <img src="assets/checkout.png" width="45%" alt="Checkout page with Stripe payment integration" />
</p>
<p align="center">
  <em>Cart with delivery address selection</em>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
  <em>Stripe checkout with order summary</em>
</p>

<br/>

<p align="center">
  <img src="assets/survey.png" width="45%" alt="Dietary preferences survey — step-by-step onboarding wizard" />
</p>
<p align="center">
  <em>Multi-step dietary preference survey for BMR calculation</em>
</p>
