LOCAL_PATH := $(call my-dir)

ifeq ($(FULL_GMS_VERSION),true)
include $(LOCAL_PATH)/fullgms/Android.mk
endif