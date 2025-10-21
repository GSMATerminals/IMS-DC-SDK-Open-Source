Note: This document aims to help developers understand the architectural design of the 5G New Calling Terminal SDK, facilitating rapid secondary development or direct compilation and use based on this project, and guiding the development and debugging of 5G New Calling Application.

# 5G New Calling Terminal SDK
- 5G New Calling adds a data channel (i.e., IMS Data Channel) on top of the IMS audio and video channels, integrating AR, AI, and other technologies to enable interactive information exchange during calls.

- The 5G New Calling Terminal SDK (hereinafter referred to as the SDK) is developed by the China Telecom Research Institute. After a call is established, the chip and the network negotiate the establishment of the IMS Data Channel. The terminal encapsulates the call logic for the IMS Data Channel and provides it to the SDK via AIDL interfaces ([Android Interface Definition Language](https://developer.android.google.cn/develop/background-work/services/aidl)). The SDK serves as the runtime environment for the 5G New Calling Application (hereinafter referred to as IMS Data Channel Application) and provides a unified interface for IMS Data Channel Application to call, enabling them to operate the IMS Data Channel and other terminal capability interfaces.

![ScreenShare(Initiator).gif](images/ScreenShare(Initiator).gif) ![ScreenShare(Receiver).gif](images/ScreenShare(Receiver).gif)

## I. Features
While complying with international standards such as 3GPP and GSMA, the SDK also implements the following features：

- Closely associated with call state, manages the lifecycle of IMS Data Channel Application, providing a stable runtime environment;

- Isolated storage space for IMS Data Channel Application, ensuring data security;

- Provides interfaces for IMS Data Channel Application to operate the IMS Data Channel and other terminal capabilities;

- Supports running multiple IMS Data Channel Application simultaneously, each in an independent process;

- Supports opening and using IMS Data Channel Application after a call is established;

- Supports different operators and terminal manufacturers implementing extended capability interfaces, introducing their respective private features via AAR packages;

- Supports IMS Data Channel caching when a IMS Data Channel Application is closed during a call, avoiding re-creation of the IMS Data Channel when the IMS Data Channel Application is reopened;

- Manages sensitive JS APIs (related to permissions, data privacy, etc.) through a license verification mechanism;

- Supports simulated calls and loading local IMS Data Channel Application packages for offline debugging.

## II. Architecture Design
![SDK架构.png](images/SDK架构.png)

- Inherits InCallService: The SDK obtains call information by inheriting InCallService (https://developer.android.google.cn/reference/android/telecom/InCallService), closely associating with call state. It starts running when a call is established and releases resources when the call ends. Refer to SDK code: [core/src/main/java/com/ct/ertclib/dc/core/service/InCallServiceImpl.kt](core/src/main/java/com/ct/ertclib/dc/core/service/InCallServiceImpl.kt).  

- TS.71 IDL Interfaces: Interfaces between the SDK and the terminal. The SDK uses these interfaces to obtain the IMS Data Channel Application list, download IMS Data Channel Application packages, create IMS Data Channels and send/receive data as instructed by the IMS Data Channel Application. Refer to SDK code: [core/src/main/aidl/com/newcalllib/datachannel/V1_0](core/src/main/aidl/com/newcalllib/datachannel/V1_0).  

- Extended Capability Interfaces: To promote the unification of the 5G New Calling Terminal SDK (i.e., only one SDK runs on a terminal) while meeting the needs of operators to provide unique features for their platform's IMS Data Channel Applications, the SDK designs extended capability interfaces. This allows different operators and terminal manufacturers to introduce their respective private features via AAR packages for IMS Data Channel Applications to call.

    1) Definition of extended capability interfaces, refer to SDK code: [base/src/main/java/com/ct/ertclib/dc/base/port/ec/IEC.kt](base/src/main/java/com/ct/ertclib/dc/base/port/ec/IEC.kt);    

    2) Logic for the SDK to integrate and manage extended capabilities, refer to SDK code: [core/src/main/java/com/ct/ertclib/dc/core/manager/common/ExpandingCapacityManager.kt](core/src/main/java/com/ct/ertclib/dc/core/manager/common/ExpandingCapacityManager.kt);    

    3) Example implementation of extended capabilities by a terminal manufacturer, refer to SDK code: [oemec/src/main/java/com/ct/oemec/OemEC.kt](oemec/src/main/java/com/ct/oemec/OemEC.kt).    

