package com.fonguard.utils.android.telephony;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class CallManager {
    public static void makeCall(Context context, String recipient) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);

        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        callIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        callIntent.setData(Uri.parse("tel:" + recipient));
        context.startActivity(callIntent);
    }
}
