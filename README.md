# IMS DC SDK

5G Enhanced Call refers to a series of services that enhance system capabilities and enable service innovation based on the operator's IMS network, which improve the user experience for both individual and enterprise users. The most notable technical feature is the addition of a data channel and AR technology on top of the existing IMS audio and video channels, and it relies on the IMS DC SDK on the device, enabling interactive information exchange during calls and upgrading the user experience.

The IMS DC SDK was developed by the China Telecom Research Institute. The chip provides IMS DC (Data Channel) capabilities, while terminal manufacturers encapsulate these capabilities. The SDK manages these capabilities and provides a unified JS API to H5 applications on the platform side, allowing H5 applications across different operator platforms to seamlessly invoke the terminal's IMS DC capabilities for data interaction.

## Features

The SDK monitors call states. When a call is established, it binds to the OEM's DC Service, waits for the BDC to be established, retrieves the IMS DC Application list and downloads the IMS DC Application package via the BDC, and initiates or receives ADC creation based on IMS DC Application parameters. During the call, it provides various capabilities, including DC, based on the JS APIs called by the IMS DC Applications. During call hold, IMS DC Applications are disabled. When the call ends, all resources are released.

Key features include:
1. Supports running multiple IMS DC Applications simultaneously, each in an independent process.
2. Supports opening and using IMS DC Applications during or after a call.
3. Allows operators and OEMs to integrate their unique capabilities into IMS DC Applications.
4. Supports initiating and receiving DC data channel creation, with ADC caching when IMS DC Applications are closed.
5. Obtains floating window permissions to display entry points on the dialer page.
6. Implements license control for sensitive JS APIs.
7. Supports screen sharing based on OEM capabilities.
8. Supports simulated incoming calls and loading local IMS DC Application packages for debugging.
9. Other features.

## Development Environment

- JDK Version: 17
- Gradle Version: 8.1
- Android SDK Version: compileSdk-34, minSdk-21

## Architecture Design

![SDK Architecture.jpg](images/SDK架构.png)

1. Tightly binds with call states by implementing Android's `InCallService`.
2. The SDK interacts with the OEM-provided DC Service via the DC AIDL interface (`com.newcalllib.datachannel.V1_0`) to acquire and encapsulate DC data channel capabilities.
3. Provides a unified JS API interface (`JSApi`) for IMS DC Applications, offering capabilities such as DC data channels, extended features, and data caching, implemented using DSBridge.
4. Operators/OEMs implement the `IEC` interface defined in the `base` module and package it as an AAR library, which is integrated into `ExpandingCapacityManager` to provide unique capabilities. IMS DC Applications invoke these capabilities via the `expandingCapacityRequest` JS API, following the parameter structure: `{"provider":"","module":"","func":"","data":T}`.

## Project Structure

NewCall  
├── app/ IMS DC Application space related  
├── base/ Common classes, indirectly referenced by other modules (referenced as an AAR)  
│ ├── data/ Data structures  
│ └── port/ Interfaces  
├── build-logic/ Project build configurations  
├── core/ Core logic code  
│ ├── aidl/ AIDL interfaces for DC, screen sharing, extended capabilities, etc.  
│ └── core/  
│ ├── common/ Utility tools  
│ ├── constants/ Constant configurations  
│ ├── data/ Data structures  
│ ├── dispatcher/ JS API and IMS DC Application service event dispatching  
│ ├── factory/ Dispatcher factory  
│ ├── manager/ Various management classes  
│ ├── miniapp/ IMS DC Application (management, UI, DC, etc.) related  
│ ├── picker/ Image selection  
│ ├── port/ Various interfaces  
│ ├── service/ IMS DC Application, call, and other services  
│ ├── ui/ UI components (excluding IMS DC Application space and IMS DC Applications themselves)  
│ ├── usecase/ JS API and IMS DC Application service event handling  
│ └── utils/ Utility classes  
├── libs/ Third-party libraries  
├── script/ Build scripts  
├── testing/ Local simulation testing  
└── webrtcDC/ Implements GSMA ts.66-defined interfaces based on the SDK, compiling into a JS library for IMS DC Application integration  


## Technology Stack

- Programming Languages: Kotlin, Java, Rust
- Architecture Pattern: MVVM
- Asynchronous Handling: Coroutines + Flow
- Database: Room
- UI Framework: Jetpack Compose / XML Layouts

## Build and Release

1. **Adaptation**:  
   The SDK relies on the terminal's DC, screen sharing, extended capabilities, and AR services, which must be supported by the terminal.

2. **Packaging**:  
   Supports multi-channel packaging.
   ```bash
   ./gradlew assembleRelease
3. **Release**:  
The SDK must be integrated as a system default application by the phone manufacturer and released with the system.  

## License
This project is licensed under the Apache 2.0 License.

## Contact
xuq17@chinatelecom.cn