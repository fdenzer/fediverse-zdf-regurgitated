# fediverse-zdf-regurgitated

A lightweight, client-only Fediverse app for Android, focused on `zdf.social`. It runs entirely on Android using native HTTP calls (via BigBone library) and local storage (Realm).

---

## Features

-   **Account Management**
    -   OAuth2 login against `zdf.social`.
    -   Secure token storage.
    *(Multiple account support is not yet implemented but planned).*

-   **Timelines**
    -   Home, Local (`zdf.social`), and Federated timelines.
    -   Pull-to-refresh for updating timelines.
    *(Infinite scroll is a planned improvement).*

-   **Posting & Interactions**
    -   Create toots with text and content warnings.
    -   Select post visibility (Public, Unlisted, Followers Only, Direct).
    -   Like, boost, and bookmark statuses.
    *(Image/media attachments and polls are planned).*
    *(Reply functionality is partially UI-stubbed but not fully implemented).*

-   **Notifications**
    *(Real-time notifications and local push notifications are planned, not yet implemented).*

-   **Offline & Caching**
    -   Timelines are persisted in local storage (Realm) for offline viewing.
    *(Drafts and offline posting queue are planned).*

-   **Customization**
    -   Light, Dark, and System default themes.
    -   Adjustable font sizes (Small, Medium, Large).
    -   Configurable filter lists for keywords and users (hides matching statuses from timelines).

-   **Accessibility**
    -   Content descriptions for interactive elements.
    -   Text scaling with system and in-app font size settings.
    *(Ongoing improvements and full TalkBack/VoiceOver testing are priorities).*

---

## Architecture

1.  **API Client Layer (using BigBone)**
    -   Direct requests to `https://zdf.social/api/v1` via the [BigBone](https://github.com/andregasser/bigbone) library.
    -   Handles OAuth2 authentication flow.

2.  **Data Layer**
    -   Local storage using [Realm Kotlin SDK](https://www.mongodb.com/docs/realm/sdk/kotlin/).
    -   Repository pattern (`StatusRepository`, `FilterRepository`) for managing data from API and local cache.
    -   `AppSettings` using SharedPreferences for theme/font preferences.

3.  **UI Layer**
    -   Declarative UI with Jetpack Compose.
    -   `AppNavigation` for screen routing.
    -   Reusable components in `ui/components` (e.g., `TimelineItem`).
    -   Screens in `ui/screens` (e.g., `TimelineScreen`, `PostCreationScreen`, `SettingsScreen`).

4.  **Auth Flow**
    -   In-app OAuth2 flow redirecting to `zdf.social`.
    -   Access token stored securely using `EncryptedSharedPreferences` (`SecureTokenStorage.kt`).

---

## Getting Started

1.  **Clone the repository.**
2.  **Configure Environment (IMPORTANT for OAuth):**
    *   Before the OAuth flow can complete, you **must** register this application with your Mastodon instance (`https_://zdf.social/settings/applications/new`).
    *   During registration, you'll need to provide a **Redirect URI**. Use `myapp://oauth-callback`.
    *   After registration, the instance will provide a **Client Key (ID)** and **Client Secret**.
    *   Update these placeholder values in `app/src/main/java/com/example/fediversezdfregurgitated/data/remote/AuthConstants.kt`:
        *   `CLIENT_ID_PLACEHOLDER`
        *   `CLIENT_SECRET_PLACEHOLDER`
3.  **Install dependencies and run:**
    *   Open the project in Android Studio.
    *   Let Gradle sync dependencies.
    *   Run the app on an emulator or physical device (Android API 26+).
    *   Standard build command: `./gradlew assembleDebug`
    *   Install command: `./gradlew installDebug`

---

## Configuration

-   **`FEDIVERSE_INSTANCE`**: The base URL for API calls is currently hardcoded to `https://zdf.social` in `AuthConstants.kt` and `MastodonApiClient.kt`.
-   **Themes & Fonts**: Configurable via the in-app Settings screen.
-   **Filters**: Configurable via "Manage Filters" in the in-app Settings screen.

---

## Roadmap / Non-goals (from initial request, status updated)

**Implemented or In Progress:**
- [x] OAuth2 login against zdf.social
- [x] Home, Local, Federated timelines
- [x] Pull-to-refresh
- [x] Create toots with text, content warnings
- [x] Like, boost, bookmark
- [x] Persist timelines in local storage (Realm)
- [x] Light, dark, system themes
- [x] Adjustable font sizes
- [x] Configurable filter lists for keywords and users

**Future / Non-goals for now:**
- [ ] Multiple account support
- [ ] Infinite scroll
- [ ] Image, video, and link attachments in posts
- [ ] Polls in posts
- [ ] Full reply functionality (currently placeholder)
- [ ] Real-time notifications via Mastodon’s streaming API
- [ ] Local push notifications for mentions and boosts
- [ ] Persist drafts in local storage
- [ ] Read and queue posts offline; auto-publish when back online
- [ ] Adjustable image preview sizes (once inline images are supported)
- [ ] Full VoiceOver/TalkBack support (initial pass done, needs thorough testing)
- [ ] Dynamic type sizing and high-contrast mode (initial pass done, needs thorough testing)
- [ ] Direct Messages via Mastodon API v2
- [ ] Post scheduling and drafts management
- [ ] Instance discovery: browse other Fediverse servers
- [ ] Plugin system for alternate Fediverse protocols (Pleroma, Misskey)


---

## Contributing

1.  Fork the repo.
2.  Create a feature branch (`feature/awesome-feature`).
3.  Commit your changes with clear messages.
4.  Open a Pull Request against `main`.
5.  Ensure IntelliJ IDEA code inspections pass and adhere to standard Kotlin coding conventions. (An `.editorconfig` might be added later).

---

## License

Distributed under the EUPL v1.2 License. See [EUPL v1.2 Text](https://commission.europa.eu/content/eupl-text-eupl-12_en).Tool output for `create_file_with_block`:
