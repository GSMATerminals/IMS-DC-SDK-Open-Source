这模块指定不同类型的插件，自动添加相应的基础配置

现有的插件：

- [`newcall.android.application`](convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt)
  [`newcall.android.library`](convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt)

带compose组件：

- [`newcall.android.application.compose`](convention/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt)
  [`newcall.android.library.compose`](convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt)

使用参考
```kts
plugins {
    id("newcall.android.application")
}
```