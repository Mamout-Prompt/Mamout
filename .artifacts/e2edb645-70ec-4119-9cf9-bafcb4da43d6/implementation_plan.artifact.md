# Fix Unresolved Reference 'SearchScreen'

The `SearchScreen` and `SearchContent` composables are missing from `SearchScreen.kt`, causing `Unresolved reference` errors in `SearchIntegrationTest.kt` and `SearchScreenTest.kt`.

## User Review Required

> [!IMPORTANT]
> I will be implementing the missing `SearchScreen` and `SearchContent` composables in `SearchScreen.kt`. The implementation will follow the state managed by `SearchViewModel` and will include a search bar as expected by the integration tests.

## Proposed Changes

### UI Layer

#### [MODIFY] [SearchScreen.kt](file:///D:/File/Documents/Mamout/app/src/main/java/it/xyra/mamout/ui/search/SearchScreen.kt)
- Add `SearchScreen` composable that integrates with `SearchViewModel`.
- Add `SearchContent` stateless composable for easier testing and previewing.
- Implement a search bar within `SearchContent`.
- Logic to switch between `InitialSearchState`, `EmptySearchState`, and `SearchResultsList`.

## Verification Plan

### Automated Tests
- Run the failing integration test:
  `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=it.xyra.mamout.ui.search.SearchIntegrationTest`
- Run the unit/UI test for the screen:
  `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=it.xyra.mamout.ui.search.SearchScreenTest`

### Manual Verification
- Render a Compose Preview of `SearchContent` to verify the UI layout.
