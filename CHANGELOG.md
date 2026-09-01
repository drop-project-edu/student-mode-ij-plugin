<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# student-mode-ij-plugin Changelog

## [Unreleased]

## [0.4.0] - 2026-09-01

### Changed

- Replaced an IDE API that is scheduled for removal, so the plugin keeps working on future IDE versions

### Fixed

- Local inline completion suggestions stayed on with Student Mode enabled on IntelliJ IDEA 2026.1 and 2026.2 and their WebStorm equivalents, because the setting moved to a new place in those versions. It is now turned off there as well
- Closing the IDE with Student Mode still on left the intention bulb and preview settings switched off. They are now restored the next time the IDE starts

### Added

- Support for JetBrains IDEs other than IntelliJ IDEA, which the plugin was previously restricted to. Tested on IntelliJ IDEA and WebStorm; other IDEs should work but have not been verified

## [0.4.0-beta.2] - 2026-08-31

### Changed

- Replaced an IDE API that is scheduled for removal, so the plugin keeps working on future IDE versions

### Fixed

- Local inline completion suggestions stayed on with Student Mode enabled on IntelliJ IDEA 2026.1 and 2026.2 and their WebStorm equivalents, because the setting moved to a new place in those versions. It is now turned off there as well
- Closing the IDE with Student Mode still on left the intention bulb and preview settings switched off. They are now restored the next time the IDE starts

### Added

- Support for JetBrains IDEs other than IntelliJ IDEA, which the plugin was previously restricted to. Tested on IntelliJ IDEA and WebStorm; other IDEs should work but have not been verified

## [0.4.0-beta.1] - 2026-08-31

### Added

- Support for JetBrains IDEs other than IntelliJ IDEA, which the plugin was previously restricted to. Tested on IntelliJ IDEA and WebStorm; other IDEs should work but have not been verified

### Fixed

- Closing the IDE with Student Mode still on left the intention bulb and preview settings switched off. They are now restored the next time the IDE starts

## [0.3.0] - 2026-07-05

### Added

- A status bar icon that shows whether Student Mode is currently on or off
- Student Mode now also turns off local AI code completion suggestions

### Changed

- Improved behavior when multiple project windows are open at the same time
- Now requires IntelliJ IDEA 2025.2 or newer

## [0.2.2] - 2025-09-20

### Changed

- Improve plugin compatibility

## [0.2.1] - 2025-09-20

### Changed

- Compiled against a more recent Intellij Platform version

## [0.2.0] - 2025-09-18

### Added

- Prevent JetBrains AI Assistance using a .noai file

### Changed

- Plugin now restricted to IntelliJ IDEA only (removed support for other JetBrains IDEs)

## [0.1.0] - 2025-09-18

### Added

- Disable red/yellow light bulb icons that appear next to code issues
- Disables quick previews of potential fixes
- Detect the presence of the GitHub Copilot plugin, Gemini Code Assist and DeepSeek AI developer

[Unreleased]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.4.0...HEAD
[0.4.0]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.4.0-beta.2...0.4.0
[0.4.0-beta.2]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.4.0-beta.1...0.4.0-beta.2
[0.4.0-beta.1]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.3.0...0.4.0-beta.1
[0.3.0]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.2.2...0.3.0
[0.2.2]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/drop-project-edu/student-mode-ij-plugin/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/drop-project-edu/student-mode-ij-plugin/commits/0.1.0
