# CookMate

CookMate is a full-stack social recipe platform with an Android app frontend and a PHP + MySQL backend. It supports recipe sharing, social engagement, reminders, notifications, and profile management in one product-style implementation.

## Recruiter-Friendly Highlights

- Built a full-stack Android recipe platform using an Android client, PHP APIs, and MySQL persistence.
- Implemented end-to-end user flows: signup/login, profile management, recipe publishing, and personalized content retrieval.
- Developed social features including follow/unfollow, followers/following lists, user search, and like/unlike interactions.
- Added recipe discovery and utility workflows such as recipe search, detail fetch, download/remove download, and saved collections.
- Integrated push-notification infrastructure with Firebase Cloud Messaging (FCM) token updates and notification retrieval.
- Implemented reminder management (create, list, delete) to support practical meal-planning behavior.
- Structured backend functionality into dedicated API endpoints and migration-based schema updates for maintainability and feature scaling.

## What the Project Demonstrates

- Full-stack mobile product development
- API design and backend modularization
- Relational data modeling and SQL migration handling
- Social graph and engagement mechanics
- Notification-driven user engagement patterns

## Major Product Functions

### 1) Authentication and User Profile
- Account registration and login
- User profile retrieval and update
- Privacy/settings updates
- FCM token registration for push delivery

Representative backend files:
- `backend/signup.php`
- `backend/login.php`
- `backend/get_user_profile.php`
- `backend/update_profile.php`
- `backend/update_privacy_settings.php`
- `backend/update_fcm_token.php`

### 2) Recipe Lifecycle Management
- Recipe upload and deletion
- Feed/list retrieval and detailed recipe view
- Search-based recipe discovery
- Like/unlike + recipe status checks
- Download/remove downloaded recipe handling

Representative backend files:
- `backend/uploadRecipe.php`
- `backend/get_all_recipes.php`
- `backend/get_recipe_detail.php`
- `backend/search_recipes.php`
- `backend/like_recipe.php`
- `backend/unlike_recipe.php`
- `backend/check_recipe_status.php`
- `backend/download_recipe.php`
- `backend/remove_download.php`
- `backend/get_downloaded_recipes.php`
- `backend/delete_recipe.php`

### 3) Social and Community Features
- Follow/unfollow users
- View followers/following relationships
- Search users and check follow state

Representative backend files:
- `backend/follow_user.php`
- `backend/unfollow_user.php`
- `backend/get_followers.php`
- `backend/get_following.php`
- `backend/check_follow_status.php`
- `backend/search_users.php`

### 4) Notifications and Reminders
- Push notification dispatch pipeline
- Notification retrieval API
- Reminder creation, listing, and deletion

Representative backend files:
- `backend/send_notification.php`
- `backend/get_notifications.php`
- `backend/test_fcm.php`
- `backend/save_reminder.php`
- `backend/get_reminders.php`
- `backend/delete_reminder.php`

## Technology Stack

### Mobile
- Android app module (`app/`)
- Gradle Kotlin DSL build setup (`build.gradle.kts`)
- Firebase integration (`app/google-services.json`)

### Backend
- PHP endpoint-based API layer (`backend/`)
- MySQL relational storage
- SQL schema + migrations for iterative feature delivery

### Data and Migration Assets
- `schema.sql`
- `backend/schema.sql`
- `backend/migration_multiple_images.sql`
- `backend/migration_settings.sql`
- `backend/fcm_schema.sql`
- `backend/reminders_schema.sql`

## Architecture Overview

1. Android client sends HTTP requests to PHP endpoints.
2. PHP backend validates requests and executes business logic.
3. MySQL stores users, recipes, likes, follows, notifications, reminders, and settings.
4. FCM infrastructure enables push-based engagement workflows.

## Resume Bullet Options

- Built CookMate, a full-stack social recipe application using Android, PHP, and MySQL, delivering end-to-end user and content workflows.
- Engineered a modular backend with dedicated APIs for authentication, recipes, social graph, reminders, notifications, and user settings.
- Implemented social engagement mechanics (follow/unfollow, likes, user search, relationship checks) to support community growth and retention.
- Integrated Firebase Cloud Messaging token handling and notification APIs to enable real-time user communication.
- Designed and evolved SQL schema/migrations to support feature expansion, including settings, reminder, and notification capabilities.

## Repository Snapshot

- `app/` - Android client application
- `backend/` - PHP API endpoints + SQL migration files
- Root Gradle files - Android build configuration
- Root SQL/PHP files - additional schema/legacy endpoint artifacts

## Notes for Recruiters and Interviewers

CookMate reflects practical experience building a product-oriented mobile platform that combines client engineering, backend API development, relational database design, and engagement-centric feature implementation.
