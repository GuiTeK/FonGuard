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

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fonguard.utils.camera.Camera2Wrapper;

import java.util.List;

public class TriggersMotionViewModel extends ViewModel {
    private MutableLiveData<List<Camera2Wrapper.CameraCharacteristicsWrapper>> mCameras;
    private MutableLiveData<String> mSelectedCameraId;
    private MutableLiveData<Integer> mPixelValueDiffThreshold;
    private MutableLiveData<Integer> mPixelNumberDiffThreshold;


    public TriggersMotionViewModel() {
        mCameras = new MutableLiveData<>();
        mSelectedCameraId = new MutableLiveData<>();
        mPixelValueDiffThreshold = new MutableLiveData<>();
        mPixelNumberDiffThreshold = new MutableLiveData<>();
    }


    MutableLiveData<List<Camera2Wrapper.CameraCharacteristicsWrapper>> getCameras() {
        return mCameras;
    }

    MutableLiveData<String> getSelectedCameraId() {
        return mSelectedCameraId;
    }

    MutableLiveData<Integer> getPixelValueDiffThreshold() {
        return mPixelValueDiffThreshold;
    }

    MutableLiveData<Integer> getPixelNumberDiffThreshold() {
        return mPixelNumberDiffThreshold;
    }
}
