# Add Prompt Feature Implementation Plan

We need to create a new page for adding prompts to the Mamout app. This involves setting up navigation, creating a ViewModel to handle the multi-step prompt creation process, and building the UI using the existing `AdvancedPromptViewer` component.

## User Review Required

> [!IMPORTANT]
> The "three steps" for prompt setup will be implemented as follows:
> 1. **Basic Info**: Enter Title and Description.
> 2. **Prompt Draft**: Enter the raw text of the prompt.
> 3. **Templatization & Preview**: Use `AdvancedPromptViewer` to add/refine `<INPUT>` tags and preview the final result.
>
> I will also add a "Save" action to persist the prompt to the Room database.

## Proposed Changes

### Data & Domain Layer

#### [MODIFY] [PromptRepository](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/domain/repository/PromptRepository.kt)
- Add `suspend fun savePrompt(title: String, description: String, templateText: String)` method.

#### [MODIFY] [PromptRepositoryImpl](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/data/repository/PromptRepositoryImpl.kt)
- Implement `savePrompt` using `PromptDao`.

---

### UI Layer

#### [NEW] [AddPromptViewModel](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/ui/addprompt/AddPromptViewModel.kt)
- Manage UI state for the add prompt workflow.
- Handle state for title, description, and template text.
- Integrate with `PromptRepository` to save the prompt.

#### [NEW] [AddPromptScreen](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/ui/addprompt/AddPromptScreen.kt)
- A new screen with a step-by-step UI (using `HorizontalPager` or simple state switching).
- **Step 1 & 2**: Fields for Title, Description, and Raw Text.
- **Step 3**: `AdvancedPromptViewer` for template refinement.

#### [MODIFY] [MainActivity](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/MainActivity.kt)
- Introduce a simple navigation state to switch between `PromptListScreen` and `AddPromptScreen`.
- Provide the necessary callbacks (`onAddPromptClick`, `onBack`).

## Verification Plan

### Automated Tests
- I will create a unit test for `AddPromptViewModel` to verify the saving logic.

### Manual Verification
- Deploy the app to a device/emulator.
- Click the "+" button on the home screen.
- Verify that the "Add Prompt" page appears.
- Fill in the title, description, and prompt text.
- Use the `AdvancedPromptViewer` to add `<INPUT>` tags.
- Save the prompt and verify it appears in the list.
