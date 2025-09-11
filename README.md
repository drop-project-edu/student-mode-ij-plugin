# Student Mode IntelliJ Plugin

![Build](https://github.com/drop-project-edu/student-mode-ij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

A plugin that disables IntelliJ IDEA's assistance features to create a learning environment where students must solve problems without hints.

<!-- Plugin description -->
Student Mode disables quick fix light bulbs and intention previews, forcing students to understand and solve coding problems manually rather than relying on automated suggestions.

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
4. **Verify settings**: Go to Settings > Editor > General > Appearance to see the checkboxes are unchecked when Student Mode is on

## Behavior

- **Always starts in OFF mode** - Never surprises users
- **Preserves original settings** - Remembers your preferences before first toggle
- **Perfect restoration** - When turned off, restores exactly your original settings
- **Project-specific** - Each project can have its own Student Mode state

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "student-mode-ij-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/drop-project-edu/student-mode-ij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation




