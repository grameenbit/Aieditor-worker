# CodeAI - Android Code Editor with AI Integration

## Project Structure (Root Level)

```
.github/
  └── workflows/
      └── android-apk-build.yml    # GitHub Actions CI/CD workflow
app/                               # Main app module
  ├── build.gradle.kts             # App build config
  ├── proguard-rules.pro
  └── src/main/
      ├── AndroidManifest.xml
      ├── java/com/codeai/editor/
      │   ├── MainActivity.kt
      │   ├── ui/
      │   ├── data/
      │   └── utils/
      └── res/
build.gradle.kts                   # Root build configuration
settings.gradle.kts                # Project settings
gradlew                            # Gradle wrapper (Unix)
gradlew.bat                        # Gradle wrapper (Windows)
gradle/                            # Gradle wrapper files
  └── wrapper/
      ├── gradle-wrapper.jar
      └── gradle-wrapper.properties
.gitignore
gradle.properties
README.md
```

## Key Fixes Applied

### 1. Project Flattening
- **Problem**: Project was inside a `CodeAI/` subdirectory, causing `No such file or directory` errors in CI when it expected files at root or incorrectly referenced the folder.
- **Solution**: Moved all project files from `CodeAI/` to the repository root.
- **Result**: Standard Android project structure that works seamlessly with CI/CD runners.

### 2. Workflow Cleanup
- **Problem**: The GitHub Actions workflow still looked for a `CodeAI` directory that no longer existed.
- **Solution**: Removed all `working-directory: CodeAI` declarations from `.github/workflows/android-apk-build.yml`.
- **Result**: CI now correctly executes from the root directory.

### 3. Gradle Wrapper CRLF Fix
- **Problem**: `gradlew` file had Windows CRLF line endings.
- **Solution**: Rewrote `gradlew` with proper Unix LF line endings and added `sed` cleanup in the workflow.

## Features
- Code editor with syntax highlighting
- AI chat integration (Gemini & OpenRouter)
- Terminal panel
- File explorer
- Action approval dialog for AI commands
- Settings management