// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

#import "util_mac.h"
#import "util.h"

#import <Cocoa/Cocoa.h>
#import <Foundation/NSLock.h>
#import <jni.h>
#include <objc/runtime.h>

#include "include/base/cef_callback.h"
#include "include/cef_app.h"
#include "include/cef_application_mac.h"
#include "include/cef_browser.h"
#include "include/cef_path_util.h"

#include "JCEFThreadUtilities.h"
#include "client_app.h"
#include "client_handler.h"
#include "critical_wait.h"
#include "jni_util.h"
#include "render_handler.h"
#include "temp_window.h"
#include "window_handler.h"

#include <chrono>
#include <thread>

namespace {

static std::set<CefWindowHandle> g_browsers_;
static CriticalLock g_browsers_lock_;
id g_mouse_monitor_ = nil;
static CefRefPtr<ClientApp> g_client_app_ = nullptr;
bool g_handling_send_event = false;
bool g_before_shutdown = false;
bool g_after_shutdown = false;

bool isBrowserExists(CefWindowHandle handle) {
    g_browsers_lock_.Lock();
    const bool result = g_browsers_.count(handle) > 0;
    g_browsers_lock_.Unlock();
    return result;
}

}  // namespace

// Used for passing data to/from ClientHandler initialize:.
@interface InitializeParams : NSObject {
 @public
  std::shared_ptr<CefMainArgs> args_;
  CefSettings settings_;
  CefRefPtr<ClientApp> application_;
  bool result_;
}
@end
@implementation InitializeParams
@end

// Used for passing data to/from ClientHandler setVisiblity:.
@interface SetVisibilityParams : NSObject {
 @public
  CefWindowHandle handle_;
  bool isVisible_;
}
@end
@implementation SetVisibilityParams
@end

// Obj-C Wrapper Class to be called by "performSelectorOnMainThread".
@interface CefHandler : NSObject {
}

+ (void)initialize:(InitializeParams*)params;
+ (void)shutdown;
+ (void)doMessageLoopWork;
+ (void)setVisibility:(SetVisibilityParams*)params;

@end  // interface CefHandler

@interface NSAutoreleasePool (JCEFAutoreleasePool)
- (void)_swizzled_drain;
@end

@implementation NSAutoreleasePool (JCEFAutoreleasePool)

+ (void)load {
    Method originalDrain = class_getInstanceMethod([NSAutoreleasePool class], @selector(drain));
    Method swizzledDrain = class_getInstanceMethod(self, @selector(_swizzled_drain));
    method_exchangeImplementations(originalDrain, swizzledDrain);
}

- (void)_swizzled_drain {
    // do not up-call during a shutdown when on the main thread to avoid crash
    if (!g_before_shutdown || g_after_shutdown || ![NSThread isMainThread]) {
        [self _swizzled_drain];
    }
}

@end

// Java provides an NSApplicationAWT implementation that we can't access or
// override directly. Therefore add the necessary CefAppProtocol
// functionality to NSApplication using categories and swizzling.
@interface NSApplication (JCEFApplication) <CefAppProtocol>

- (BOOL)isHandlingSendEvent;
- (void)setHandlingSendEvent:(BOOL)handlingSendEvent;
- (void)_swizzled_sendEvent:(NSEvent*)event;
- (void)_swizzled_terminate:(id)sender;
- (BOOL)_swizzled_NSMenuItem_accessibilityIsAttributeSettable:(NSAccessibilityAttributeName)attribute;

@end

@implementation NSApplication (JCEFApplication)

