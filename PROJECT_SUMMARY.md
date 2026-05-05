# CodeAI - Android Code Editor with AI Integration

## Project Structure

```
.github/
  └── workflows/
      └── android-apk-build.yml    # GitHub Actions CI/CD workflow
CodeAI/                            # Android app source code
  ├── build.gradle.kts             # Root build configuration
  ├── settings.gradle.kts          # Project settings
  ├── gradlew                      # Gradle wrapper (Unix)
  ├── gradlew.bat                  # Gradle wrapper (Windows)
  ├── gradle/                      # Gradle wrapper files
  ├── app/                         # Main app module
  │   ├── build.gradle.kts         # App build config
  │   ├── proguard-rules.pro
  │   └── src/main/
  │       ├── AndroidManifest.xml
  │       ├── java/com/codeai/editor/
  │       │   ├── MainActivity.kt
  │       │   ├── ui/
  │       │   ├── data/
  │       │   └── utils/
  │       └── res/
  ├── .gitignore
  ├── gradle.properties
  └── README.md
```

## Key Fixes Applied

### 1. Gradle Wrapper CRLF Fix
- **Problem**: `gradlew` file had Windows CRLF line endings
- **Error**: `Could not find or load main class "-Xmx64m"`
- **Root Cause**: CRLF line endings caused shell to not properly execute the gradlew script, leading Java to misinterpret arguments
- **Solution**: 
  - Rewrote `gradlew` with proper Unix LF line endings
  - Added `sed -i 's/\r$//' gradlew` in CI workflow as safety net
  - Workflow uses `working-directory: CodeAI` to point to correct location

### 2. Workflow Path Fix
- **Problem**: `gradlew` was not found at repository root
- **Solution**: Workflow at `.github/workflows/` uses `working-directory: CodeAI` for all Gradle commands

## Features
- Code editor with syntax highlighting
- AI chat integration (Gemini & OpenRouter)
- Terminal panel
- File explorer
- Action approval dialog for AI commands
- Settings management