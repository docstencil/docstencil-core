# Contributing to DocStencil

Thank you for considering contributing to DocStencil! This document explains how to contribute.

## How to Contribute

### Reporting Bugs

If you find a bug, please [open an issue](https://github.com/docstencil/docstencil-core/issues/new?template=bug_report.md) with:

- A clear description of the problem, ideally with a minimal docx template attached
- Expected vs. actual behavior
- DocStencil version and JDK version

### Suggesting Features

Have an idea? [Open a feature request](https://github.com/docstencil/docstencil-core/issues/new?template=feature_request.md) describing:

- The problem you're trying to solve
- Your proposed solution
- A description of your use case

### Submitting Code

1. **Fork** the repository
2. **Create a branch** for your change (`git checkout -b feature/my-feature`)
3. **Make your changes** with tests
4. **Run the tests** (`./gradlew test`)
5. **Submit a pull request**

## Development Setup

### Prerequisites

- JDK 8 or higher
- Gradle (wrapper included)

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test                    # All tests
./gradlew :docstencil-core:test   # Core module only
```

### Code Style

- Follow existing code conventions in the project
- Write tests for new functionality
- Keep commits focused and atomic

## Questions?

If you have questions, feel free to open an issue or start a discussion.
