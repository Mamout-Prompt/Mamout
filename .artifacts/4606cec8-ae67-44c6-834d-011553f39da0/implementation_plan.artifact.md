# GitHub-Style Markdown and Prompt Preview Integration

The goal is to analyze and refine the `InteractivePromptViewer` and `AddPromptScreen` (referred to as `PromptScreen`) components. Specifically, we will update the markdown styling in `PromptVisualizerEngine` to match GitHub's aesthetic and integrate the viewer into the template creation flow.

## Proposed Changes

### [Component] Prompt Viewer Styling

#### [MODIFY] [PromptVisualizerEngine.kt](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/ui/components/promptviewer/PromptVisualizerEngine.kt)
- Update `jsonKeyStyle`, `stringStyle`, and `xmlTagStyle` to use GitHub-inspired syntax colors.
- Update `codeStyle` to use a light gray background (`#F6F8FA`) and dark text (`#24292E`).
- Update `linkStyle` to GitHub's signature blue (`#0366D6`).
- Update `quoteStyle` to GitHub's muted gray (`#6A737D`).
- Remove the hardcoded blue color from headers in `getHeaderStyle` to match GitHub's behavior of using the standard text color.

### [Component] Add Prompt Screen Integration

#### [MODIFY] [AddPromptScreen.kt](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/ui/addprompt/AddPromptScreen.kt)
- Import `InteractivePromptViewer` and related Compose components (`verticalScroll`, `rememberScrollState`, etc.).
- Update `PreviewStepContent` to:
    - Show an `OutlinedTextField` when in **Raw Mode** to allow editing the template.
    - Show the `InteractivePromptViewer` when in **Preview Mode** to allow interactive testing of the template.
    - Manage local state for `inputValues` to ensure the preview is interactive.

## Verification Plan

### Automated Tests
- I will run the `InteractivePromptViewerPreview` using `render_compose_preview` to verify the new GitHub-style colors.

### Manual Verification
- Deploy the app and navigate to "Create New Template".
- Enter a prompt with markdown (headers, bold, code) and JSON/XML.
- Switch to "View Preview" and verify the colors and interactivity.
