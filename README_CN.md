# 5G增强通话终端SDK
5G增强通话是指基于运营商IMS网络进行系统增强和业务创新，使得个人及企业行业的用户体验获得提升的一系列业务统称。其最显著的技术特征是在基于IMS音频、视频通道的基础上增加数据通道和融入AR等技术，依赖终端上的5G增强通话终端SDK，实现用户在通话时的交互式信息传递和互动，带来用户通话体验升级。  
5G增强通话终端SDK由中国电信研究院开发。芯片提供IMS DC通道能力，终端厂商将芯片能力进行封装，SDK管理这些能力并向平台侧的H5应用提供统一的JS API，以实现不同运营商业务平台上的H5应用均可无差别地调用终端的IMS DC能力进行数据交互。

## 一、功能特性
SDK监听通话状态，在通话建立时，绑定OEM的DC Service，等待BDC建立后，通过BDC获取小程序列表和下载小程序包，并按照小程参数发起ADC的建立和接收对端或网络发起建立的ADC；通话中，根据小程序调用的js api提供包括DC在内的各种能力；通话保持时，禁用小程序；通话挂断时，释放所有资源。  
主要包含如下特性：  
1、支持多个小程序同时运行，各自独立进程；  
2、支持通话中、通话后打开使用小程序；  
3、支持运营商、OEM接入面向小程序的各自特色能力；  
4、支持发起和接收DC数据通道的创建，在关闭小程序时支持ADC缓存；  
5、获取悬浮窗权限，以浮窗形式在拨号盘页面显示入口；  
6、支持对敏感的js api进行license控制；  
7、支持基于OEM能力实现屏幕共享；  
8、支持模拟来电、加载本地小程序包进行调试；  
9、其他。  

## 二、开发环境
JDK 版本 17  
Gradle 版本 8.1  
Android SDK 版本 compileSdk-34，minSdk-21  

## 三、架构设计
![SDK架构CN.jpg](images/SDK架构CN.jpg)  
1、通过实现Android系统的InCallService与通话状态紧捆绑；  
2、SDK通过DC AIDL接口(com.newcalllib.datachannel.V1_0)与OEM提供的DC Service交互，获取和包装DC数据通道能力；  
3、通过统一的JS API接口(JSApi)为小程序提供包括DC数据通道、拓展能力、数据缓存等在内的所有能力，使用DSBridge实现；  
4、各运营商/OEM实现base模块中定义的IEC接口后打包成arr库，在ExpandingCapacityManager中接入，以提供特色能力，小程序通过js api中的expandingCapacityRequest调用特色能力，需遵循参数结构{"provider":"","module":"","func":"","data":T}。  

## 四、项目结构 
NewCall  
├── app/小程序空间相关  
├── base/公共类，其他模块不直接引用，编译成aar后引用  
│   ├── data/数据结构  
│   └── port/接口  
├── build-logic/存放项目构建相关    
├── core/核心逻辑代码  
│   ├── aidl/DC、屏幕共享、拓展能力等AIDL接口  
│   └── core/  
│       ├── common/通用工具  
│       ├── constants/常量配置  
│       ├── data/数据结构  
│       ├── dispatcher/js api、小程序服务事件调度  
│       ├── factory/调度器工厂
│       ├── manager/各种管理类  
│       ├── miniapp/小程序(管理、UI、DC等)相关  
│       ├── picker/图片选择  
│       ├── port/各种接口  
│       ├── service/小程序、通话等服务  
│       ├── ui/除小程序空间和小程序本身的其他UI  
│       ├── usecase/js api、小程序服务事件处理  
│       └── utils/工具类   
├── libs/第三方库文件  
├── script/编译脚本  
├── testing/本地模拟测试相关  
└── webrtcDC/基于SDK实现GSMA ts.66定义的接口，编译出js库供小程序集成使用  

## 五、技术栈
编程语言：Kotlin、java、rust  
架构模式：MVVM  
异步处理：Coroutines + Flow  
数据库：Room  
UI 框架：Jetpack Compose / XML Layouts  

## 六、构建发布
1、适配：  
SDK运行依赖终端的DC、屏幕共享、拓展能力、AR等服务，需终端支持；  

2、打包：  
支持多渠道打包  
./gradlew assembleRelease  

3、发布：  
需手机厂商将SDK作为系统默认应用集成，随系统一起发布。  

## 七、许可证

本项目采用 [Apache2.0](https://www.apache.org/licenses/LICENSE-2.0.txt) 开源协议。


## 八、联系方式
xuq17@chinatelecom.cn


