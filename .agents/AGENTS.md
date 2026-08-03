# Workspace Rules & Branch Conventions for Sonique

## Branch Overview & Workflow
- **`Ui-redesign-m3`**: Active feature branch used for all major UI/UX redesigns, new components, and features.
- **`master`**: Stable production branch used strictly for minor bug fixes or urgent hotfixes.

## How to Work on Both Branches
1. **Daily Development**: Work on the currently active working branch for all requested changes.
2. **Strict Git Safety**:
   - Only create clean, separate local commits (`git commit`).
   - NEVER run `git push`.
   - NEVER perform `git merge`.
   - NEVER automatically switch branches or perform cross-branch cherry-picks/merges unless specifically instructed by the user for that prompt.

## Material 3 Design & Code Principles
- **100% Strict Material 3 Theme**: Stick strictly to Google Material 3 prebuilt components and theme tokens. No custom ad-hoc styles.
- **Maximum Code & Component Reuse**: Maximize reuse of prebuilt Material 3 UI components, existing ViewModels, and data repositories.
- **Zero Hardcoded Values**: Never hardcode colors, layout dimensions, or font sizes. Always utilize dynamic Monet color tokens (`MaterialTheme.colorScheme`), typography scales (`MaterialTheme.typography`), and shapes (`MaterialTheme.shapes`).
- **Dynamic, Clean & Blazing Fast**: Keep the UI dynamic, minimal, lightweight, clean, and ultra-fast.

## General Rules
- **Strict Anonymity**: NEVER mention or include any external project or third-party application names anywhere in code comments, docstrings, or git commit messages.
- **Git Operations Policy**: ONLY make local commits (`git commit`). NO `git push`, NO `git merge`, and NO unauthorized branch switching or merging under any circumstances.
