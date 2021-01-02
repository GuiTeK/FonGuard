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
package com.fonguard.utils.android.telephony;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.provider.Telephony;
import android.util.Log;

import com.google.android.mms.ContentType;
import com.google.android.mms.pdu.EncodedStringValue;
import com.google.android.mms.pdu.PduBody;
import com.google.android.mms.pdu.PduComposer;
import com.google.android.mms.pdu.PduPart;
import com.google.android.mms.pdu.SendReq;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.android.internal.telephony.Phone.APN_TYPE_ALL;
import static com.android.internal.telephony.Phone.APN_TYPE_MMS;

public class MmsManager {
    private static class Apn {
        public String MmsCenterUrl;
        public String MmsProxy;
        public String MmsProxyPort;
    }

    private static final String LOG_TAG = MmsManager.class.getName();

    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private static final String CONTENT_TYPE_JPEG = "image/jpeg";

    private static final String[] APN_PROJECTION = {
            Telephony.Carriers.TYPE,
            Telephony.Carriers.MMSC,
            Telephony.Carriers.MMSPROXY,
            Telephony.Carriers.MMSPORT
    };


    public static void sendMmsAsync(Context context, String recipient, String text, Bitmap img) {
        final ConnectivityManager connManager =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
        NetworkRequest networkRequest;

        networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_MMS);
        networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        networkRequest = networkRequestBuilder.build();
        connManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NotNull Network network) {
                super.onAvailable(network);

                Apn preferredApn = getPreferredApn(context);
                byte[] mmsPdu = buildMmsPdu(context, recipient, text, img);

                sendMmsHttpRequest(preferredApn, mmsPdu);
            }
        });
    }


    private static boolean sendMmsHttpRequest(Apn apn, byte[] mmsPdu) {
        OkHttpClient httpClient = new OkHttpClient();
        Request.Builder httpRequestBuilder = new Request.Builder();
        Request httpRequest;
        RequestBody httpRequestBody;

        httpRequestBuilder.url(apn.MmsCenterUrl);
        httpRequestBody = RequestBody.create(mmsPdu, MediaType.parse(ContentType.MMS_MESSAGE));
        httpRequestBuilder.post(httpRequestBody);
        httpRequest = httpRequestBuilder.build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            Log.i(LOG_TAG, "Posted MMS request and got response: " + response.code());
            return true;
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed to post MMS request: " + ex.getMessage());
            return false;
        }
    }

    private static byte[] buildMmsPdu(Context context, String recipient, String text, Bitmap img) {
        SendReq sendReq = new SendReq();
        PduBody pduBody = new PduBody();
        PduPart textPart = new PduPart();

        sendReq.addTo(new EncodedStringValue(recipient));

        textPart.setName("Text".getBytes());
        textPart.setContentType(CONTENT_TYPE_TEXT.getBytes());
        textPart.setData(text.getBytes());

        if (img != null) {
            PduPart imgPart = new PduPart();
            byte[] imgBytes = com.fonguard.utils.android.graphics.Bitmap.toBytes(img,
                    Bitmap.CompressFormat.JPEG);

            imgPart.setName("Image".getBytes());
            imgPart.setContentType(CONTENT_TYPE_JPEG.getBytes());
            imgPart.setData(imgBytes);

            pduBody.addPart(imgPart);
        }

        pduBody.addPart(textPart);
        sendReq.setBody(pduBody);

        return new PduComposer(context, sendReq).make();
    }

    private static Apn getPreferredApn(Context context) {
        /*Cursor apnCursor = SqliteWrapper.query(context, context.getContentResolver(),
                Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current"),
                APN_PROJECTION, null, null, null);*/
        /*Cursor apnCursor = SqliteWrapper.query(context, context.getContentResolver(),
                Uri.parse("content://telephony/carriers/preferapn"),
                APN_PROJECTION, null, null, null);*/
        Cursor apnCursor = context
                .getContentResolver()
                .query(Uri.parse("content://telephony/carriers/preferapn"), APN_PROJECTION,
                        null, null, null);
        Apn preferredApn = null;

        if (apnCursor != null && apnCursor.moveToFirst()) {
            String type = apnCursor.getString(apnCursor.getColumnIndex(Telephony.Carriers.TYPE));

            if (type.contains(APN_TYPE_ALL) || type.contains(APN_TYPE_MMS)) {
                preferredApn = new Apn();
                preferredApn.MmsCenterUrl = apnCursor
                        .getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSC));
                preferredApn.MmsProxy = apnCursor
                        .getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSPROXY));
                preferredApn.MmsProxyPort = apnCursor
                        .getString(apnCursor.getColumnIndex(Telephony.Carriers.MMSPORT));

                Log.d(LOG_TAG, "Preferred APN is " + preferredApn.MmsCenterUrl + " with " +
                        "proxy " + preferredApn.MmsProxy + ":" + preferredApn.MmsProxyPort);
            }

            else {
                Log.e(LOG_TAG, "Preferred APN does not support MMS");
            }
        } else {
            Log.e(LOG_TAG, "Preferred APN not found");
        }

        apnCursor.close();

        return preferredApn;
    }
}
