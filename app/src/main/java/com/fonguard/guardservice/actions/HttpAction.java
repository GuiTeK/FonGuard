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
package com.fonguard.guardservice.actions;

import android.util.Log;

import com.fonguard.guardservice.triggers.Trigger;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpAction implements IAction {
    private static final String LOG_TAG = HttpAction.class.getName();

    private static final MediaType MEDIA_TYPE_BINARY = MediaType.parse("application/octet-stream");
    private static final MediaType MEDIA_TYPE_JPG = MediaType.parse("image/jpeg");

    private com.fonguard.guardservice.settings.actions.HttpAction mSettings;


    public HttpAction(com.fonguard.guardservice.settings.actions.HttpAction settings) {
        mSettings = settings;
    }


    @Override
    public boolean perform(Trigger source, boolean includePayload, byte[] payload) {
        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        Request request;

        requestBuilder.url(mSettings.Url);

        if (mSettings.Method.equals("GET")) {
            requestBuilder.get();
        } else if (mSettings.Method.equals("POST")) {
            RequestBody requestBody;

            if (includePayload && payload != null) {
                requestBody = RequestBody.create(payload, mediaTypeFromTrigger(source));
            } else {
                requestBody = RequestBody.create("", MEDIA_TYPE_BINARY);
            }

            requestBuilder.post(requestBody);
        } else {
            Log.w(LOG_TAG, "Unsupported HTTP method \"" + mSettings.Method + "\", aborting " +
                    "action");
            return true; // returning true because there should be no retry for this error
        }

        for (com.fonguard.guardservice.settings.actions.HttpAction.HttpHeader httpHeader :
                mSettings.Headers) {
            requestBuilder.addHeader(httpHeader.Name, httpHeader.Value);
        }

        request = requestBuilder.build();

        Log.i(LOG_TAG, "Performing HTTP action \"" + mSettings.Id + "\" (" + source +
                " trigger, include payload: " + includePayload + ")...");

        try (Response response = client.newCall(request).execute()) {
            Log.i(LOG_TAG, "Performed HTTP action \"" + mSettings.Id + "\" and got " +
                    "response: " + response.code());
            return true;
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed to perform HTTP action \"" + mSettings.Id + "\": " +
                    ex.getMessage());
            return false;
        }
    }


    private static MediaType mediaTypeFromTrigger(Trigger trigger) {
        switch (trigger) {
            case MOTION:
                return MEDIA_TYPE_JPG;
            default:
                return MEDIA_TYPE_BINARY;
        }
    }
}