// This selector is called very early during the application initialization.
+ (void)load {
  // Swap NSApplication::sendEvent with _swizzled_sendEvent.
  Method original = class_getInstanceMethod(self, @selector(sendEvent:));
  Method swizzled =
      class_getInstanceMethod(self, @selector(_swizzled_sendEvent:));
  method_exchangeImplementations(original, swizzled);

  Method originalTerm = class_getInstanceMethod(self, @selector(terminate:));
  Method swizzledTerm =
      class_getInstanceMethod(self, @selector(_swizzled_terminate:));
  method_exchangeImplementations(originalTerm, swizzledTerm);

  if (!class_getInstanceMethod([NSMenuItem class], @selector(accessibilityIsAttributeSettable:))) {
      Method method_NSMenuItem_accessibilityIsAttributeSettable =
          class_getInstanceMethod(self, @selector(_swizzled_NSMenuItem_accessibilityIsAttributeSettable:));
      class_addMethod(
          [NSMenuItem class],
          @selector(accessibilityIsAttributeSettable:),
          method_getImplementation(method_NSMenuItem_accessibilityIsAttributeSettable),
          method_getTypeEncoding(method_NSMenuItem_accessibilityIsAttributeSettable));
  }
}

+ (void)setMouseMonitor {
  g_mouse_monitor_ = [NSEvent
      addLocalMonitorForEventsMatchingMask:(NSEventMaskLeftMouseDown |
                                            NSEventMaskLeftMouseUp |
                                            NSEventMaskLeftMouseDragged |
                                            NSEventMaskRightMouseDown |
                                            NSEventMaskRightMouseUp |
                                            NSEventMaskRightMouseDragged |
                                            NSEventMaskOtherMouseDown |
                                            NSEventMaskOtherMouseUp |
                                            NSEventMaskOtherMouseDragged |
                                            NSEventMaskScrollWheel |
                                            NSEventMaskMouseMoved |
                                            NSEventMaskMouseEntered |
                                            NSEventMaskMouseExited)
                                   handler:^(NSEvent* evt) {
                                     // Get corresponding CefWindowHandle of
                                     // Java-Canvas
                                     NSView* browser = nullptr;
                                     NSPoint absPos = [evt locationInWindow];
                                     NSWindow* evtWin = [evt window];
                                     g_browsers_lock_.Lock();
                                     std::set<CefWindowHandle> browsers =
                                         g_browsers_;
                                     g_browsers_lock_.Unlock();

                                     std::set<CefWindowHandle>::iterator it;
                                     for (it = browsers.begin();
                                          it != browsers.end(); ++it) {
                                       NSView* wh =
                                           CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(
                                               *it);
                                       NSPoint relPos = [wh convertPoint:absPos
                                                                fromView:nil];
                                       NSRect frame = [wh frame]; // bounds in superview
                                       NSRect bounds = NSMakeRect(0, 0, frame.size.width, frame.size.height);
                                       if (evtWin == [wh window] &&
                                           [wh mouse:relPos
                                               inRect:bounds]) {
                                         browser = wh;
                                         break;
                                       }
                                     }

                                     if (!browser)
                                       return evt;

                                     // Forward mouse event to browsers parent
                                     // (JCEF UI)
                                     switch ([evt type]) {
                                       case NSEventTypeLeftMouseDown:
                                       case NSEventTypeOtherMouseDown:
                                       case NSEventTypeRightMouseDown:
                                         [[browser superview] mouseDown:evt];
                                         return evt;

                                       case NSEventTypeLeftMouseUp:
                                       case NSEventTypeOtherMouseUp:
                                       case NSEventTypeRightMouseUp:
                                         [[browser superview] mouseUp:evt];
                                         return evt;

                                       case NSEventTypeLeftMouseDragged:
                                       case NSEventTypeOtherMouseDragged:
                                       case NSEventTypeRightMouseDragged:
                                         [[browser superview] mouseDragged:evt];
                                         return evt;

                                       case NSEventTypeMouseMoved:
                                         [[browser superview] mouseMoved:evt];
                                         return evt;

                                       case NSEventTypeMouseEntered:
                                         [[browser superview] mouseEntered:evt];
                                         return evt;

                                       case NSEventTypeMouseExited:
                                         [[browser superview] mouseExited:evt];
                                         return evt;

                                       case NSEventTypeScrollWheel:
                                         [[browser superview] scrollWheel:evt];
                                         return evt;

                                       default:
                                         return evt;
                                     }
                                   }];
}

