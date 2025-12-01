# Racing Bank - Project Structure Guide

This document explains the organization and structure of the Racing Bank project.

## 📁 Recommended Directory Structure

```
racing-bank/
│
├── api/                              # Backend API (FastAPI)
│   ├── routes/                       # API endpoint routes
│   │   ├── __init__.py 
│   │   ├── auth_routes.py            # Authentication & onboarding
│   │   ├── account_routes.py         # Deposits & withdrawals
│   │   ├── transfer_routes.py        # Money transfers
│   │   └── fund_routes.py            # Investment fund operations
│   │
│   ├── tests/                        # API tests (create this)
│   │   ├── __init__.py
│   │   ├── test_auth.py
│   │   ├── test_transfers.py
│   │   └── test_race_conditions.py
│   │
│   ├── config.py                     # Configuration management
│   ├── database.py                   # SQLAlchemy models
│   ├── auth.py                       # JWT & TOTP utilities
│   ├── schemas.py                    # Pydantic models
│   ├── main.py                       # FastAPI application
│   ├── requirements.txt              # Python dependencies
│   ├── Dockerfile                    # API container
│   ├── compose.yaml                  # Docker Compose config
│   ├── .env.example                  # Example environment variables
│   └── README.md                     # API-specific documentation
│
├── app/                              # Android Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/hacknroll/bank/
│   │   │   │   ├── data/             # Data layer
│   │   │   │   │   ├── api/          # Retrofit API client
│   │   │   │   │   ├── models/       # Data models
│   │   │   │   │   └── repository/   # Repository pattern
│   │   │   │   │ 
│   │   │   │   ├── ui/               # UI layer
│   │   │   │   │   ├── auth/         # Login & registration
│   │   │   │   │   ├── main/         # Main activity & fragments
│   │   │   │   │   │   └── fragments/
│   │   │   │   │   │       ├── DashboardFragment.kt
│   │   │   │   │   │       ├── TransferFragment.kt
│   │   │   │   │   │       ├── InvestmentFragment.kt
│   │   │   │   │   │       ├── StatementFragment.kt
│   │   │   │   │   │       └── SettingsFragment.kt
│   │   │   │   │   └── splash/       # Splash screen
│   │   │   │   │ 
│   │   │   │   └── utils/            # Utility classes
│   │   │   │ 
│   │   │   └── res/                  # Android resources
│   │   │       ├── layout/           # XML layouts
│   │   │       ├── values/           # Colors, strings, themes
│   │   │       ├── drawable/         # Icons & images
│   │   │       ├── anim/             # Animations
│   │   │       └── font/             # Custom fonts
│   │   │ 
│   │   ├── androidTest/              # Instrumented tests
│   │   └── test/                     # Unit tests
│   │
│   ├── build.gradle.kts              # App build configuration
│   ├── proguard-rules.pro            # ProGuard configuration
│   └── README.md                     # App-specific documentation
│
├── docs/                             # Additional documentation
│   ├── RACE_CONDITIONS.md            # Detailed vulnerability examples
│   ├── API_GUIDE.md                  # Complete API reference (create)
│   ├── ANDROID_SETUP.md              # Android dev setup guide (create)
│   ├── SECURITY.md                   # Security considerations (create)
│   ├── ARCHITECTURE.md               # System architecture (create)
│   └── images/                       # Documentation images
│       ├── architecture.png
│       ├── flow-diagram.png
│       └── screenshots/
│
├── scripts/                          # Utility scripts
│   ├── setup.sh                      # Quick setup script
│   ├── demo.sh                       # Demo data loader (create)
│   ├── test_race_conditions.py       # Race condition tests (create)
│   ├── load_test.sh                  # Load testing script (create)
│   └── cleanup.sh                    # Cleanup script (create)
│
├── .github/                          # GitHub specific files
│   ├── workflows/                    # CI/CD workflows (optional)
│   │   ├── api-tests.yml 
│   │   └── android-build.yml 
│   ├── ISSUE_TEMPLATE/               # Issue templates
│   │   ├── bug_report.md 
│   │   └── feature_request.md 
│   └── PULL_REQUEST_TEMPLATE.md      # PR template
│
├── .gitignore                        # Git ignore rules
├── LICENSE                           # License file
├── README.md                         # Main project documentation
├── CONTRIBUTING.md                   # Contribution guidelines
└── PROJECT_STRUCTURE.md              # This file
```

## 📝 File Organization Principles

### 1. API Directory (`api/`)

**Purpose**: Contains all backend API code

**Key Files**:
- `main.py` - Entry point, FastAPI app initialization
- `database.py` - Database models and connection
- `auth.py` - Authentication logic (JWT, TOTP)
- `schemas.py` - Pydantic models for request/response validation
- `config.py` - Configuration and environment variables

**Routes Directory**:
- Each route file handles a specific domain (auth, accounts, transfers, funds)
- Keep routes focused and single-responsibility
- Include detailed comments about vulnerabilities

### 2. Android App Directory (`app/`)

**Purpose**: Contains Android application code

**Architecture**: MVVM (Model-View-ViewModel)

