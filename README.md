# IMS DC SDK

- 5G Enhanced Call refers to a series of services that enhance system capabilities and enable service innovation based on the operator's IMS network, which improve the user experience for both individual and enterprise users. The most notable technical feature is the addition of a data channel and AR technology on top of the existing IMS audio and video channels, and it relies on the IMS DC SDK on the device, enabling interactive information exchange during calls and upgrading the user experience.

- The IMS DC SDK was developed by the China Telecom Research Institute. The chip provides IMS DC (Data Channel) capabilities, while terminal manufacturers encapsulate these capabilities. The SDK manages these capabilities and provides a unified JS API to H5 applications on the platform side, allowing H5 applications across different operator platforms to seamlessly invoke the terminal's IMS DC capabilities for data interaction.

## Features

- The SDK monitors call states. When a call is established, it binds to the OEM's DC Service, waits for the BDC to be established, retrieves the IMS DC Application list and downloads the IMS DC Application package via the BDC, and initiates or receives ADC creation based on IMS DC Application parameters. During the call, it provides various capabilities, including DC, based on the JS APIs called by the IMS DC Applications. During call hold, IMS DC Applications are disabled. When the call ends, all resources are released.

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
- Recommend IDE : Android Studio

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
├── miniapp/IMS DC Mini Application development     
│ ├── webrtcDC/ Implements GSMA ts.66-defined interfaces based on the SDK, compiling into a JS library webrtcDC.js for IMS DC Application integration  
│ └── demo/IMS DC Mini Application example  
├── oemec/OEM EC development  
├── script/ Build scripts  
└── testing/ Local simulation testing  


## Technology Stack

- Programming Languages: Kotlin, Java, Rust
- Architecture Pattern: MVVM
- Asynchronous Handling: Coroutines + Flow
- Database: Room
- UI Framework: Jetpack Compose / XML Layouts

## Build and Release

1. **Adaptation**:  
- The SDK relies on the terminal's DC, screen sharing, extended capabilities, and AR services, which must be supported by the terminal;  
- The terminal must be granted the android.permission.CONTROL_INCALL_EXPERIENCE and android.permission.PACKAGE_USAGE_STATS permissions by default.  

2. **Packaging**:  
   Supports multi-channel packaging: Normal(General Version)、Dialer(Dial Pad Entry Version)、Local(Local Debug Version)
   ```bash
   ./gradlew assembleRelease
3. **Release**:  
The SDK must be integrated as a system default application by the phone manufacturer and released with the system.  

## IMS DC Mini Application Local Debugging  
The Local (Local Debug Version) SDK allows debugging the IMS DC Mini Application on the SDK without requiring a DC network environment.
- Development：Developers should follow web standards such as HTML5, CSS3, and ES6 for web page development. For a complete demo, refer to: [miniapp/demo/IMS_DC_Mini_app_demo_source_code](miniapp/demo/IMS_DC_Mini_app_demo_source_code).  
- Packaging: The web page should be packaged into an offline ZIP archive (the IMS DC Mini Application). The index.html and properties.json files must be placed in the root directory of the ZIP package. See example: [miniapp/demo/IMS_DC_Mini_app_demo.zip](miniapp/demo/IMS_DC_Mini_app_demo.zip).
- Install the Local Debug Version apk on the phone. Push the IMS DC Mini Application ZIP package to the phone's SDCard. Configure the settings as shown in the diagram below to launch and debug the IMS DC Mini Application:  
  1、<img src="images/localtest1.png" alt="描述文字" width="200" />2、<img src="images/localtest2.png" width="200" />3、<img src="images/localtest3.png" width="200" />  
  4、<img src="images/localtest4.png" alt="描述文字" width="200" />5、<img src="images/localtest5.png" width="200" />6、<img src="images/localtest6.png" width="200" />  
  7、<img src="images/localtest7.png" alt="描述文字" width="200" />8、<img src="images/localtest8.png" width="200" />9、<img src="images/localtest9.png" width="200" />  
  10、<img src="images/localtest10.png" alt="描述文字" width="200" />11、<img src="images/localtest11.png" width="200" />12、<img src="images/localtest12.png" width="200" />

## License
This project is licensed under the Apache 2.0 License.
 
## Contact
xuq17@chinatelecom.cn
