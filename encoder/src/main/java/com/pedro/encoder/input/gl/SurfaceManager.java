package com.pedro.encoder.input.gl;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;
import com.pedro.encoder.utils.gl.GlUtil;

/**
 * Created by pedro on 9/09/17.
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SurfaceManager {

  private static final int EGL_RECORDABLE_ANDROID = 0x3142;

  private EGLContext eglContext = null;
  private EGLSurface eglSurface = null;
  private EGLDisplay eglDisplay = null;

  /**
   * Creates an EGL context and an EGL surface.
   */
  public SurfaceManager(Surface surface, SurfaceManager manager) {
    eglSetup(surface, manager.eglContext);
  }

  /**
   * Creates an EGL context and an EGL surface.
   */
  public SurfaceManager(Surface surface, EGLContext eglContext) {
    eglSetup(surface, eglContext);
  }

  /**
   * Creates an EGL context and an EGL surface.
   */
  public SurfaceManager(Surface surface) {
    eglSetup(surface, null);
  }

  public SurfaceManager() {
    eglSetup(null, null);
  }

  public void makeCurrent() {
    if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
      Log.e("Error", "eglMakeCurrent failed");
    }
  }

  public void swapBuffer() {
    EGL14.eglSwapBuffers(eglDisplay, eglSurface);
  }

  /**
   * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
   */
  public void setPresentationTime(long nsecs) {
    EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nsecs);
    GlUtil.checkEglError("eglPresentationTimeANDROID");
  }

  /**
   * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
   */
  private void eglSetup(Surface surface, EGLContext eglSharedContext) {
    eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
      throw new RuntimeException("unable to get EGL14 display");
    }
    int[] version = new int[2];
    if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
      throw new RuntimeException("unable to initialize EGL14");
    }

    // Configure EGL for recording and OpenGL ES 2.0.
    int[] attribList;
    if (eglSharedContext == null) {
      attribList = new int[] {
          EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
          EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
          /* AA https://stackoverflow.com/questions/27035893/antialiasing-in-opengl-es-2-0 */
          //EGL14.EGL_SAMPLE_BUFFERS, 1 /* true */,
          //EGL14.EGL_SAMPLES, 4, /* increase to more smooth limit of your GPU */
          EGL14.EGL_NONE
      };
    } else {
      attribList = new int[] {
          EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
          EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1,
          /* AA https://stackoverflow.com/questions/27035893/antialiasing-in-opengl-es-2-0 */
          //EGL14.EGL_SAMPLE_BUFFERS, 1 /* true */,
          //EGL14.EGL_SAMPLES, 4, /* increase to more smooth limit of your GPU */
          EGL14.EGL_NONE
      };
    }
    EGLConfig[] configs = new EGLConfig[1];
    int[] numConfigs = new int[1];
    EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0);
    GlUtil.checkEglError("eglCreateContext RGB888+recordable ES2");

    // Configure context for OpenGL ES 2.0.
    int[] attrib_list = {
        EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE
    };

    eglContext = EGL14.eglCreateContext(eglDisplay, configs[0],
        eglSharedContext == null ? EGL14.EGL_NO_CONTEXT : eglSharedContext, attrib_list, 0);
    GlUtil.checkEglError("eglCreateContext");

    // Create a window surface, and attach it to the Surface we received.
    if (surface == null) {
      int[] surfaceAttribs = {
          EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE
      };
      eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0);
    } else {
      int[] surfaceAttribs = {
          EGL14.EGL_NONE
      };
      eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0);
    }
    GlUtil.checkEglError("eglCreateWindowSurface");
  }

  /**
   * Discards all resources held by this class, notably the EGL context.
   */
  public void release(boolean resetContext) {
    if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
      EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
          EGL14.EGL_NO_CONTEXT);
      EGL14.eglDestroySurface(eglDisplay, eglSurface);
      if (resetContext) EGL14.eglDestroyContext(eglDisplay, eglContext);
      EGL14.eglReleaseThread();
      EGL14.eglTerminate(eglDisplay);
    }
    eglDisplay = EGL14.EGL_NO_DISPLAY;
    eglContext = EGL14.EGL_NO_CONTEXT;
    eglSurface = EGL14.EGL_NO_SURFACE;
  }

  public EGLContext getEglContext() {
    return eglContext;
  }

  public EGLSurface getEglSurface() {
    return eglSurface;
  }

  public EGLDisplay getEglDisplay() {
    return eglDisplay;
  }
}