- (BOOL)isHandlingSendEvent {
  return g_handling_send_event;
}

- (void)setHandlingSendEvent:(BOOL)handlingSendEvent {
  g_handling_send_event = handlingSendEvent;
}

- (BOOL)_swizzled_NSMenuItem_accessibilityIsAttributeSettable:(NSAccessibilityAttributeName)attribute {
    return NO;
}

- (void)_swizzled_sendEvent:(NSEvent*)event {
  CefScopedSendingEvent sendingEventScoper;
  // Calls NSApplication::sendEvent due to the swizzling.
  [self _swizzled_sendEvent:event];
}

// This method will be called via Cmd+Q.
- (void)_swizzled_terminate:(id)sender {
  bool continueTerminate = true;

  if (g_client_app_.get()) {
    // Call CefApp.handleBeforeTerminate() in Java. Will result in a call
    // to CefShutdownOnMainThread() via CefApp.shutdown().
    continueTerminate = !g_client_app_->HandleTerminate();
  }

  if (continueTerminate && !g_after_shutdown)
    [[CefHandler class] shutdown];

  // [tav] let NSApplication::terminate proceed
  [self _swizzled_terminate:sender];
}

@end

@implementation CefHandler

// |params| will be released by the caller.
+ (void)initialize:(InitializeParams*)params {
  g_client_app_ = params->application_;
  params->result_ = CefInitialize(*params->args_, params->settings_,
                                  g_client_app_.get(), nullptr);
}

+ (void)shutdown {
  // JBR-5822: to debug intermittent crashes on shutdown use constants from environment
  int workCount = 10;
  const char* sval = getenv("JCEF_SHUTDOWN_WORK_COUNT");
  if (sval != nullptr) {
    workCount = atoi(sval);
    if (workCount < 0) workCount = 0;
    if (workCount > 100) workCount = 100;
    fprintf(stderr, "\tPreform CefDoMessageLoopWork %d times before shutdown\n", (int)workCount);
  }
  int workPause = 0;
  sval = getenv("JCEF_SHUTDOWN_WORK_PAUSE");
  if (sval != nullptr) {
    workPause = atoi(sval);
    if (workPause < 0) workPause = 0;
    if (workPause > 20) workPause = 20;
    fprintf(stderr, "\tUse workPause=%d\n", (int)workPause);
  }

  // Pump CefDoMessageLoopWork a few times before shutting down.
  for (int i = 0; i < workCount; ++i) {
    if (workPause > 0)
      std::this_thread::sleep_for(std::chrono::milliseconds(workPause));
    CefDoMessageLoopWork();
  }

  g_before_shutdown = true;

  CefShutdown();

  g_after_shutdown = true;
  g_client_app_ = nullptr;

  if (g_mouse_monitor_) [NSEvent removeMonitor:g_mouse_monitor_];
}

+ (void)doMessageLoopWork {
  CefDoMessageLoopWork();
}

+ (void)setVisibility:(SetVisibilityParams*)params {
  if (g_client_app_) {
    if (!isBrowserExists(params->handle_)) return;

    NSView* wh = CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(params->handle_);

    bool isHidden = [wh isHidden];
    if (isHidden == params->isVisible_) {
      [wh setHidden:!params->isVisible_];
      [wh needsDisplay];
      [[wh superview] display];
    }
  }
  [params release];
}

@end  // implementation CefHandler

