package org.cef.callback;

/**
 * Callback interface used for asynchronous continuation of media access
 * permission requests.
 */
public interface CefMediaAccessCallback {
    /**
     * States of a setting.
     */
    final class MediaPermissionFlags {
        public final static int NONE = 0; // No permission.
        public final static int DEVICE_AUDIO_CAPTURE = 1 << 0; // Audio capture permission.
        public final static int DEVICE_VIDEO_CAPTURE = 1 << 1; // Video capture permission.
        public final static int DESKTOP_AUDIO_CAPTURE = 1 << 2; // Desktop audio capture permission.
        public final static int DESKTOP_VIDEO_CAPTURE = 1 << 3; // Desktop video capture permission
    }

    /**
     * Call to allow or deny media access.
     *
     * @param allowed_permissions If this callback was initiated in response to a getUserMedia (indicated by
     * MediaPermissionFlags.DEVICE_AUDIO_CAPTURE and/or MediaPermissionFlags.DEVICE_VIDEO_CAPTURE being set)
     * the allowed_permissions are required to match those given in
     * required_permissions in the OnRequestMediaAccessPermission.
     */
    public void Continue(int allowed_permissions);


    /**
     * Cancel the media access request.
     */
    public void Cancel();
}
