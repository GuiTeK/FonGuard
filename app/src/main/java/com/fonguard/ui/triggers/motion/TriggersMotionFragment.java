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
package com.fonguard.ui.triggers.motion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.fonguard.MainActivity;
import com.fonguard.Preferences;
import com.fonguard.R;
import com.fonguard.guardservice.triggers.MotionTrigger;
import com.fonguard.utils.camera.Camera2Wrapper;

import java.util.ArrayList;
import java.util.List;

import static com.fonguard.guardservice.triggers.MotionTrigger.PIXEL_NUMBER_DIFF_THRESHOLD_MAX;
import static com.fonguard.guardservice.triggers.MotionTrigger.PIXEL_NUMBER_DIFF_THRESHOLD_MIN;
import static com.fonguard.guardservice.triggers.MotionTrigger.PIXEL_VALUE_DIFF_THRESHOLD_MAX;
import static com.fonguard.guardservice.triggers.MotionTrigger.PIXEL_VALUE_DIFF_THRESHOLD_MIN;

public class TriggersMotionFragment extends Fragment {
    private static final String LOG_TAG = TriggersMotionFragment.class.getName();

    public static final int HANDLER_MSG_UPDATE_DIFF_PIXELS_NB = 1;

    public static final String INTENT_EXTRA_DIFF_PIXELS_NB = "DiffPixelsNb";

    private Preferences mPreferences;
    private TriggersMotionViewModel mTriggersMotionViewModel;

    private Spinner mCamerasSpinner;
    private SurfaceView mCameraPreviewSurfaceView;
    private EditText mPixelValueDiffThresholdEditTextNumber;
    private EditText mPixelPercentageDiffThresholdEditTextNumber;
    private TextView mMotionStatusTextView;

    private boolean mIsCamerasSpinnerInitialized = false;
    private int mPixelNumberDiffThreshold;