// Instead of adding CefBrowser as child of the windows main view, a content
// view (CefBrowserContentView) is set between the main view and the
// CefBrowser. Why?
//
// The CefBrowserContentView defines the viewable part of the browser view.
// In most cases the content view has the same size as the browser view,
// but for example if you add CefBrowser to a JScrollPane, you only want to
// see the portion of the browser window you're scrolled to. In this case
// the sizes of the content view and the browser view differ.
//
// With other words the CefBrowserContentView clips the CefBrowser to its
// displayable content.
//
// +- - - - - - - - - - - - - - - - - - - - -+
// |/   / CefBrowser/   /   /   /   /   /   /|
//    +-------------------------+  /   /   / <--- invisible part of CefBrowser
// |  | CefBrowserContentView   | /   /   /  |
//   /|                         |/   /   /
// |/ |                         |   /   /   /|
//    |                       <------------------ visible part of CefBrowser
// |  |                         | /   /   /  |
//   /|                         |/   /   /
// |/ |                         |   /   /   /|
//    |                         |  /   /   /
// |  +-------------------------+ /   /   /  |
//   /   /   /   /   /   /   /   /   /   /
// |/   /   /   /   /   /   /   /   /   /   /|
//     /   /   /   /   /   /   /   /   /   /
// |  /   /   /   /   /   /   /   /   /   /  |
// +- - - - - - - - - - - - - - - - - - - - -+
@interface CefBrowserContentView : NSView {
  CefRefPtr<CefBrowser> cefBrowser;
}

@property(readonly) BOOL isLiveResizing;

- (void)addCefBrowser:(CefRefPtr<CefBrowser>)browser;
- (void)destroyCefBrowser;
- (void)updateView:(NSDictionary*)dict;
@end  // interface CefBrowserContentView

@implementation CefBrowserContentView

@synthesize isLiveResizing;

- (id)initWithFrame:(NSRect)frameRect {
  self = [super initWithFrame:frameRect];
  cefBrowser = nullptr;
  return self;
}

- (void)dealloc {
  if (cefBrowser) {
    util::DestroyCefBrowser(cefBrowser);
  }
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  cefBrowser = nullptr;
  [super dealloc];
}

- (void)setFrame:(NSRect)frameRect {
  // Instead of using the passed frame, get the visible rect from java because
  // the passed frame rect doesn't contain the clipped view part. Otherwise
  // we'll overlay some parts of the Java UI.
  if (cefBrowser.get()) {
    CefRefPtr<ClientHandler> clientHandler =
        (ClientHandler*)(cefBrowser->GetHost()->GetClient().get());

    CefRefPtr<WindowHandler> windowHandler = clientHandler->GetWindowHandler();
    if (windowHandler.get() != nullptr) {
      CefRect rect;
      windowHandler->GetRect(cefBrowser, rect);
      util_mac::TranslateRect(self, rect);
      frameRect = (NSRect){{rect.x, rect.y}, {rect.width, rect.height}};
    }
  }
  [super setFrame:frameRect];
}

- (void)addCefBrowser:(CefRefPtr<CefBrowser>)browser {
  cefBrowser = browser;
  // Register for the start and end events of "liveResize" to avoid
  // Java paint updates while someone is resizing the main window (e.g. by
  // pulling with the mouse cursor)
  [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(windowWillStartLiveResize:)
             name:NSWindowWillStartLiveResizeNotification
           object:[self window]];
  [[NSNotificationCenter defaultCenter]
      addObserver:self
         selector:@selector(windowDidEndLiveResize:)
             name:NSWindowDidEndLiveResizeNotification
           object:[self window]];
}

- (void)destroyCefBrowser {
  [[NSNotificationCenter defaultCenter] removeObserver:self];
  cefBrowser = nullptr;
  [self removeFromSuperview];
  // Also remove all subviews so the CEF objects are released.
  for (NSView* view in [self subviews]) {
    [view removeFromSuperview];
  }
}

- (void)windowWillStartLiveResize:(NSNotification*)notification {
  isLiveResizing = YES;
}

- (void)windowDidEndLiveResize:(NSNotification*)notification {
  isLiveResizing = NO;
  [self setFrame:[self frame]];
}

