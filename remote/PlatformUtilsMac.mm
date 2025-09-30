#include "PlatformUtils.h"

#include <jni.h>

#import <IOSurface/IOSurface.h>
#import <Metal/Metal.h>

#include <atomic>

namespace {

id<MTLDevice> GetMetalDevice() {
  static std::atomic<bool> invalided{false};
  static id<MTLDevice> device = [&]() {
    [[NSNotificationCenter defaultCenter]
        addObserverForName:MTLDeviceWasRemovedNotification
                    object:nil
                     queue:[NSOperationQueue mainQueue]
                usingBlock:^(NSNotification* note) {
                  invalided.store(true, std::memory_order_release);
                }];
    return MTLCreateSystemDefaultDevice();
  }();

  if (invalided.exchange(false, std::memory_order_acq_rel)) {
    device = MTLCreateSystemDefaultDevice();
  }

  return device;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_getMtlTexture(JNIEnv* env,
                                                          jclass,
                                                          jlong pSurfaceRef) {
  if (!pSurfaceRef) {
    return 0;
  }

  auto surface = (IOSurfaceRef)(uintptr_t)(pSurfaceRef);
  MTLTextureDescriptor* textureDescriptor = [[MTLTextureDescriptor alloc] init];
  textureDescriptor.width = IOSurfaceGetWidth(surface);
  textureDescriptor.height = IOSurfaceGetHeight(surface);
  textureDescriptor.pixelFormat = MTLPixelFormatBGRA8Unorm;
  textureDescriptor.textureType = MTLTextureType2D;
  textureDescriptor.usage = MTLTextureUsageShaderRead;
  textureDescriptor.storageMode = MTLStorageModeShared;
  id<MTLTexture> texture =
      [GetMetalDevice() newTextureWithDescriptor:textureDescriptor
                                       iosurface:surface
                                           plane:0];
  [textureDescriptor release];

  return (jlong)texture;
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_releaseMtlTextureRef(
    JNIEnv* env,
    jclass jclass,
    jlong pMtlTexture) {
  if (!pMtlTexture)
    return;

  auto texture = (id<MTLTexture>)(void*)pMtlTexture;
  [texture release];
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_retainIOSurfaceRef(
    JNIEnv* env,
    jclass,
    jlong pIOSurfaceRef) {
  if (!pIOSurfaceRef)
    return;
  auto surface = (IOSurfaceRef)(uintptr_t)pIOSurfaceRef;
  CFRetain(surface);
}

JNIEXPORT void JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_releaseIOSurfaceRef(
    JNIEnv* env,
    jclass,
    jlong pIOSurfaceRef) {
  if (!pIOSurfaceRef)
    return;
  auto surface = (IOSurfaceRef)(uintptr_t)pIOSurfaceRef;
  CFRelease(surface);
}

JNIEXPORT jlong JNICALL
Java_com_jetbrains_cef_remote_PlatformUtils_getIOSurfaceRef(JNIEnv*,
                                                            jclass,
                                                            jlong ioSurfaceId) {
  if (!ioSurfaceId) {
    return 0;
  }

  IOSurfaceRef surface = IOSurfaceLookup(ioSurfaceId);
  return (jlong)(surface);
}
}  // extern "C"
