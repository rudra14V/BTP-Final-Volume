# Project Plan

Build VoiceLock - an Android Voice-Based App Locker using Kotlin, Jetpack Compose, ECAPA-TDNN speaker verification model (ONNX Runtime / TFLite), Android Accessibility Service, DataStore, and Android Keystore. Key features: 3-step voice registration ("My voice is my password"), speaker embedding extraction, max cosine similarity verification (threshold 0.83), protected app selection list, Accessibility Service foreground monitor, lock overlay UI, configurable grace period (10s), secure storage using Android Keystore/DataStore, security settings, failed attempt handling.

## Project Brief

# VoiceLock - Project Brief

VoiceLock is an Android voice-based app locker that secures sensitive applications using biometric voice verification powered by on-device machine learning.

## Features

1. **Voice Passphrase Enrollment & Verification**: A 3-step voice registration flow ("My voice is my password") that extracts speaker embeddings using an on-device ECAPA-TDNN model and authenticates users via cosine similarity verification.
2. **Protected App Selection**: Interactive list of installed user applications allowing users to select which applications require voice verification prior to launch.
3. **Accessibility Foreground App Monitor & Lock Overlay**: An Android Accessibility Service that monitors foreground app transitions and displays a secure Jetpack Compose lock screen overlay when a protected application is accessed.
4. **Security Settings & Re-authentication Logic**: Configurable unlock grace period (e.g., 10-second window before re-locking), failed attempt handling, and secure encrypted storage for speaker embeddings using Android Keystore and DataStore.

## High-Level Tech Stack

- **Language & Runtime**: Kotlin, Kotlin Coroutines, Flow
- **UI Framework**: Jetpack Compose, Material Design 3
- **Navigation & Adaptive Strategy**: Jetpack Navigation 3 (state-driven), Compose Material Adaptive
- **On-Device Machine Learning**: ONNX Runtime / TFLite with ECAPA-TDNN model
- **System Integration**: Android Accessibility Service, System Alert Window overlay (`TYPE_APPLICATION_OVERLAY`)
- **Storage & Security**: Preferences DataStore, Android Keystore

## Implementation Steps

### Task_1_Setup_Model_And_Enrollment: Implement Audio Recorder, ECAPA-TDNN ONNX Runtime/TFLite model integration, speaker embedding extraction, cosine similarity verification logic, and Android Keystore/DataStore secure storage for speaker embeddings. Build 3-phase voice enrollment screen and testing mode UI in Jetpack Compose.
- **Status:** COMPLETED
- **Updates:** Implemented AudioRecorder, AudioPreprocessor, VoiceActivityDetector, EcapaTdnnModel ONNX Runtime integration, SpeakerEmbeddingExtractor, SimilarityCalculator with max similarity > 0.83, EncryptionManager (Android Keystore AES-GCM), SecureStorage (DataStore), VoiceRepository, RegistrationViewModel & VoiceRegistrationScreen (3-step enrollment with passphrase "My voice is my password"), and TestingViewModel & TestingScreen (development test mode). All unit tests pass and debug assemble succeeds.
- **Acceptance Criteria:**
  - Audio recording captures voice passphrase
  - ECAPA-TDNN model extracts speaker embeddings on-device
  - Embeddings are encrypted using Android Keystore and saved to DataStore
  - Cosine similarity matching calculates similarity score against stored embedding
  - 3-phase voice enrollment screen and testing UI fully functional
- **Duration:** N/A

### Task_2_App_Selection_And_Settings: Build Protected App Selection screen listing installed applications with toggle switches, and Settings screen managing grace period timer, failed attempt thresholds, and re-enrollment options using DataStore and MVVM architecture.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Installed user applications listed with icon, name, and toggle switch
  - Protected app package list persisted in DataStore
  - Settings screen allows configuring unlock grace period duration and re-authentication parameters
- **StartTime:** 2026-09-25 12:03:23 IST

### Task_3_Accessibility_Service_And_Lock_Overlay: Implement Android Accessibility Service to detect foreground app launches, check against protected apps list and grace period, and launch System Alert Window overlay requiring voice verification to unlock.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Accessibility Service monitors foreground app package transitions
  - Lock overlay window pops up when opening a protected app
  - Voice verification on lock overlay validates passphrase and dismisses overlay on successful similarity match (>0.83)
  - Grace period active window bypasses lock screen re-prompting

### Task_4_Run_And_Verify: Run build and verify end-to-end VoiceLock application stability, ensuring full functionality from voice enrollment to accessibility app locking/unlocking, no crashes, and alignment with user requirements.
- **Status:** PENDING
- **Acceptance Criteria:**
  - build pass
  - make sure all existing tests pass
  - app does not crash
  - verify application stability, alignment with user requirements, and report critical UI issues