    private List<Camera2Wrapper.CameraCharacteristicsWrapper> mCameras;

    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case HANDLER_MSG_UPDATE_DIFF_PIXELS_NB:
                    int diffPixels = (int)msg.obj;
                    updateMotionStatusUi(diffPixels);
                    break;
                default:
                    Log.w(LOG_TAG, "unhandled TriggersMotionFragment message \"" +
                            msg.what + "\"");
                    break;
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MainActivity.INTENT_ACTION_TRIGGERS_MOTION_UPDATE_DIFF_PIXELS_NB: {
                    Message msg = mHandler.obtainMessage(HANDLER_MSG_UPDATE_DIFF_PIXELS_NB);
                    msg.obj = intent.getExtras().getInt(INTENT_EXTRA_DIFF_PIXELS_NB);
                    mHandler.sendMessage(msg);
                    break;
                }
                default:
                    Log.w(LOG_TAG, "received unknown intent action " + intent.getAction());
                    break;
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_triggers_motion, container, false);
        Camera2Wrapper camera2Wrapper = new Camera2Wrapper(getActivity());
        IntentFilter intentFilter = new IntentFilter();

        mPreferences = Preferences.getInstance(getContext());
        mTriggersMotionViewModel = ViewModelProviders.of(this).get(
                TriggersMotionViewModel.class);

        mCamerasSpinner = root.findViewById(R.id.fragment_triggers_motion_cameras_spinner);
        mCameraPreviewSurfaceView = root.findViewById(
                R.id.fragment_triggers_motion_camera_preview_surfaceView);
        mPixelValueDiffThresholdEditTextNumber = root.findViewById(
                R.id.fragment_triggers_motion_pixel_value_diff_threshold_editTextNumber);
        mPixelPercentageDiffThresholdEditTextNumber = root.findViewById(
                R.id.fragment_triggers_motion_pixel_number_diff_threshold_editTextNumber);
        mMotionStatusTextView = root.findViewById(R.id.fragment_triggers_motion_status_textView);

        mPixelNumberDiffThreshold = mPreferences.getPixelNumberDiffThreshold();

        mCameras = camera2Wrapper.getCameras();

        initCamerasUi();
        initCameraPreviewUi();
        initPixelValueDiffThresholdUi();
        initPixelDiffNumberThresholdUi();

        intentFilter.addAction(MainActivity.INTENT_ACTION_TRIGGERS_MOTION_UPDATE_DIFF_PIXELS_NB);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(mBroadcastReceiver,
                intentFilter);

        return root;
    }

    private void initCamerasUi() {
        final String defaultSelectedCameraId = mPreferences.getSelectedCameraId();
        final int defaultSelectedCameraIndex = Camera2Wrapper.getCameraIndex(mCameras,
                defaultSelectedCameraId);

        mTriggersMotionViewModel.getCameras().observe(this,
                new Observer<List<Camera2Wrapper.CameraCharacteristicsWrapper>>() {
                    @Override
                    public void onChanged(List<Camera2Wrapper.CameraCharacteristicsWrapper> cameras) {
                        ArrayList<String> cameraItems = new ArrayList<>();
                        ArrayAdapter<String> cameraItemsAdapter = new ArrayAdapter<>(
                                TriggersMotionFragment.this.getContext(),
                                android.R.layout.simple_spinner_dropdown_item, cameraItems);

                        for (Camera2Wrapper.CameraCharacteristicsWrapper camera : cameras) {
                            if (!camera.IsBackCamera && !camera.IsFrontCamera) {
                                continue;
                            }

                            cameraItems.add(getString(R.string.fragment_triggers_motion_camera_name,
                                    camera.CameraId) + " (" + (camera.IsFrontCamera ?
                                    getString(R.string.fragment_triggers_motion_camera_front) :
                                    getString(R.string.fragment_triggers_motion_camera_back)) + ")");
                        }

                        cameraItemsAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        mCamerasSpinner.setAdapter(cameraItemsAdapter);
                    }
                });

        mTriggersMotionViewModel.getSelectedCameraId().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String selectedCameraId) {
                mPreferences.setSelectedCameraId(selectedCameraId);
                restartMotionTrigger();
            }
        });

        mCamerasSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mIsCamerasSpinnerInitialized) {
                    mIsCamerasSpinnerInitialized = true;
                    if (defaultSelectedCameraIndex != -1) {
                        mCamerasSpinner.setSelection(defaultSelectedCameraIndex);
                    }
                }

                mTriggersMotionViewModel.getSelectedCameraId().setValue(
                        mCameras.get(position).CameraId);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mTriggersMotionViewModel.getSelectedCameraId().setValue("");
            }
        });

        mTriggersMotionViewModel.getCameras().setValue(mCameras);
    }

    private void initCameraPreviewUi() {
        mCameraPreviewSurfaceView.getHolder().setFixedSize(150, 150);

        setMotionTriggerPreviewSurface();
    }

    private void initPixelValueDiffThresholdUi() {
        mTriggersMotionViewModel.getPixelValueDiffThreshold().observe(this,
                new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                        mPreferences.setPixelValueDiffThreshold(integer);

                        setMotionTriggerPixelValueDiffThreshold(integer);
                    }
                });

        mPixelValueDiffThresholdEditTextNumber.setText(
                Integer.toString(mPreferences.getPixelValueDiffThreshold()));
        mPixelValueDiffThresholdEditTextNumber
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            int value = Integer.parseInt(s.toString());

                            // TODO: use an InputFilter instead
                            if (value >= PIXEL_VALUE_DIFF_THRESHOLD_MIN &&
                                    value <= PIXEL_VALUE_DIFF_THRESHOLD_MAX) {
                                mTriggersMotionViewModel.getPixelValueDiffThreshold()
                                        .setValue(value);
                            }
                        } catch (NumberFormatException e) { }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
    }

    private void initPixelDiffNumberThresholdUi() {
        mTriggersMotionViewModel.getPixelNumberDiffThreshold().observe(this,
                new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                        mPreferences.setPixelNumberDiffThreshold(integer);
                        mPixelNumberDiffThreshold = integer;

                        setMotionTriggerPixelPercentageDiffThreshold(integer);
                    }
                });

        mPixelPercentageDiffThresholdEditTextNumber.setText(
                Integer.toString(mPreferences.getPixelNumberDiffThreshold()));
        mPixelPercentageDiffThresholdEditTextNumber
                .addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        try {
                            int value = Integer.parseInt(s.toString());

                            // TODO: use an InputFilter instead
                            if (value >= PIXEL_NUMBER_DIFF_THRESHOLD_MIN &&
                                    value <= PIXEL_NUMBER_DIFF_THRESHOLD_MAX) {
                                mTriggersMotionViewModel.getPixelNumberDiffThreshold()
                                        .setValue(value);
                            }
                        } catch (NumberFormatException e) { }
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                    }
                });
    }

    private void updateMotionStatusUi(int diffPixelsNb) {
        boolean isMotionActive = diffPixelsNb >= mPixelNumberDiffThreshold;
        String motionStatusText = getString(R.string.fragment_triggers_motion_status,
                diffPixelsNb,
                isMotionActive ?
                        getString(R.string.fragment_triggers_motion_status_motion) :
                        getString(R.string.fragment_triggers_motion_status_no_motion));

        mMotionStatusTextView.setText(motionStatusText);

        if (isMotionActive) {
            mMotionStatusTextView.setTextColor(Color.RED);
        } else {
            mMotionStatusTextView.setTextColor(Color.GREEN);
        }
    }


    private void setMotionTriggerPreviewSurface() {
        Intent intent = new Intent(MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PREVIEW_SURFACE);
        intent.putExtra(MotionTrigger.INTENT_EXTRA_PREVIEW_SURFACE,
                mCameraPreviewSurfaceView.getHolder().getSurface());
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void restartMotionTrigger() {
        Intent intent = new Intent(MainActivity.INTENT_ACTION_MOTION_TRIGGER_RESTART);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void setMotionTriggerPixelValueDiffThreshold(int value) {
        Intent intent = new Intent(
                MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_VALUE_DIFF_THRESHOLD);
        intent.putExtra(MotionTrigger.INTENT_EXTRA_PIXEL_VALUE_DIFF_THRESHOLD, value);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void setMotionTriggerPixelPercentageDiffThreshold(int value) {
        Intent intent = new Intent(
                MainActivity.INTENT_ACTION_MOTION_TRIGGER_SET_PIXEL_NUMBER_DIFF_THRESHOLD);
        intent.putExtra(MotionTrigger.INTENT_EXTRA_PIXEL_NUMBER_DIFF_THRESHOLD, value);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }
}
