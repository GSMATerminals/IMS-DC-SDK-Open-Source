LOCAL_PATH := $(call my-dir)

# 预装CtCallSdk到product/priv-app/CtCallSdk/CtCallSdk.apk
include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := CtCallSdk
LOCAL_PACKAGE_NAME    := $(LOCAL_MODULE)
LOCAL_SRC_FILES       := $(LOCAL_MODULE).apk
LOCAL_MODULE_CLASS    := APPS
LOCAL_MODULE_SUFFIX   := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE     := PRESIGNED
LOCAL_PRIVILEGED_MODULE := true
LOCAL_PRODUCT_MODULE := true
include $(BUILD_PREBUILT)

# 定义privileged权限
define DefaultPermissionSetting
$(shell mkdir -p $(PRODUCT_OUT)/product/etc/permissions/; \
            cp -f vendor/ct/CtCallSdk/com.ct.ertclib.dc.xml $(PRODUCT_OUT)/product/etc/permissions/)
endef

$(call DefaultPermissionSetting)