**Key Packages**:
- `data/` - Data layer (API, Repository, Models)
- `ui/` - UI layer (Activities, Fragments, ViewModels)
- `utils/` - Utility classes and helpers

**Resources**:
- Keep layouts in `res/layout/`
- Organize themes in `res/values/themes.xml`
- Store icons in `res/drawable/`

### 3. Documentation Directory (`docs/`)

**Purpose**: Detailed documentation beyond README files

**Recommended Files**:
- `RACE_CONDITIONS.md` - Comprehensive vulnerability guide
- `API_GUIDE.md` - Complete API endpoint reference
- `ANDROID_SETUP.md` - Step-by-step Android setup
- `SECURITY.md` - Security analysis and fixes
- `ARCHITECTURE.md` - System design and architecture

### 4. Scripts Directory (`scripts/`)

**Purpose**: Automation and utility scripts

**Scripts to Create**:
- `setup.sh` - One-command setup ✅
- `demo.sh` - Load demo data quickly
- `test_race_conditions.py` - Automated vulnerability tests
- `load_test.sh` - Performance and concurrency testing
- `cleanup.sh` - Clean up Docker, temp files, etc.

## 🔧 Configuration Files

### Root Level
- `.gitignore` - Git ignore patterns for Python, Android, and common files
- `LICENSE` - Educational use license
- `README.md` - Main project documentation
- `CONTRIBUTING.md` - Contribution guidelines

### API Level
- `compose.yaml` - Docker Compose configuration
- `Dockerfile` - API container definition
- `.env.example` - Example environment variables
- `requirements.txt` - Python dependencies

### Android Level
- `build.gradle.kts` - Gradle build configuration
- `gradle.properties` - Gradle properties
- `settings.gradle.kts` - Gradle settings
- `proguard-rules.pro` - Code obfuscation rules

## 📋 Files to Create

Based on this structure, here are files you should create:

### Documentation
- [ ] `docs/RACE_CONDITIONS.md` - Detailed vulnerability examples
- [ ] `docs/API_GUIDE.md` - Complete API reference
- [ ] `docs/ANDROID_SETUP.md` - Android development guide
- [ ] `docs/SECURITY.md` - Security analysis
- [ ] `docs/ARCHITECTURE.md` - System architecture

### Scripts
- [ ] `scripts/demo.sh` - Demo data loader
- [ ] `scripts/test_race_conditions.py` - Automated tests
- [ ] `scripts/load_test.sh` - Load testing
- [ ] `scripts/cleanup.sh` - Cleanup utility

### API
- [ ] `api/.env.example` - Example environment file
- [ ] `api/tests/` - Test directory with test files

### Android
- [ ] `app/src/test/` - Unit tests
- [ ] `app/src/androidTest/` - Instrumented tests

### GitHub (Optional)
- [ ] `.github/workflows/` - CI/CD workflows
- [ ] `.github/ISSUE_TEMPLATE/` - Issue templates
- [ ] `.github/PULL_REQUEST_TEMPLATE.md` - PR template

## 🎯 Best Practices

### Code Organization
1. **Separation of Concerns**: Keep API and Android code completely separate
2. **Single Responsibility**: Each file/class should have one clear purpose
3. **Documentation**: Comment all race condition vulnerabilities clearly
4. **Consistency**: Follow language conventions (PEP 8 for Python, Kotlin conventions)

### File Naming
- **Python**: `snake_case.py`
- **Kotlin**: `PascalCase.kt`
- **Config files**: `lowercase-with-hyphens.yaml`
- **Documentation**: `UPPERCASE.md`

### Directory Organization
- Keep related files together
- Use subdirectories for grouping (routes, fragments, etc.)
- Don't nest too deeply (max 3-4 levels)

### Documentation
- README at each level (root, api, app)
- Code comments for vulnerabilities
- Separate docs for complex topics
- Keep docs up-to-date with code

## 🔄 Workflow

### Development Flow
1. **API Development** → `api/` directory
2. **Android Development** → `app/` directory
3. **Documentation** → `docs/` directory
4. **Scripts** → `scripts/` directory

### Testing Flow
1. **Unit Tests** → Within respective directories
2. **Integration Tests** → `scripts/test_race_conditions.py`
3. **Load Tests** → `scripts/load_test.sh`

## 📦 Distribution

When sharing or deploying:

```
racing-bank/
├── api/           # Can be deployed independently
├── app/           # Can be built and distributed as APK
└── docs/          # Documentation for users
```

## 🚀 Quick Commands

```bash
# Setup everything
./setup.sh

# Start API only
cd api && docker-compose up -d

# Build Android app
cd app && ./gradlew assembleDebug

# Run tests
cd scripts && python test_race_conditions.py

# Clean everything
./scripts/cleanup.sh
```

## 📚 Additional Resources

- [FastAPI Project Structure](https://fastapi.tiangolo.com/tutorial/)
- [Android App Architecture](https://developer.android.com/topic/architecture)
- [Git Repository Best Practices](https://github.com/github/gitignore)

---

This structure promotes:
- ✅ Clear separation of concerns
- ✅ Easy navigation
- ✅ Maintainable codebase
- ✅ Educational value
- ✅ Professional organization