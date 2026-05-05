# CodeAI - Android Code Editor with AI Integration

## Project Structure (Root Level)

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
  │   ├── proguard-rules.pro       # ProGuard rules
  │   └── src/main/                # Source code
  │       ├── AndroidManifest.xml
  │       ├── java/com/codeai/editor/
  │       │   ├── MainActivity.kt
  │       │   ├── ui/              # UI components & screens
  │       │   ├── data/            # Data layer (API, models, repo)
  │       │   └── utils/           # Utility classes
  │       └── res/                 # Resources
├── .gitignore
├── gradle.properties
└── README.md
```

## Key Changes

### GitHub Actions Workflow Fix
- **Problem**: `gradlew` was not found because workflow expected it at root level
- **Solution**: Moved workflow to `.github/workflows/` at root level and updated paths to reference `CodeAI/gradlew` with proper `working-directory`

## Features
- Code editor with syntax highlighting
- AI chat integration (Gemini & OpenRouter)
- Terminal panel
- File explorer
- Action approval dialog for AI commands
- Settings management