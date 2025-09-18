# MultiPoint Floating Clicker

A React Native Android application that displays configurable floating points on the top layer of any Android application. Users can drag points to adjust positions, and the agent can simulate clicks on underlying apps using Accessibility Service.

## Features

### 🎯 Floating Overlay
- Points are displayed above all apps using `TYPE_APPLICATION_OVERLAY`
- Points are draggable and clickable
- Multiple points can be added dynamically based on user configuration

### 🔄 Synchronized Clicks
- Clicking a designated area or button triggers all configured points simultaneously
- Each point can also be clicked individually to trigger its assigned action

### ♿ Accessibility Service Integration
- Uses Accessibility Service to simulate touch events on underlying apps
- Checks if Accessibility Service is enabled and prompts the user to activate it if not
- Supports precise x, y coordinate targeting for automated clicks

### 💾 Configuration & Persistence
- Points configuration is stored in JSON format
- Positions are persisted locally using AsyncStorage
- Users can modify positions by dragging points; changes are automatically saved

### 🏗️ React Native + Kotlin Architecture
- Frontend UI built with React Native + TypeScript for draggable points and user interaction
- Native module in Kotlin exposes methods to start/stop floating window and trigger clicks
- Kotlin service handles overlay window management and communicates with Accessibility Service

## Prerequisites

- React Native development environment set up
- Android Studio with Android SDK
- Android device or emulator (API level 21+)
- Node.js 20+

## Installation

1. Clone the repository:
```bash
git clone <repository-url>
cd MDot
```

2. Install dependencies:
```bash
npm install
```

3. For Android, navigate to the android directory and run:
```bash
cd android
./gradlew clean
cd ..
```

## Usage

### 1. Launch the App
```bash
npm run android
```

### 2. Grant Permissions
The app requires two critical permissions:

#### Overlay Permission
- Tap "请求权限" (Request Permission) for overlay access
- Go to Settings > Apps > MDot > Display over other apps
- Enable the permission

#### Accessibility Permission
- Tap "启用服务" (Enable Service) for accessibility access
- Go to Settings > Accessibility > MultiPoint Floating Clicker
- Enable the service

### 3. Configure Points
- Drag the floating points to adjust their positions
- Positions are automatically saved
- Click individual points to test them

### 4. Start Floating Window
- Tap "启动悬浮窗" (Start Floating Window)
- Points will now appear on top of all other apps
- You can minimize the main app and use other applications

### 5. Trigger Actions
- Click individual floating points to trigger single clicks
- Use the "Trigger All" button to click all points simultaneously
- Points will simulate clicks on the underlying application

### 6. Stop Floating Window
- Return to the app and tap "停止悬浮窗" (Stop Floating Window)
- Or stop the service from Android settings

## Architecture

### React Native Frontend
- **HomeScreen**: Main configuration interface
- **DraggablePoint**: Individual draggable point component
- **FloatingClicker**: Native module interface

### Android Native Components
- **FloatingClickerModule**: React Native bridge module
- **FloatingWindowService**: Manages the overlay window
- **AccessibilityClickService**: Handles click simulation

### Data Flow
1. User configures points in React Native UI
2. Points are saved to AsyncStorage
3. Native module starts FloatingWindowService
4. Service creates overlay with draggable points
5. Accessibility service simulates clicks when triggered

## Permissions Required

- `SYSTEM_ALERT_WINDOW`: Display floating overlay
- `BIND_ACCESSIBILITY_SERVICE`: Simulate clicks on other apps
- `FOREGROUND_SERVICE`: Keep service running
- `WAKE_LOCK`: Prevent device sleep during automation

## Configuration

### Points Configuration
Points are stored in `src/config/points.json`:
```json
[
  { "id": "point1", "x": 50, "y": 100 },
  { "id": "point2", "x": 150, "y": 200 },
  { "id": "point3", "x": 250, "y": 300 }
]
```

### Accessibility Service Configuration
The accessibility service is configured in `android/app/src/main/res/xml/accessibility_service_config.xml`:
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100"
    android:packageNames="" />
```

## Development

### Building for Release
```bash
cd android
./gradlew assembleRelease
```

### Debugging
- Use React Native debugger for frontend debugging
- Use Android Studio for native debugging
- Check logs with `adb logcat | grep -E "(FloatingClicker|AccessibilityClickService)"`

## Troubleshooting

### Common Issues

1. **Overlay Permission Denied**
   - Ensure the app has overlay permission in Android settings
   - Restart the app after granting permission

2. **Accessibility Service Not Working**
   - Verify the service is enabled in Accessibility settings
   - Check that the service has the correct package name

3. **Points Not Clicking**
   - Ensure the target app is visible and interactive
   - Check that coordinates are within screen bounds
   - Verify accessibility service is active

4. **Floating Window Not Showing**
   - Check overlay permission
   - Ensure the service is running
   - Restart the app if needed

### Logs
Check Android logs for debugging:
```bash
adb logcat | grep -E "(MDot|FloatingClicker|AccessibilityClickService)"
```

## Security Considerations

- This app requires sensitive permissions (overlay and accessibility)
- Only use on trusted devices
- Be cautious when automating sensitive applications
- The accessibility service can interact with any app on the device

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly on Android devices
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Disclaimer

This tool is for educational and accessibility purposes. Users are responsible for complying with all applicable laws and terms of service when using this application. The developers are not responsible for any misuse of this software.