- (void)updateView:(NSDictionary*)dict {
  NSRect contentRect = NSRectFromString([dict valueForKey:@"content"]);
  NSRect browserRect = NSRectFromString([dict valueForKey:@"browser"]);

  NSArray* childs = [self subviews];
  for (NSView* child in childs) {
    [child setFrame:browserRect];
    [child setNeedsDisplay:YES];
  }
  [super setFrame:contentRect];
  [self setNeedsDisplay:YES];
}

@end  // implementation CefBrowserContentView

namespace util_mac {

std::string GetAbsPath(const std::string& path) {
  char full_path[PATH_MAX];
  if (realpath(path.c_str(), full_path) == nullptr)
    return std::string();
  return full_path;
}

bool IsNSView(void* ptr) {
  id obj = (id)ptr;
  bool result = [obj isKindOfClass:[NSView class]];
  if (!result)
    NSLog(@"Expected NSView, found %@", NSStringFromClass([obj class]));
  return result;
}

void* GetNSView(void* nsWindow) {
  if (![(id)nsWindow isKindOfClass:[NSWindow class]]) {
    NSLog(@"Expected NSWindow, found %@",
          NSStringFromClass([(id)nsWindow class]));
    return nullptr;
  }

  return [(NSWindow*)nsWindow contentView];
}

CefWindowHandle CreateBrowserContentView(NSWindow* window, CefRect& orig) {
  NSView* mainView = CAST_CEF_WINDOW_HANDLE_TO_NSVIEW([window contentView]);
  TranslateRect(mainView, orig);
  NSRect frame = {{orig.x, orig.y}, {orig.width, orig.height}};

  CefBrowserContentView* contentView =
      [[CefBrowserContentView alloc] initWithFrame:frame];

  // Make the content view for the window have a layer. This will make all
  // sub-views have layers. This is necessary to ensure correct layer
  // ordering of all child views and their layers.
  [contentView setWantsLayer:YES];

  [mainView addSubview:contentView];
  [contentView setAutoresizingMask:(NSViewWidthSizable | NSViewHeightSizable)];
  [contentView setNeedsDisplay:YES];

  [contentView release];

  // Override origin before "orig" is returned because the new origin is
  // relative to the created CefBrowserContentView object
  orig.x = 0;
  orig.y = 0;
  return contentView;
}

// translate java's window origin to Obj-C's window origin
void TranslateRect(CefWindowHandle view, CefRect& orig) {
  NSRect bounds =
      [[[CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(view) window] contentView] bounds];
  orig.y = bounds.size.height - orig.height - orig.y;
}

bool CefInitializeOnMainThread(const CefMainArgs& args,
                               const CefSettings& settings,
                               CefRefPtr<ClientApp> application) {
  InitializeParams* params = [[InitializeParams alloc] init];
  params->args_ = std::make_shared<CefMainArgs>(args);
  params->settings_ = settings;
  params->application_ = application;
  params->result_ = false;

  // Block until done.
  [JCEFThreadUtilities performOnMainThread:@selector(initialize:) on:[CefHandler class] withObject:params waitUntilDone:YES];

  int result = params->result_;
  [params release];
  return result;
}

void CefShutdownOnMainThread() {
  // Block until done.
  [[CefHandler class] performSelectorOnMainThread:@selector(shutdown)
                                       withObject:nil
                                    waitUntilDone:NO];
}

void CefDoMessageLoopWorkOnMainThread() {
  [[CefHandler class] performSelectorOnMainThread:@selector(doMessageLoopWork)
                                       withObject:nil
                                    waitUntilDone:NO];
}

void SetVisibility(CefWindowHandle handle, bool isVisible) {
  SetVisibilityParams* params = [[SetVisibilityParams alloc] init];
  params->handle_ = handle;
  params->isVisible_ = isVisible;
  [[CefHandler class] performSelectorOnMainThread:@selector(setVisibility:)
                                       withObject:params
                                    waitUntilDone:NO];
}

void UpdateView(CefWindowHandle handle,
                CefRect contentRect,
                CefRect browserRect) {
  if (!isBrowserExists(handle)) return;

  util_mac::TranslateRect(handle, contentRect);
  CefBrowserContentView* browser =
      (CefBrowserContentView*)[CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(handle)
          superview];
  browserRect.y = contentRect.height - browserRect.height - browserRect.y;

  // Only update the view if nobody is currently resizing the main window.
  // Otherwise the CefBrowser part may start flickering because there's a
  // significant delay between the native window resize event and the java
  // resize event
  if (![browser isLiveResizing]) {
    NSString* contentStr = [[NSString alloc]
        initWithFormat:@"{{%d,%d},{%d,%d}", contentRect.x, contentRect.y,
                       contentRect.width, contentRect.height];
    NSString* browserStr = [[NSString alloc]
        initWithFormat:@"{{%d,%d},{%d,%d}", browserRect.x, browserRect.y,
                       browserRect.width, browserRect.height];
    NSDictionary* dict = [[NSDictionary alloc]
        initWithObjectsAndKeys:contentStr, @"content", browserStr, @"browser",
                               nil];
    [browser performSelectorOnMainThread:@selector(updateView:)
                              withObject:dict
                           waitUntilDone:NO];
  }
}

}  // namespace util_mac

