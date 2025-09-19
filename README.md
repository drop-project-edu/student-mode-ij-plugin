# Student Mode IntelliJ Plugin

![Build](https://github.com/drop-project-edu/student-mode-ij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

A plugin that disables IntelliJ IDEA's assistance features to create a learning environment where students must solve problems without hints.

<!-- Plugin description -->
Student Mode disables quick fix light bulbs and intention previews, forcing students to understand and solve coding problems manually rather than relying on automated suggestions. 
It also checks for active third-party AI plugins and prevents JetBrains AI Assistant from functioning in the project, ensuring a comprehensive AI-free learning environment.

Perfect for educational environments where instructors want to ensure students learn fundamental programming concepts without IDE assistance.
<!-- Plugin description end -->

## What does Student Mode disable?

When Student Mode is **enabled**, the following IntelliJ settings are automatically **unchecked**:

### Settings > Editor > General > Appearance
- ❌ **"Show intention bulb"** - Hides the red/yellow light bulb icons that appear next to code issues
- ❌ **"Show preview for intention actions when available"** - Disables quick previews of potential fixes

### Visual Effect
Students will no longer see:
- 🚫 Red light bulbs next to errors (e.g., "Cannot resolve symbol")
- 🚫 Yellow light bulbs next to warnings (e.g., "Can be simplified") 
- 🚫 Quick fix previews when hovering over issues
- 🚫 AI-powered "Fix with AI Assistant" suggestions

### What remains available
Students can still use:
- ✅ Syntax highlighting and error detection
- ✅ Basic code completion (non-AI)
- ✅ Inlay hints (parameter names, types)
- ✅ Debugging tools
- ✅ Refactoring tools (via menus)

## How to use

1. **Install the plugin** and restart IntelliJ IDEA
2. **Look for the Student Mode icon** in the main toolbar (right side)
3. **Click the icon** to toggle Student Mode on/off

## .noai File Feature

When Student Mode is enabled:
- 📁 A `.noai` file is automatically created in your project root directory with the content: `"Created and managed by Student Mode plugin. Don't remove."`
- 👁️ The plugin monitors this file every 5 seconds
- ⚠️ If you manually delete or modify the `.noai` file, Student Mode is automatically disabled with a warning
- 🧹 The file is automatically removed when you disable Student Mode normally
- 🔄 Any leftover `.noai` files from previous sessions are cleaned up when IntelliJ starts

**Important**: Do not manually delete or modify the `.noai` file while working in Student Mode. If you need to disable Student Mode, use the toggle button instead.

## Behavior

- **Always starts in OFF mode** - Never surprises users
- **Preserves original settings** - Remembers your preferences before first toggle
- **Perfect restoration** - When turned off, restores exactly your original settings
- **Checks for AI plugins** - Refuses to turn on if AI assistant plugins like GitHub Copilot or Gemini are active
- **File monitoring** - Continuously monitors .noai files and automatically disables Student Mode if they're manually removed
- **Automatic cleanup** - Removes .noai files when the plugin is disabled or IntelliJ is closed

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation




