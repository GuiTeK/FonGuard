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
package com.fonguard.utils.android.graphics;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Bitmap {
    private static final String LOG_TAG = Bitmap.class.getName();

    public static byte[] toBytes(android.graphics.Bitmap bitmap,
                                 android.graphics.Bitmap.CompressFormat format) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer;

        bitmap.compress(format, 100, outputStream);
        buffer = outputStream.toByteArray();

        try {
            outputStream.close();
        } catch (IOException ex) {
            Log.w(LOG_TAG, "Could not close bitmap outputStream: " + ex.getMessage());
        }

        return buffer;
    }
}
