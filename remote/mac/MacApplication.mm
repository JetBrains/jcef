#import <Cocoa/Cocoa.h>
#import <objc/runtime.h>

#include "include/cef_application_mac.h"

// Provide the CefAppProtocol implementation required by CEF.
@interface MacApplication : NSApplication <CefAppProtocol> {
 @private
  BOOL handlingSendEvent_;
}
@end

@implementation MacApplication
- (BOOL)isHandlingSendEvent {
  return handlingSendEvent_;
}

- (void)setHandlingSendEvent:(BOOL)handlingSendEvent {
  handlingSendEvent_ = handlingSendEvent;
}

- (void)sendEvent:(NSEvent*)event {
  CefScopedSendingEvent sendingEventScoper;
  [super sendEvent:event];
}
@end

namespace {

id swizzledDefaultUserNotificationCenter(id self, SEL _cmd) {
  NSLog(@"Swizzled NSUserNotificationCenter: blocking defaultUserNotificationCenter");
  return nil;
}

void installLegacyNotificationCenterBlocker() {
  static dispatch_once_t onceToken;
  dispatch_once(&onceToken, ^{
    Class cls = NSClassFromString(@"NSUserNotificationCenter");
    if (!cls) {
      return;
    }

    SEL sel = @selector(defaultUserNotificationCenter);
    Method m = class_getClassMethod(cls, sel);
    if (!m) {
      return;
    }

    IMP newImp = (IMP)swizzledDefaultUserNotificationCenter;
    method_setImplementation(m, newImp);
  });
}

}  // namespace

void initMacApplication() {
  @autoreleasepool {
    installLegacyNotificationCenterBlocker();

    // Initialize the SimpleApplication instance.
    [MacApplication sharedApplication];

    // If there was an invocation to NSApp prior to this method, then the NSApp
    // will not be a MacApplication, but will instead be an NSApplication.
    // This is undesirable and we must enforce that this doesn't happen.
    CHECK([NSApp isKindOfClass:[MacApplication class]]);
  }
}