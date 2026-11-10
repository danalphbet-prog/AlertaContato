package com.diemdev.alertacontato;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public final class AlertNotifier {
    private static final String CHANNEL = "sos_realtime_v2";
    private AlertNotifier() {}
    public static void makeChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm.getNotificationChannel(CHANNEL)!=null)return;
        Uri sound=Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.siren);
        AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Alerta SOS em tempo real",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Alertas dos contatos pareados recebidos pelo Realtime Database");
        channel.setSound(sound,attrs);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0,700,200,700,200,1000});
        nm.createNotificationChannel(channel);
    }
    public static void notifySos(Context context,String senderId){
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        makeChannel(context);
        Intent intent=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder alert=new NotificationCompat.Builder(context,CHANNEL)
           .setSmallIcon(android.R.drawable.ic_dialog_alert)
           .setContentTitle("ALERTA SOS DE UM CONTATO")
           .setContentText("Um contato pareado acionou a segurança. Abra o aplicativo.")
           .setStyle(new NotificationCompat.BigTextStyle().bigText("O contato "+senderId+" acionou um SOS. Verifique sua segurança e contate a emergência quando necessário."))
           .setPriority(NotificationCompat.PRIORITY_MAX)
           .setCategory(NotificationCompat.CATEGORY_ALARM)
           .setAutoCancel(true).setContentIntent(pi);
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE))
            .notify((int)(System.currentTimeMillis()%Integer.MAX_VALUE),alert.build());
    }
}
