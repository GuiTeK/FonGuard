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
package com.fonguard.utils.java.io;

import android.content.ContentResolver;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class File {
    public static String readAllText(Uri uri, ContentResolver contentResolver) throws IOException {
        InputStream fileInputStream = contentResolver.openInputStream(uri);
        int fileLength = fileInputStream.available();
        byte[] buffer = new byte[fileLength];

        fileInputStream.read(buffer, 0, fileLength);
        fileInputStream.close();

        return new String(buffer, StandardCharsets.UTF_8);
    }
}
