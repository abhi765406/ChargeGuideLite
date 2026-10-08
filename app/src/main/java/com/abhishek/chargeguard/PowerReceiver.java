package com.abhishek.chargeguard;

import android.app.*;
import android.content.*;
import android.os.Build;

public class PowerReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "charge_tips";
    @Override public void onReceive(Context c, Intent i) {
        NotificationManager nm = (NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(new NotificationChannel(CHANNEL, "Charging tips", NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CHANNEL) : new Notification.Builder(c);
        b.setSmallIcon(com.abhishek.chargeguard.R.drawable.ic_app).setContentTitle("Charging started")
          .setContentText("Open ChargeGuard for temperature and slow-charge checks.").setContentIntent(pi).setAutoCancel(true);
        try { nm.notify(1001, b.build()); } catch (SecurityException ignored) { }
    }
}
