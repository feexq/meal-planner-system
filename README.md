# FoodMart — Meal Planner System

Full-stack meal planning platform with algorithmic diet generation, AI-powered food logging, an integrated grocery store, and Stripe-based checkout.

![Java 17](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot 4.0.3](https://img.shields.io/badge/Spring%20Boot-4.0.3-brightgreen?logo=springboot)
![React 19](https://img.shields.io/badge/React-19-blue?logo=react)
![Docker](https://img.shields.io/badge/Docker-ready-blue?logo=docker)

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Screenshots](#screenshots)

---

## Overview

FoodMart generates personalised weekly meal plans by matching a database of 12,000+ recipes against each user's caloric targets, dietary restrictions, allergies, and budget preferences. The scoring algorithm considers macro-nutrient balance, cooking complexity, and ingredient diversity, then produces a 7-day plan with per-slot calorie budgets and adaptive rebalancing when meals are skipped or extra food is logged.

A companion Python microservice handles NLP tasks: parsing free-text food entries (e.g. "big Snickers bar") into structured nutrition data via LLM calls with Redis caching, classifying ingredients against dietary conditions, and finalising meal plans into a ready-to-eat schedule.

The frontend is a full grocery e-commerce UI — users can browse a product catalog, add recipe ingredients to a cart in one click, and pay via Stripe with Nova Poshta delivery integration.

---

## Key Features

- **Algorithmic Meal Planning & Rebalancing** — Generates personalized weekly plans based on caloric targets, dietary restrictions, and budget, with adaptive daily rebalancing if meals are skipped or altered. You can easily swap main or side dishes for any slot.
- **AI-Powered Nutrition Assistant** — Uses a multi-tier LLM pipeline to parse free-text food logs into structured nutrition data and automatically tags 1,800+ ingredients against various health conditions.
- **Integrated Grocery E-Commerce** — Browse products, add complete recipes to your cart in one click, and check out securely via Stripe with Nova Poshta delivery integration.
- **Comprehensive User Dashboards** — Track weight, goal progress, and diet streaks. View your daily nutrition summaries on a GitHub-style contribution heatmap.
- **Modern Security & Profile Management** — Features JWT-based authentication alongside Google and GitHub OAuth2, complete with Azure Blob Storage for seamless profile picture uploads.

---

## Tech Stack

### ☕ Backend
* **Java 17 & Spring Boot 4** — Core application framework
* **PostgreSQL 15** — Primary relational database
* **Liquibase 5** — Database schema versioning & data seeding
* **Stripe SDK & MapStruct** — Payment processing and DTO mapping

### ⚛️ Frontend
* **React 19 & Vite 8** — High-performance SPA with fast HMR
* **Chart.js 4.5** — Interactive data visualizations (weight dynamics, macros)
* **Stripe Elements** — Secure embedded checkout UI

### 🧠 AI Microservice
* **Python 3.11 & FastAPI** — Asynchronous API for ML tasks
* **Redis 7** — In-memory caching for LLM responses
* **LLM Pipeline** — Multi-tier integration (Gemini, Groq, OpenAI) for food parsing

### 🐳 Infrastructure
* **Docker Compose** — Multi-container orchestration (6 services)
* **Azure Blob Storage** — Cloud object storage for user avatars

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