- JS API Interfaces: Interfaces between IMS Data Channel Applications and the SDK, implemented using the DSBridge framework (https://github.com/wendux/DSBridge-Android). IMS Data Channel Applications use these interfaces to operate the IMS Data Channel and other terminal capabilities (including the aforementioned extended capabilities). Refer to SDK code: [core/src/main/java/com/ct/ertclib/dc/core/miniapp/bridge/JSApi.kt](core/src/main/java/com/ct/ertclib/dc/core/miniapp/bridge/JSApi.kt).  

## III. Project Structure
NewCall  
├── app/ IMS Data Channel Application list display related  
├── base/ Basic common classes, not directly referenced by other modules, referenced after being compiled into an aar  
│ ├── data/ Data structures  
│ └── port/ Interfaces  
├── build-logic/ Contains project build-related files  
├── core/ Core logic code  
│ ├── aidl/ AIDL interfaces for DC, screen sharing, extended capabilities, etc.  
│ └── core/  
│ ├── common/ Common utilities  
│ ├── constants/ Constant configurations  
│ ├── data/ Data structures  
│ ├── dispatcher/ JS API and IMS Data Channel Application service event dispatching  
│ ├── factory/ Dispatcher factory  
│ ├── manager/ Various managers  
│ ├── miniapp/ IMS Data Channel Application (management, UI, DC, etc.) related  
│ ├── picker/ Image selection  
│ ├── port/ Various interfaces  
│ ├── service/ IMS Data Channel Application, call, and other services  
│ ├── ui/ Other UI besides the IMS Data Channel Application space and IMS Data Channel Applications themselves  
│ ├── usecase/ JS API and IMS Data Channel Application service event handling  
│ └── utils/ Utility classes  
├── libs/ Third-party library files  
├── miniapp/ IMS Data Channel Application development related  
│ ├── webrtcDC/ Implements interfaces defined by GSMA TS.66 based on the SDK, can compile the webrtcDC.js library for IMS Data Channel Application integration  
│ └── demo/ IMS Data Channel Application examples  
├── oemec/ Terminal manufacturer extended capabilities  
├── script/ Build scripts  
└── testing/ Local simulation testing related  

## IV. Technology Stack
- Programming Languages: Kotlin, Java, JS

- Architecture Pattern: MVVM

- Asynchronous Processing: Coroutines + Flow

- Database: Room

- UI Framework: Jetpack Compose / XML Layouts

## V. Development Environment
- JDK Version 17

- Gradle Version 8.1

- Android SDK Version compileSdk-34, minSdk-26

- Recommended Development Tool: AndroidStudio

## VI. Build & Release
- Packaging: Currently configured for three distribution channels: Normal (Floating Ball entry version), Dialer (Dialer entry version), Local (Local debugging version, for local debugging only)
   ```bash
   ./gradlew assembleRelease 

- Terminal Adaptation:
Terminals must be adapted according to the [《5G New Calling SDK Terminal Adaptation Specification》](/document/5G New Calling SDK Terminal Adaptation Specification.docx) to ensure the proper functioning of all SDK features.

- Release:
Terminal manufacturers integrate the SDK as a system default application and push it to adapted user terminals along with the system.
When a user is on a call, if the device integrates the Normal version, a 5G New Calling icon will appear as a floating bubble on the native call interface, tapping this icon will open the 5G New Calling IMS Data Channel Application space; If the device integrates the Dialer version and has been implemented according to the adaptation specifications, a fixed entry button will appear on the native call interface, clicking this button will open the 5G New Calling IMS Data Channel Application space.

## VII. IMS Data Channel Application Development & Debugging
Using the Local (Local debugging version) SDK, developers can debug IMS Data Channel Applications on ordinary Android terminals without relying on an IMS Data Channel network environment or terminal adaptation.

- IMS Data Channel Application Development: Developers need to follow web standards like HTML5, CSS3, ES6 for web development. The document [《5G New Calling IMS Data Channel JS API》](/document/5G New Calling IMS Data Channel JS API.docx) lists all interfaces exposed by the SDK to IMS Data Channel Applications. IMS Data Channel Application developers should refer to this document for development. Refer to example IMS Data Channel Application code: [miniapp/demo/IMS_DC_Mini_app_demo_source_code](miniapp/demo/IMS_DC_Mini_app_demo_source_code).  

- IMS Data Channel Application Packaging: Package the web project into an offline zip format compressed package, i.e., the IMS Data Channel Application package. The index.html and properties.json files must be in the root directory of the zip package. Refer to the example IMS Data Channel Application package: [miniapp/demo/IMS_DC_Mini_app_demo.zip](miniapp/demo/IMS_DC_Mini_app_demo.zip).  

- IMS Data Channel Application Local Debugging: Install the Local version SDK onto the phone like a regular APK. Push the IMS Data Channel Application zip package to the phone's sdcard. Then launch the "Telecom Enhanced Calling" app from the phone's home screen. After granting permissions as guided, open Settings -> Local Debugging Entry to configure and debug the IMS Data Channel Application.  
<img src="images/localtest.png" alt="Description" width="200" />

## VIII. License
This project is licensed under the Apache 2.0 License.

## IX. Contact
xuq17@chinatelecom.cn