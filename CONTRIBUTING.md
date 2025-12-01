# Contributing to Racing Bank

Thank you for your interest in contributing to Racing Bank! This is an educational project designed to demonstrate race condition vulnerabilities in financial systems.

## 🎯 Project Purpose

**Important**: This project intentionally contains security vulnerabilities to demonstrate race conditions. When contributing, please keep these vulnerabilities intact - they are the core educational value of the project.

## 🤝 How to Contribute

### Types of Contributions We Welcome

1. **Additional Race Condition Examples**
   - New vulnerability scenarios
   - Different attack vectors
   - More sophisticated race condition demonstrations

2. **Documentation Improvements**
   - Clearer explanations of vulnerabilities
   - Better examples and diagrams
   - Translations

3. **Test Scenarios**
   - Scripts to reproduce race conditions
   - Load testing scenarios
   - Automated vulnerability demonstrations

4. **UI/UX Enhancements**
   - Android app improvements (maintaining retro theme)
   - Better visual feedback
   - Improved user experience

5. **Educational Content**
   - Tutorial videos
   - Blog posts
   - Presentation materials

### What NOT to Contribute

- ❌ Fixes for race conditions (this defeats the purpose)
- ❌ Production-ready security implementations
- ❌ Changes that remove vulnerabilities
- ❌ Performance optimizations that hide race conditions

## 📋 Contribution Process

### 1. Fork & Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR_USERNAME/racing-bank.git
cd racing-bank
```

### 2. Create a Branch

```bash
git checkout -b feature/your-feature-name
# or
git checkout -b docs/your-documentation-improvement
# or
git checkout -b example/new-race-condition
```

### 3. Make Your Changes

- Follow existing code style
- Add comments explaining vulnerabilities
- Update documentation if needed
- Test your changes

### 4. Commit Your Changes

```bash
git add .
git commit -m "Add: Brief description of changes"
```

**Commit Message Format:**
- `Add: New feature or example`
- `Fix: Bug fix (not security fixes!)`
- `Docs: Documentation changes`
- `Test: New test scenarios`
- `Style: Code style improvements`

### 5. Push and Create Pull Request

```bash
git push origin feature/your-feature-name
```

Then create a Pull Request on GitHub with:
- Clear title describing the change
- Description of what was added/changed
- Why this contribution is valuable for education
- Screenshots (if UI changes)

## 📝 Code Style Guidelines

### Python (API)

```python
# Follow PEP 8
# Use type hints
def transfer_money(from_user: str, to_user: str, amount: float) -> dict:
    """
    VULNERABLE: This function has no locking mechanism!
    
    Demonstrates: Double spending via race conditions
    Attack vector: Concurrent requests can overdraft account
    """
    # Implementation...
    pass

# Add docstrings explaining vulnerabilities
# Use clear variable names
# Comment race condition points
```

### Kotlin (Android)

```kotlin
// Follow Kotlin conventions
// Use meaningful names
suspend fun transferMoney(
    toUsername: String,
    amount: Double
): Result<Transaction> {
    // Implementation...
}

// Use coroutines properly
// Add comments for clarity
```

### Documentation

- Use clear, concise language
- Include code examples
- Add diagrams where helpful
- Explain both the vulnerability AND how to fix it
- Use proper markdown formatting

## 🧪 Testing Guidelines

### Before Submitting

1. **Test the API**
   ```bash
   cd api
   docker-compose up -d
   # Run your tests
   ```

2. **Test the Android App**
   ```bash
   cd app
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

3. **Verify Race Conditions Still Work**
   ```bash
   cd scripts
   python test_race_conditions.py
   ```

### Adding New Tests

```python
# In scripts/test_race_conditions.py
def test_new_vulnerability():
    """
    Test Name: Brief description
    
    Demonstrates: Type of race condition
    Expected Result: Description of vulnerability
    """
    # Test implementation
    pass
```

## 📖 Documentation Standards

### Adding New Race Condition Examples

Create a new section in `docs/RACE_CONDITIONS.md`:

```markdown
### Example N: Descriptive Title

**Vulnerability Type**: Lost Update / Double Spending / etc.

**Description**: 
Clear explanation of the vulnerability.

**Attack Scenario**:
Step-by-step description of how to exploit.

**Code Example**:
\`\`\`python
# Vulnerable code
\`\`\`

**Exploitation**:
\`\`\`bash
# Commands to reproduce
\`\`\`

**Expected Result**:
What happens when exploited.

**Fix (Educational)**:
\`\`\`python
# How to fix in production
\`\`\`
```

## 🐛 Reporting Issues

### Bug Reports (Non-Security)

If you find a bug that's NOT related to race conditions:

```markdown
**Bug Description**:
Clear description of the issue

**Steps to Reproduce**:
1. Step one
2. Step two
3. ...

**Expected Behavior**:
What should happen

**Actual Behavior**:
What actually happens

**Environment**:
- OS: 
- API Version:
- Android Version:
- Device:
```

### Enhancement Requests

```markdown
**Enhancement Description**:
Clear description of proposed enhancement

**Educational Value**:
How this helps demonstrate race conditions

**Implementation Ideas**:
Your thoughts on implementation

**Examples**:
Similar implementations or references
```

## 🔍 Code Review Process

Maintainers will review your PR for:

1. **Educational Value**: Does it help demonstrate race conditions?
2. **Code Quality**: Is it well-written and documented?
3. **Vulnerability Preservation**: Are race conditions still present?
4. **Documentation**: Is it properly documented?
5. **Testing**: Does it include appropriate tests?

## 📞 Communication

- **Questions**: Open an issue with `[Question]` prefix
- **Discussions**: Use GitHub Discussions
- **Security Issues**: Even though this project is intentionally vulnerable, if you find a non-educational security issue, please open an issue

## ⚖️ License

By contributing, you agree that your contributions will be licensed under the same license as the project (Educational Use Only).

## 🙏 Recognition

Contributors will be recognized in:
- README.md
- Release notes
- Project documentation

## 📚 Resources for Contributors

### Understanding Race Conditions
- [OWASP Race Conditions](https://owasp.org/www-community/vulnerabilities/Race_condition)
- [Database Isolation Levels](https://www.postgresql.org/docs/current/transaction-iso.html)

### Project-Specific
- [API Documentation](api/README.md)
- [Android Documentation](app/README.md)
- [Race Conditions Guide](docs/RACE_CONDITIONS.md)

---

Thank you for contributing to Racing Bank! Your contributions help others learn about concurrent programming vulnerabilities. 🎓🔒