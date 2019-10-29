/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class org_cef_browser_CefBrowser_N */

#ifndef _Included_org_cef_browser_CefBrowser_N
#define _Included_org_cef_browser_CefBrowser_N
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_CreateBrowser
 * Signature:
 * (Lorg/cef/handler/CefClientHandler;JLjava/lang/String;ZZLjava/awt/Component;Lorg/cef/browser/CefRequestContext;)Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CreateBrowser(JNIEnv*,
                                                    jobject,
                                                    jobject,
                                                    jlong,
                                                    jstring,
                                                    jboolean,
                                                    jboolean,
                                                    jobject,
                                                    jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_CreateDevTools
 * Signature:
 * (Lorg/cef/browser/CefBrowser;Lorg/cef/handler/CefClientHandler;JZZLjava/awt/Component;Ljava/awt/Point;)Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CreateDevTools(JNIEnv*,
                                                     jobject,
                                                     jobject,
                                                     jobject,
                                                     jlong,
                                                     jboolean,
                                                     jboolean,
                                                     jobject,
                                                     jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetWindowHandle
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetWindowHandle(JNIEnv*, jobject, jlong);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_CanGoBack
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CanGoBack(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GoBack
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1GoBack(JNIEnv*,
                                                                    jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_CanGoForward
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CanGoForward(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GoForward
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1GoForward(JNIEnv*,
                                                                       jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_IsLoading
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1IsLoading(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_Reload
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1Reload(JNIEnv*,
                                                                    jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_ReloadIgnoreCache
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ReloadIgnoreCache(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_StopLoad
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1StopLoad(JNIEnv*,
                                                                      jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetIdentifier
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetIdentifier(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetMainFrame
 * Signature: ()Lorg/cef/browser/CefFrame;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetMainFrame(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFocusedFrame
 * Signature: ()Lorg/cef/browser/CefFrame;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFocusedFrame(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFrame
 * Signature: (J)Lorg/cef/browser/CefFrame;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrame(JNIEnv*, jobject, jlong);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFrame2
 * Signature: (Ljava/lang/String;)Lorg/cef/browser/CefFrame;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrame2(JNIEnv*, jobject, jstring);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFrameIdentifiers
 * Signature: ()Ljava/util/Vector;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameIdentifiers(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFrameNames
 * Signature: ()Ljava/util/Vector;
 */
JNIEXPORT jobject JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameNames(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetFrameCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetFrameCount(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_IsPopup
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1IsPopup(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_HasDocument
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1HasDocument(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_ViewSource
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ViewSource(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetSource
 * Signature: (Lorg/cef/callback/CefStringVisitor;)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1GetSource(JNIEnv*,
                                                                       jobject,
                                                                       jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetText
 * Signature: (Lorg/cef/callback/CefStringVisitor;)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1GetText(JNIEnv*,
                                                                     jobject,
                                                                     jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_LoadRequest
 * Signature: (Lorg/cef/network/CefRequest;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1LoadRequest(JNIEnv*, jobject, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_LoadURL
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1LoadURL(JNIEnv*,
                                                                     jobject,
                                                                     jstring);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_ExecuteJavaScript
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ExecuteJavaScript(JNIEnv*,
                                                        jobject,
                                                        jstring,
                                                        jstring,
                                                        jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetURL
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_cef_browser_CefBrowser_1N_N_1GetURL(JNIEnv*,
                                                                       jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_Close
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1Close(JNIEnv*,
                                                                   jobject,
                                                                   jboolean);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SetFocus
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1SetFocus(JNIEnv*,
                                                                      jobject,
                                                                      jboolean);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SetWindowVisibility
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetWindowVisibility(JNIEnv*,
                                                          jobject,
                                                          jboolean);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_GetZoomLevel
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1GetZoomLevel(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SetZoomLevel
 * Signature: (D)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SetZoomLevel(JNIEnv*, jobject, jdouble);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_RunFileDialog
 * Signature:
 * (Lorg/cef/handler/CefDialogHandler/FileDialogMode;Ljava/lang/String;Ljava/lang/String;Ljava/util/Vector;ILorg/cef/callback/CefRunFileDialogCallback;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1RunFileDialog(JNIEnv*,
                                                    jobject,
                                                    jobject,
                                                    jstring,
                                                    jstring,
                                                    jobject,
                                                    jint,
                                                    jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_StartDownload
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1StartDownload(JNIEnv*, jobject, jstring);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_Print
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1Print(JNIEnv*,
                                                                   jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_PrintToPDF
 * Signature:
 * (Ljava/lang/String;Lorg/cef/misc/CefPdfPrintSettings;Lorg/cef/callback/CefPdfPrintCallback;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1PrintToPDF(JNIEnv*,
                                                 jobject,
                                                 jstring,
                                                 jobject,
                                                 jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_Find
 * Signature: (ILjava/lang/String;ZZZ)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1Find(JNIEnv*,
                                                                  jobject,
                                                                  jint,
                                                                  jstring,
                                                                  jboolean,
                                                                  jboolean,
                                                                  jboolean);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_StopFinding
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1StopFinding(JNIEnv*, jobject, jboolean);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_CloseDevTools
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1CloseDevTools(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_ReplaceMisspelling
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1ReplaceMisspelling(JNIEnv*,
                                                         jobject,
                                                         jstring);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_WasResized
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1WasResized(JNIEnv*,
                                                                        jobject,
                                                                        jint,
                                                                        jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_Invalidate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1Invalidate(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SendKeyEvent
 * Signature: (Ljava/awt/event/KeyEvent;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendKeyEvent(JNIEnv*, jobject, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SendMouseEvent
 * Signature: (Ljava/awt/event/MouseEvent;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendMouseEvent(JNIEnv*, jobject, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SendMouseWheelEvent
 * Signature: (Ljava/awt/event/MouseWheelEvent;)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1SendMouseWheelEvent(JNIEnv*,
                                                          jobject,
                                                          jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragTargetDragEnter
 * Signature: (Lorg/cef/callback/CefDragData;Ljava/awt/Point;II)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragEnter(JNIEnv*,
                                                          jobject,
                                                          jobject,
                                                          jobject,
                                                          jint,
                                                          jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragTargetDragOver
 * Signature: (Ljava/awt/Point;II)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragOver(JNIEnv*,
                                                         jobject,
                                                         jobject,
                                                         jint,
                                                         jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragTargetDragLeave
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDragLeave(JNIEnv*, jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragTargetDrop
 * Signature: (Ljava/awt/Point;I)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragTargetDrop(JNIEnv*,
                                                     jobject,
                                                     jobject,
                                                     jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragSourceEndedAt
 * Signature: (Ljava/awt/Point;I)V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragSourceEndedAt(JNIEnv*,
                                                        jobject,
                                                        jobject,
                                                        jint);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_DragSourceSystemDragEnded
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1DragSourceSystemDragEnded(JNIEnv*,
                                                                jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_UpdateUI
 * Signature: (Ljava/awt/Rectangle;Ljava/awt/Rectangle;)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1UpdateUI(JNIEnv*,
                                                                      jobject,
                                                                      jobject,
                                                                      jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_SetParent
 * Signature: (JLjava/awt/Component;)V
 */
JNIEXPORT void JNICALL Java_org_cef_browser_CefBrowser_1N_N_1SetParent(JNIEnv*,
                                                                       jobject,
                                                                       jlong,
                                                                       jobject);

/*
 * Class:     org_cef_browser_CefBrowser_N
 * Method:    N_NotifyMoveOrResizeStarted
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_org_cef_browser_CefBrowser_1N_N_1NotifyMoveOrResizeStarted(JNIEnv*,
                                                                jobject);

#ifdef __cplusplus
}
#endif
#endif
