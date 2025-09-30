package org.cef;

import com.jetbrains.cef.remote.PlatformUtils;

public class Utils {
    static public long getMtlTexture(long pSurfaceRef) {
        return PlatformUtils.getMtlTexture(pSurfaceRef);
    }
    static public void releaseMtlTextureRef(long pMtlTexture) {
        PlatformUtils.releaseMtlTextureRef(pMtlTexture);
    }

    static public void retainIOSurfaceRef(long pSurfaceRef) {
        PlatformUtils.retainIOSurfaceRef(pSurfaceRef);
    }

    static public void releaseIOSurfaceRef(long pSurfaceRef) {
        PlatformUtils.releaseIOSurfaceRef(pSurfaceRef);
    }

    static public long getIOSurfaceRef(long surfaceId) {
        return PlatformUtils.getIOSurfaceRef(surfaceId);
    }
}
