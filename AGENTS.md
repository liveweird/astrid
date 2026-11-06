# Repository Guidelines

## Project Structure & Module Organization

This directory is currently empty: no source code, tests, assets, or project configuration have been added. Establish the layout with the first implementation and document it in `README.md`. A suggested starting structure is `src/` for application code, `tests/` for automated tests, and `assets/` for static resources when needed. Organize modules by responsibility and keep related functionality together.

## Build, Test, and Development Commands

No build system, package manager, or development commands are configured yet. When introducing a toolchain, provide reproducible commands for dependency installation, local development, building, testing, and linting. Record the exact commands and required runtime versions in `README.md`; commit the appropriate dependency lockfile. Do not document commands as supported until they work in this repository.

## Coding Style & Naming Conventions

No language-specific style has been established. Use the selected language’s standard formatter and linter, and commit their configuration with the initial code. Use consistent indentation, descriptive names, and small modules with clear responsibilities. Follow the language’s naming conventions and avoid mixing styles within a file.

## Testing Guidelines

No testing framework or coverage threshold is configured. Add a framework appropriate to the chosen stack and document how to run it. Name tests after the behavior they verify and follow the framework’s file discovery conventions. Include regression tests for bug fixes and cover failure paths as well as successful behavior.

## Commit & Pull Request Guidelines

This directory is not yet a Git repository, so no commit-message conventions can be inferred. Once Git is initialized, use concise, imperative subjects such as `Add initial application scaffold`. Keep changes focused. Pull requests should explain the purpose, summarize changes, link relevant issues, and report checks performed. Include screenshots for visible interface changes and call out configuration changes.

## Security & Configuration

Keep credentials and local configuration out of version control. Provide placeholder values in an example configuration file and document required environment variables when configuration is introduced.
