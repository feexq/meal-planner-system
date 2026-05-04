# FoodMart — Meal Planner System

FoodMart is an advanced full-stack platform that **algorithmically generates** personalized weekly meal plans, **intelligently optimizing** a database of **12,000+ recipes** to meet caloric targets, dietary restrictions, and budget constraints. It features an **AI-powered nutrition assistant** for natural language food logging, an **integrated grocery store** with **Stripe payments**, and adaptive daily rebalancing. Built with **Spring Boot 4** and **React 19**, the system automates the entire journey from BMR calculation to **Nova Poshta** integration for warehouse selection.

![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot 4.0.3](https://img.shields.io/badge/Spring%20Boot-4.0.3-6DB33F?logo=springboot)
![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react)
![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)

## ⭐ Key Features

- **Algorithmic Meal Planning** — Generates 7-day plans based on macro-balance, cooking complexity, and budget preferences.
- **Adaptive Rebalancing** — Automatically recalculates caloric budgets if meals are skipped or extra food is logged.
- **AI Nutrition Assistant** — LLM-powered parsing of free-text food entries (e.g., "bowl of oatmeal with honey") into structured data.
- **Grocery E-Commerce** — Integrated store where users can buy recipe ingredients in one click with **Stripe** checkout.
- **Health Dashboards** — Goal tracking, weight dynamics charts, and a GitHub-style nutrition contribution heatmap.
- **Enterprise Security** — JWT-based auth with **Google/GitHub OAuth2** and Azure Blob Storage for media.

## 🚀 Quick Start

1. **Setup Environment**:
   ```bash
   git clone https://github.com/feexq/meal-planner-system.git
   cd meal-planner-system/meal-planner-system
   cp .env.example .env # Fill in your API keys
   ```
2. **Launch Services**:
   ```bash
   docker-compose up -d --build
   ```



## 🛠 Tech Stack
<details>
<summary><b>View detailed technology layers</b></summary>

### ☕ Backend & AI
- **Java 17 / Spring Boot 4** — Core API, Security (JWT/OAuth2), Data JPA
- **Python 3.11 / FastAPI** — NLP microservice for LLM food parsing
- **PostgreSQL 15** — Relational storage with **Liquibase** migrations
- **Redis 7** — Caching layer for LLM responses and sessions

### ⚛️ Frontend & Integration
- **React 19 / Vite** — Modern UI with Chart.js for health analytics
- **Stripe SDK** — Secure payment processing & embedded Elements
- **Azure Blob Storage** — Cloud hosting for user profile pictures
- **Nova Poshta API** — Integrated shipping and warehouse selection

</details>

## 📸 Screenshots
<details>
<summary><b>View application gallery</b></summary>

<p align="center">
  <img src="assets/homepage.png" width="45%" alt="Homepage" />
  <img src="assets/meal-tracker.png" width="45%" alt="Meal tracker" />
</p>
<p align="center">
  <img src="assets/profile.png" width="45%" alt="Profile" />
  <img src="assets/statistics.png" width="45%" alt="Stats" />
</p>
<p align="center">
  <img src="assets/cart.png" width="45%" alt="Cart" />
  <img src="assets/checkout.png" width="45%" alt="Checkout" />
</p>
<p align="center">
  <img src="assets/survey.png" width="45%" alt="Survey" />
</p>

</details>
