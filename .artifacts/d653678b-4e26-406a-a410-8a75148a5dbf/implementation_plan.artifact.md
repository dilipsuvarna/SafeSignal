# SafeSignal Implementation Plan

This plan details the steps to build a complete offline communication app using Google Nearby Connections API.

## User Review Required

> [!IMPORTANT]
> The app requires multiple permissions (Bluetooth, Location, WiFi) to function. The user will be prompted for these on launch.
> The app uses Nearby Connections Strategy `P2P_CLUSTER`, which allows for N-to-N connections.

## Proposed Changes

### Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/gradle/libs.versions.toml)
Add Navigation and ViewModel Compose dependencies.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/build.gradle.kts)
Apply added dependencies.

### Models

#### [NEW] [Models.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/model/Models.kt)
Define `Device`, `Message`, `ConnectionStatus`, and `ChatState`.

### Core Logic (Nearby Connections)

#### [NEW] [NearbyManager.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/nearby/NearbyManager.kt)
Encapsulate Google Nearby Connections API logic (Advertising, Discovery, Payload handling).

#### [NEW] [NearbyRepository.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/nearby/NearbyRepository.kt)
Manage data flow between NearbyManager and UI.

### ViewModels

#### [NEW] [MainViewModel.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/viewmodel/MainViewModel.kt)
Manage home screen state and discovery/advertising controls.

#### [NEW] [ChatViewModel.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/viewmodel/ChatViewModel.kt)
Manage chat history and message sending.

### UI Components

#### [NEW] [Theme.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/theme/Theme.kt)
Material 3 Dark Theme implementation.

#### [NEW] [Components.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/components/Components.kt)
Common UI elements like message bubbles and device cards.

#### [NEW] [Screens.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/screens/Screens.kt)
Home, Discovery, Connected Devices, and Chat screens.

#### [NEW] [Navigation.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/ui/navigation/Navigation.kt)
NavHost configuration.

### Activity & Permissions

#### [MODIFY] [MainActivity.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/MainActivity.kt)
Handle permission requests and set up the Compose UI.

#### [NEW] [PermissionUtils.kt](file:///C:/Users/dilip/AndroidStudioProjects/SafeSignal/app/src/main/java/com/example/safesignal/util/PermissionUtils.kt)
Helper for requesting complex Android 12+ permissions.

## Verification Plan

### Automated Tests
- Build the project using `./gradlew assembleDebug`.

### Manual Verification
- Deploy to two physical devices.
- Turn on Advertising on one, Discovery on the other.
- Connect devices.
- Send messages back and forth.
- Verify Broadcast functionality.
