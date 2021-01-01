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
package com.fonguard.utils.camera;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.fonguard.utils.java.Collections;

import java.util.ArrayList;
import java.util.List;

public class Camera2Wrapper {
    private static final String LOG_TAG = Camera2Wrapper.class.getName();

    public final class CameraCharacteristicsWrapper {
        public String CameraId;
        public boolean IsFrontCamera;
        public boolean IsBackCamera;
        public Size[] AvailableOutputSizes;
    }

    public interface CameraClosedListener {
        void onCameraClosed();
    }

    private CameraManager mCameraManager;
    private ContextWrapper mContextWrapper;
    private CameraDevice mOpenCameraDevice;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraClosedListener mCameraClosedListener;

    public Camera2Wrapper(ContextWrapper contextWrapper) {
        mContextWrapper = contextWrapper;
        mCameraManager = (CameraManager)mContextWrapper.getSystemService(Context.CAMERA_SERVICE);
    }


    public boolean startCaptureAsync(String cameraId, final List<Surface> targets) {
        if (mOpenCameraDevice != null) {
            return false;
        }

        if (ContextCompat.checkSelfPermission(mContextWrapper, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        try {
            mCameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    mOpenCameraDevice = camera;

                    Log.i(LOG_TAG, "camera " + camera.getId() + " opened");

                    try {
                        mOpenCameraDevice.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession session) {
                                Log.i(LOG_TAG, "capture session of camera " +
                                        session.getDevice().getId() + " configured");

                                // Prevent restarting the capture as soon as it has finished
                                if (mOpenCameraDevice == null) {
                                    return;
                                }

                                try {
                                    CaptureRequest.Builder captureRequestBuilder = session
                                            .getDevice()
                                            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

                                    for (Surface target : targets) {
                                        captureRequestBuilder.addTarget(target);
                                    }

                                    session.setRepeatingRequest(captureRequestBuilder.build(), null, null);
                                    mCameraCaptureSession = session;
                                } catch (CameraAccessException e) {
                                    Log.e(LOG_TAG, e.getMessage());
                                }
                            }

                            @Override
                            public void onClosed(@NonNull CameraCaptureSession session) {
                                Log.i(LOG_TAG, "capture session of camera " +
                                        session.getDevice().getId() + " closed");

                                mCameraCaptureSession = null;
                                mOpenCameraDevice.close();
                            }

                            @Override
                            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                Log.e(LOG_TAG, "capture session of camera " +
                                        session.getDevice().getId() + " failed to configure");
                            }
                        }, null);
                    } catch (CameraAccessException e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                }

                @Override
                public void onClosed(@NonNull CameraDevice camera) {
                    Log.i(LOG_TAG, "camera " + camera.getId() + " closed");
                    mOpenCameraDevice = null;

                    if (mCameraClosedListener != null) {
                        mCameraClosedListener.onCameraClosed();
                    }
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.i(LOG_TAG, "camera " + camera.getId() + " disconnected");
                    mOpenCameraDevice = null;
                    mCameraCaptureSession = null;

                    if (mCameraClosedListener != null) {
                        mCameraClosedListener.onCameraClosed();
                    }
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(LOG_TAG, "camera " + camera.getId() + " error: " + error);
                    mOpenCameraDevice = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
            return false;
        }

        return true;
    }

    public void stopCapture() {
        mCameraCaptureSession.close();
    }

    public boolean isCapturing() {
        return mCameraCaptureSession != null && mOpenCameraDevice != null;
    }


    public List<CameraCharacteristicsWrapper> getCameras() {
        List<CameraCharacteristicsWrapper> cameras = new ArrayList<>();
        String[] cameraIds;

        try {
            cameraIds = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
            return null;
        }

        for (String cameraId : cameraIds) {
            CameraCharacteristicsWrapper cameraCharacteristicsWrapper =
                    new CameraCharacteristicsWrapper();
            CameraCharacteristics cameraCharacteristics;
            int[] availableCapabilities;
            int lensFacing;
            StreamConfigurationMap streamConfigurationMap;

            try {
                cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG, e.getMessage());
                continue;
            }

            availableCapabilities = cameraCharacteristics.get(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            if (!Collections.intArrayContains(availableCapabilities,
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)) {
                continue;
            }

            cameraCharacteristicsWrapper.CameraId = cameraId;

            lensFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            cameraCharacteristicsWrapper.IsBackCamera =
                    lensFacing == CameraCharacteristics.LENS_FACING_BACK;
            cameraCharacteristicsWrapper.IsFrontCamera =
                    lensFacing == CameraCharacteristics.LENS_FACING_FRONT;

            streamConfigurationMap = cameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            cameraCharacteristicsWrapper.AvailableOutputSizes =
                    streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

            cameras.add(cameraCharacteristicsWrapper);
        }

        return cameras;
    }

    public static int getCameraIndex(List<CameraCharacteristicsWrapper> cameras, String cameraId) {
        for (int i = 0; i < cameras.size(); ++i) {
            if (cameras.get(i).CameraId.equals(cameraId)) {
                return i;
            }
        }

        return -1;
    }


    public void setCameraClosedListener(CameraClosedListener listener) {
        mCameraClosedListener = listener;
    }


    public static Size findLowestOutputSizeAvailable(CameraCharacteristicsWrapper camera) {
        Size lowestOutputSizeAvailable = null;

        for (Size outputSize : camera.AvailableOutputSizes) {
            if (lowestOutputSizeAvailable == null ||
                    (outputSize.getWidth() * outputSize.getHeight()) <
                            (lowestOutputSizeAvailable.getWidth() *
                                    lowestOutputSizeAvailable.getHeight())) {
                lowestOutputSizeAvailable = outputSize;
            }
        }

        return lowestOutputSizeAvailable;
    }

    public static Size findLowestHDOutputSizeAvailable(CameraCharacteristicsWrapper camera) {
        final int HD_MIN_PIXELS = 1280 * 720;
        Integer lowestDiffWithHdMinPixels = null;
        Size lowestHDOutputSizeAvailable = null;

        for (Size outputSize : camera.AvailableOutputSizes) {
            int pixels = outputSize.getWidth() * outputSize.getHeight();
            int diffWithHdMinPixels = Math.abs(HD_MIN_PIXELS - pixels);

            if (lowestHDOutputSizeAvailable == null ||
                    diffWithHdMinPixels < lowestDiffWithHdMinPixels) {
                lowestDiffWithHdMinPixels = diffWithHdMinPixels;
                lowestHDOutputSizeAvailable = outputSize;
            }
        }

        return lowestHDOutputSizeAvailable;
    }
}
