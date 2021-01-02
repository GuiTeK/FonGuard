/*
 * FonGuard
 * Copyright (C) 2021  Guillaume TRUCHOT <guillaume.truchot@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.fonguard.guardservice.triggers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicResize;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fonguard.MainActivity;
import com.fonguard.Preferences;

import com.fonguard.ScriptC_grayscale;
import com.fonguard.guardservice.GuardService;
import com.fonguard.guardservice.rules.RulesManager;
import com.fonguard.ui.triggers.motion.TriggersMotionFragment;
import com.fonguard.utils.camera.Camera2Wrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MotionTrigger extends Handler implements Allocation.OnBufferAvailableListener,
        Camera2Wrapper.CameraClosedListener {
    private static final String LOG_TAG = MotionTrigger.class.getName();

    public static final int PIXEL_VALUE_DIFF_THRESHOLD_MIN = 1;
    public static final int PIXEL_VALUE_DIFF_THRESHOLD_MAX = 255;
    public static final int PIXEL_NUMBER_DIFF_THRESHOLD_MIN = 1;
    // Even with a high resolution camera, it is unlikely that one needs a diff of more than 100,000
    public static final int PIXEL_NUMBER_DIFF_THRESHOLD_MAX = 100000;

    public static final int HANDLER_MSG_RESTART = 1;
    public static final int HANDLER_MSG_SET_PREVIEW_SURFACE = 2;
    public static final int HANDLER_MSG_SET_PIXEL_VALUE_DIFF_THRESHOLD = 3;
    public static final int HANDLER_MSG_SET_PIXEL_NUMBER_DIFF_THRESHOLD = 4;

    public static final String INTENT_EXTRA_PREVIEW_SURFACE = "PreviewSurface";
    public static final String INTENT_EXTRA_PIXEL_VALUE_DIFF_THRESHOLD = "PixelValueDiffThreshold";
    public static final String INTENT_EXTRA_PIXEL_NUMBER_DIFF_THRESHOLD =
            "PixelNumberDiffThreshold";


    private final GuardService mGuardService;
    private final Preferences mPreferences;
    private final RulesManager mRulesManager;
    private final RenderScript mRenderScript;
    private final Camera2Wrapper mCameraWrapper;
    private boolean mAutoRestartWhenCameraClosed = false;
    private boolean mIsStoppingCapture = false;
    private int mPixelValueDiffThreshold;
    private int mPixelNumberDiffThreshold;
    private int mCapturePreviewWidth;
    private int mCapturePreviewHeight;
    private int mCaptureProcessingWidth;
    private int mCaptureProcessingHeight;
    private Surface mCapturePreviewSurface;
    private Allocation mCaptureYuvAllocation;
    private Allocation mCaptureRgbAllocation;
    private Allocation mCaptureRgbResizedAllocation;
    private Allocation mCaptureGrayscaleResizedAllocation;
    private Allocation mCaptureGrayscaleResizedBlurredAllocation;
    private ScriptIntrinsicYuvToRGB mYuvToRgbIntrinsicScript;
    private ScriptIntrinsicResize mResizeIntrinsicScript;
    private ScriptC_grayscale mGrayscaleScript;
    private ScriptIntrinsicBlur mBlurIntrinsicScript;
    private Bitmap mCapturePreviewBitmap;
    private Bitmap mCaptureProcessingBitmap;
    private int[] mCaptureProcessingBitmapPixels;
    private int[] mReferenceBitmapPixels;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PREVIEW_SURFACE: {
                    Message msg = MotionTrigger.this.obtainMessage(HANDLER_MSG_SET_PREVIEW_SURFACE);
                    msg.obj = intent.getExtras().getParcelable(INTENT_EXTRA_PREVIEW_SURFACE);
                    MotionTrigger.this.sendMessage(msg);
                    break;
                }
                case MainActivity.INTENT_ACTION_MOTION_TRIGGER_RESTART: {
                    Message msg = MotionTrigger.this.obtainMessage(HANDLER_MSG_RESTART);
                    MotionTrigger.this.sendMessage(msg);
                    break;
                }
                case MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_VALUE_DIFF_THRESHOLD: {
                    Message msg = MotionTrigger.this.obtainMessage(
                            HANDLER_MSG_SET_PIXEL_VALUE_DIFF_THRESHOLD);
                    msg.obj = intent.getExtras().getInt(INTENT_EXTRA_PIXEL_VALUE_DIFF_THRESHOLD);
                    MotionTrigger.this.sendMessage(msg);
                    break;
                }
                case MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_NUMBER_DIFF_THRESHOLD: {
                    Message msg = MotionTrigger.this.obtainMessage(
                            HANDLER_MSG_SET_PIXEL_NUMBER_DIFF_THRESHOLD);
                    msg.obj = intent.getExtras().getInt(INTENT_EXTRA_PIXEL_NUMBER_DIFF_THRESHOLD);
                    MotionTrigger.this.sendMessage(msg);
                    break;
                }
                default:
                    Log.w(LOG_TAG, "received unknown intent action " + intent.getAction());
                    break;
            }
        }
    };

    public MotionTrigger(Looper looper, GuardService guardService) {
        super(looper);
        IntentFilter intentFilter = new IntentFilter();

        mGuardService = guardService;
        mPreferences = Preferences.getInstance(mGuardService);
        mRulesManager = RulesManager.getInstance(mPreferences);
        mPixelValueDiffThreshold = mPreferences.getPixelValueDiffThreshold();
        mPixelNumberDiffThreshold = mPreferences.getPixelNumberDiffThreshold();
        mRenderScript = RenderScript.create(mGuardService);
        mCameraWrapper = new Camera2Wrapper(mGuardService);

        mCameraWrapper.setCameraClosedListener(this);

        intentFilter.addAction(MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PREVIEW_SURFACE);
        intentFilter.addAction(MainActivity.INTENT_ACTION_MOTION_TRIGGER_RESTART);
        intentFilter.addAction(
                MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_VALUE_DIFF_THRESHOLD);
        intentFilter.addAction(
                MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_NUMBER_DIFF_THRESHOLD);
        LocalBroadcastManager.getInstance(mGuardService).registerReceiver(mBroadcastReceiver,
                intentFilter);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case HANDLER_MSG_RESTART:
                if (mCameraWrapper.isCapturing()) {
                    mAutoRestartWhenCameraClosed = true; // Need to wait for the current CameraCaptureSession to finish before starting a new one
                    stopCapture();
                } else {
                    mAutoRestartWhenCameraClosed = false;
                    startCapture();
                }
                break;
            case HANDLER_MSG_SET_PREVIEW_SURFACE:
                mCapturePreviewSurface = (Surface)msg.obj;
                break;
            case HANDLER_MSG_SET_PIXEL_VALUE_DIFF_THRESHOLD:
                mPixelValueDiffThreshold = (int)msg.obj;
                break;
            case HANDLER_MSG_SET_PIXEL_NUMBER_DIFF_THRESHOLD:
                mPixelNumberDiffThreshold = (int)msg.obj;
                break;
            default:
                Log.w(LOG_TAG, "unhandled MotionTrigger message \"" + msg.what + "\"");
                break;
        }
    }

    @Override
    public void onBufferAvailable(Allocation captureInYuvAllocation) {
        captureInYuvAllocation.ioReceive();

        if (mIsStoppingCapture) {
            return;
        }

        mYuvToRgbIntrinsicScript.setInput(captureInYuvAllocation);
        mYuvToRgbIntrinsicScript.forEach(mCaptureRgbAllocation);

        mCaptureRgbAllocation.copyTo(mCapturePreviewBitmap);

        mResizeIntrinsicScript.setInput(mCaptureRgbAllocation);
        mResizeIntrinsicScript.forEach_bicubic(mCaptureRgbResizedAllocation);

        mGrayscaleScript.forEach_toGrayscale(mCaptureRgbResizedAllocation,
                mCaptureGrayscaleResizedAllocation);

        mBlurIntrinsicScript.setInput(mCaptureGrayscaleResizedAllocation);
        mBlurIntrinsicScript.forEach(mCaptureGrayscaleResizedBlurredAllocation);

        mCaptureGrayscaleResizedBlurredAllocation.copyTo(mCaptureProcessingBitmap);

        int diffPixelsNb = computeGrayscaleDiffPixels();
        Log.v(LOG_TAG, "Different pixels nb: " + diffPixelsNb);

        if (diffPixelsNb >= mPixelNumberDiffThreshold) {
            Log.v(LOG_TAG, "MOTION DETECTED");
            mRulesManager.performActionsAsync(Trigger.MOTION,
                    Bitmap.createBitmap(mCapturePreviewBitmap), mGuardService);
        }

        broadcastDiffPixelsNb(diffPixelsNb);
    }

    @Override
    public void onCameraClosed() {
        if (mAutoRestartWhenCameraClosed) {
            mAutoRestartWhenCameraClosed = false;
            startCapture();
        }
    }


    private void startCapture() {
        String cameraId = mPreferences.getSelectedCameraId();
        Camera2Wrapper.CameraCharacteristicsWrapper camera;
        Size outputSize;
        List<Surface> cameraTargets;
        Type.Builder yuvTypeBuilder;
        Type.Builder rgbTypeBuilder;

        if (cameraId.equals("") || mCameraWrapper.isCapturing()) {
            Log.w(LOG_TAG, "cannot start capture: " + (cameraId.equals("") ?
                    "no camera selected" : "already capturing"));
            return;
        }

        camera = findMatchingCamera(cameraId, mCameraWrapper.getCameras());
        if (camera == null) {
            Log.w(LOG_TAG, "cannot start capture: selected camera ID " + cameraId +
                    " cannot be found");
            return;
        }

        outputSize = Camera2Wrapper.findLowestHDOutputSizeAvailable(camera);
        mCapturePreviewWidth = outputSize.getWidth();
        mCapturePreviewHeight = outputSize.getHeight();
        Log.i(LOG_TAG, "Capture preview size set to " + mCapturePreviewWidth + " x " +
                mCapturePreviewHeight);

        outputSize = Camera2Wrapper.findLowestOutputSizeAvailable(camera);
        mCaptureProcessingWidth = outputSize.getWidth();
        mCaptureProcessingHeight = outputSize.getHeight();
        Log.i(LOG_TAG, "Capture processing size set to " + mCaptureProcessingWidth + " x " +
                mCaptureProcessingHeight);

        cameraTargets = new ArrayList<>();
        if (mCapturePreviewSurface != null) {
            cameraTargets.add(mCapturePreviewSurface);
        }

        yuvTypeBuilder = new Type.Builder(mRenderScript, Element.YUV(mRenderScript))
                .setX(mCapturePreviewWidth)
                .setY(mCapturePreviewHeight)
                .setYuvFormat(ImageFormat.YUV_420_888);
        mCaptureYuvAllocation = Allocation.createTyped(mRenderScript, yuvTypeBuilder.create(),
                Allocation.USAGE_SCRIPT | Allocation.USAGE_IO_INPUT);
        cameraTargets.add(mCaptureYuvAllocation.getSurface());
        mCaptureYuvAllocation.setOnBufferAvailableListener(this);

        rgbTypeBuilder = new Type.Builder(mRenderScript, Element.RGBA_8888(mRenderScript))
                .setX(mCapturePreviewWidth)
                .setY(mCapturePreviewHeight);
        mCaptureRgbAllocation = Allocation.createTyped(mRenderScript, rgbTypeBuilder.create(),
                Allocation.USAGE_SCRIPT);

        rgbTypeBuilder = new Type.Builder(mRenderScript, Element.RGBA_8888(mRenderScript))
                .setX(mCaptureProcessingWidth)
                .setY(mCaptureProcessingHeight);
        mCaptureRgbResizedAllocation = Allocation.createTyped(mRenderScript,
                rgbTypeBuilder.create(), Allocation.USAGE_SCRIPT);
        mCaptureGrayscaleResizedAllocation = Allocation.createTyped(mRenderScript,
                rgbTypeBuilder.create(), Allocation.USAGE_SCRIPT);
        mCaptureGrayscaleResizedBlurredAllocation = Allocation.createTyped(mRenderScript,
                rgbTypeBuilder.create(), Allocation.USAGE_SCRIPT);

        mYuvToRgbIntrinsicScript = ScriptIntrinsicYuvToRGB.create(mRenderScript,
                Element.U8_4(mRenderScript));
        mResizeIntrinsicScript = ScriptIntrinsicResize.create(mRenderScript);
        mGrayscaleScript = new ScriptC_grayscale(mRenderScript);
        mBlurIntrinsicScript = ScriptIntrinsicBlur.create(mRenderScript,
                Element.U8_4(mRenderScript));

        mCapturePreviewBitmap = Bitmap.createBitmap(mCapturePreviewWidth,
                mCapturePreviewHeight, Bitmap.Config.ARGB_8888);
        mCaptureProcessingBitmap = Bitmap.createBitmap(mCaptureProcessingWidth,
                mCaptureProcessingHeight, Bitmap.Config.ARGB_8888);

        mCaptureProcessingBitmapPixels =
                new int[mCaptureProcessingWidth * mCaptureProcessingHeight];
        mReferenceBitmapPixels = new int[mCaptureProcessingWidth * mCaptureProcessingHeight];

        Log.i(LOG_TAG, "starting capture...");
        mCameraWrapper.startCaptureAsync(cameraId, cameraTargets);
        mIsStoppingCapture = false;
    }

    private void stopCapture() {
        if (!mCameraWrapper.isCapturing()) {
            Log.w(LOG_TAG, "cannot stop capture: camera is not capturing");
            return;
        }

        Log.i(LOG_TAG, "stopping capture...");

        mIsStoppingCapture = true;
        mCameraWrapper.stopCapture();
    }


    private int computeGrayscaleDiffPixels() {
        mCaptureProcessingBitmap.getPixels(mCaptureProcessingBitmapPixels, 0,
                mCaptureProcessingWidth, 0, 0, mCaptureProcessingWidth,
                mCaptureProcessingHeight);

        if (mReferenceBitmapPixels == null) {
            mReferenceBitmapPixels = mCaptureProcessingBitmapPixels.clone();
            return -1;
        }

        int diffNb = 0;
        for (int x = 0; x < mCaptureProcessingWidth; ++x) {
            for (int y = 0; y < mCaptureProcessingHeight; ++y) {
                int pixel1 = mCaptureProcessingBitmapPixels[x + y * mCaptureProcessingWidth];
                int pixel1Y = Color.red(pixel1); // RGB components have the same value in grayscale
                int pixel2 = mReferenceBitmapPixels[x + y * mCaptureProcessingWidth];
                int pixel2Y = Color.red(pixel2); // RGB components have the same value in grayscale
                int diff = Math.abs(pixel1Y - pixel2Y);

                if (diff >= mPixelValueDiffThreshold) {
                    diffNb++;
                }
            }
        }

        mReferenceBitmapPixels = mCaptureProcessingBitmapPixels.clone();

        return diffNb;
    }


    private void broadcastDiffPixelsNb(int diffPixelsNb) {
        Intent intent = new Intent(
                MainActivity.INTENT_ACTION_TRIGGERS_MOTION_UPDATE_DIFF_PIXELS_NB);
        intent.putExtra(TriggersMotionFragment.INTENT_EXTRA_DIFF_PIXELS_NB, diffPixelsNb);
        LocalBroadcastManager.getInstance(mGuardService).sendBroadcast(intent);
    }


    private static Camera2Wrapper.CameraCharacteristicsWrapper findMatchingCamera(
            String cameraId, List<Camera2Wrapper.CameraCharacteristicsWrapper> cameras) {
        for (Camera2Wrapper.CameraCharacteristicsWrapper camera : cameras) {
            if (camera.CameraId.equals(cameraId)) {
                return camera;
            }
        }

        return null;
    }
}