namespace util {

void AddCefBrowser(CefRefPtr<CefBrowser> browser) {
  if (!browser.get() || browser->GetHost()->IsWindowRenderingDisabled())
    return;
  CefWindowHandle handle = browser->GetHost()->GetWindowHandle();
  g_browsers_lock_.Lock();
  g_browsers_.insert(handle);
  g_browsers_lock_.Unlock();
  CefBrowserContentView* browserImpl =
      (CefBrowserContentView*)[CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(handle)
          superview];
  [browserImpl addCefBrowser:browser];
  if (!g_mouse_monitor_) {
      [NSApplication setMouseMonitor];
  }
}

void DestroyCefBrowser(CefRefPtr<CefBrowser> browser) {
  if (!browser.get() || browser->GetHost()->IsWindowRenderingDisabled())
    return;
  NSView* handle =
      CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(browser->GetHost()->GetWindowHandle());
  g_browsers_lock_.Lock();
  bool browser_exists = g_browsers_.erase(handle) > 0;
  g_browsers_lock_.Unlock();
  if (!browser_exists)
    return;

  // There are some cases where the superview of CefBrowser isn't
  // a CefBrowserContentView. For example if another CefBrowser window was
  // created by calling "window.open()" in JavaScript.
  NSView* superView = [handle superview];
  if ([superView isKindOfClass:[CefBrowserContentView class]]) {
    [(CefBrowserContentView*)superView destroyCefBrowser];
  }
}

void SetParent(CefWindowHandle handle,
               jlong parentHandle,
               base::OnceClosure callback) {
  base::RepeatingClosure* pCallback = new base::RepeatingClosure(
      base::BindRepeating([](base::OnceClosure& cb) { std::move(cb).Run(); },
                          OwnedRef(std::move(callback))));
  dispatch_async(dispatch_get_main_queue(), ^{
    if (!isBrowserExists(handle)) return;

      CefBrowserContentView* browser_view =
        (CefBrowserContentView*)[CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(handle)
            superview];
    [browser_view retain];
    [browser_view removeFromSuperview];

    NSView* contentView;
    if (parentHandle) {
      NSWindow* window = (NSWindow*)parentHandle;
      contentView = [window contentView];
    } else {
      contentView =
          CAST_CEF_WINDOW_HANDLE_TO_NSVIEW(TempWindow::GetWindowHandle());
    }
    [contentView addSubview:browser_view];
    [browser_view release];
    pCallback->Run();
    delete pCallback;
  });
}

}  // namespace util
