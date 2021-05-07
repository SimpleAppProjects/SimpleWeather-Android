LOCAL_PATH := $(call my-dir)

ifneq ($(FULL_GMS_VERSION),)
include $(LOCAL_PATH)/fullgms/Android.mk
endif