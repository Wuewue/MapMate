# Zenly Clone Project Documentation

This document provides a comprehensive overview of the Zenly Clone project, including its dependencies, setup instructions, and the internal architecture.

---

## 1. Required Dependencies

The application relies on several key packages to function properly (defined in `package.json`):

*   **Core Frameworks:** `expo`, `react`, `react-native`
*   **Navigation:** `expo-router` (File-based routing system)
*   **Backend & Database:** `firebase` (Used for Authentication and real-time Firestore database syncing)
*   **Map Interface:** `react-native-maps` (Displays the map, custom markers, and handles gestures)
*   **Device APIs:**
    *   `expo-location`: Accesses device GPS for live location tracking and reverse geocoding (finding city/region names).
    *   `expo-battery`: Retrieves battery percentage and charging status to display on friends' map markers.
*   **UI Elements:**
    *   `expo-linear-gradient`: Creates the visually appealing gradients used for map markers.
    *   `@expo/vector-icons`: Provides standard UI icons across the application.

---

## 2. How to Open the Project

1.  Open your preferred IDE (e.g., Visual Studio Code).
2.  Open the terminal and navigate into the `Demo` directory where the React Native app is located:
    ```bash
    cd g:/demoo/MapMate/Demo
    ```
3.  Install all the required dependencies by running:
    ```bash
    npm install
    ```

---

## 3. How to Launch the App

Once dependencies are installed, you can launch the app locally:

1.  Start the Expo development server:
    ```bash
    npx expo start
    ```
2.  **To view the app:**
    *   Press **`a`** in the terminal to launch it on a running **Android Emulator**.
    *   Press **`i`** to launch it on an **iOS Simulator** (Mac only).
    *   Alternatively, download the **Expo Go** app on your physical mobile device and scan the QR code displayed in the terminal.

---

## 4. Project Structure & How Files Connect

The project follows a standard Expo Router structure, where the filesystem dictates the navigation paths.

### 📂 `/app` (Screens & Navigation)
This folder manages what the user sees and how they navigate between screens.

*   **`_layout.tsx` (Root Layout):** This is the entry point of the app's UI. It constantly listens to the Firebase authentication state (from `/utils/firebase.ts`).
    *   *Connection:* If a user is logged in, it redirects them to the `/(tabs)` group. If not, it redirects them to `login.tsx`.
*   **`login.tsx`:** The authentication screen.
    *   *Connection:* It allows users to sign in or automatically creates a new account if the email isn't recognized. Upon successful login, it creates a base user document in Firestore and triggers the `_layout.tsx` redirect.
*   **`/(tabs)/`:** This folder represents the main, authenticated portion of the app containing the bottom navigation bar.
    *   **`_layout.tsx`:** Configures the visual layout of the bottom tabs (Map and Profile).
    *   **`index.tsx` (Map Screen):** The core feature. 
        *   *Connection:* It uses `expo-location` to track your coordinates and pushes them to Firestore. It constantly listens to the `locations` and `users` collections in Firestore to render your friends' live locations, battery levels, and details on the `react-native-maps` component. It also makes external requests to Open-Meteo for real-time weather data.
    *   **`profile.tsx`:** The settings screen.
        *   *Connection:* Reads and writes to Firestore. Allows the user to manage friend requests, toggle their privacy "Ghost Mode" (Precise, Blurred, Frozen), and log out of their account.

### 📂 `/components` (Reusable UI)
Contains shared React components to keep the app visually consistent and the code DRY (Don't Repeat Yourself).
*   Files like `themed-text.tsx` and `parallax-scroll-view.tsx` provide styled, boilerplate UI elements used within the `/app` screens.

### 📂 `/utils` (Services)
*   **`firebase.ts`:** The central hub for all backend connections. It initializes the Firebase application, Authentication, and Firestore database instances.
    *   *Connection:* This file is imported by almost every screen (`login.tsx`, `index.tsx`, `profile.tsx`, `_layout.tsx`) to perform reads, writes, and authentication checks.

### 📂 `/constants` (Design System)
*   **`theme.ts`:** Holds the primary color palettes for both Light and Dark modes.

### 📂 `/hooks` (Custom Logic)
*   Contains custom React Hooks like `use-color-scheme.ts` to cleanly manage the device's theme state across different components.

### ⚙️ Root Configuration Files
*   **`package.json`**: Lists all npm dependencies and custom CLI scripts.
*   **`app.json`**: The central configuration for Expo. It defines the app's name, icon, splash screen, and necessary permissions (like background location).
