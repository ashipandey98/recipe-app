# 🍽️ Fridge to Fork — AI Recipe Generator

## What This Project Does
- Type ingredients → Get 3 AI-generated recipes
- Smart ingredient suggestions as you type
- Vegan mode filter
- Full nutrition info per recipe
- Generate 2-week meal plan
- Email meal plan to your Gmail

## Tech Stack
- Frontend: HTML/CSS/JS (just open index.html)
- Backend:  Java 21 + Spring Boot 3.2
- AI:       Google Gemini API (FREE)
- Database: H2 in-memory (zero setup)
- Email:    Gmail SMTP

## How to Run

### Step 1: Get Free Gemini API Key
Go to: https://aistudio.google.com → Get API Key

### Step 2: Get Gmail App Password
Go to: myaccount.google.com → Security → App Passwords

### Step 3: Edit application.yml
File: backend/src/main/resources/application.yml
Change:
  app.gemini.api-key       → your Gemini API key
  spring.mail.username     → your Gmail address
  spring.mail.password     → your Gmail App Password
  app.menu.recipient-email → your Gmail address

### Step 4: Open in IntelliJ
Open the backend/ folder in IntelliJ IDEA Community

### Step 5: Enable Lombok
Settings → Build → Compiler → Annotation Processors → Enable

### Step 6: Run
Right-click RecipeApplication.java → Run

### Step 7: Open Frontend
Double-click frontend/index.html in your browser

## API Endpoints
- GET  /api/v1/health            → Health check
- POST /api/v1/recipes/generate  → Generate recipes
- POST /api/v1/recipes/save      → Save a recipe
- GET  /api/v1/recipes/saved     → Get saved recipes
- POST /api/v1/menu/generate     → Generate 2-week plan
- POST /api/v1/menu/send-email   → Email the plan

## Cost
- Gemini API: FREE (1,500 calls/day)
- Gmail SMTP: FREE
- H2 Database: FREE
- Total: ₹0/month
