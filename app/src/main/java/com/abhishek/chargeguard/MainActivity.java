package com.abhishek.chargeguard;

import android.Manifest;
import android.app.*;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout root, list;
    private TextView batteryText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private BroadcastReceiver batteryReceiver;
    private final int green = Color.rgb(27,127,90);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        buildUi();
        requestNotifications();
        batteryReceiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { renderBattery(i); } };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(14),dp(16),dp(28));
        root.setBackgroundColor(Color.rgb(246,250,248)); scroll.addView(root);
        TextView title = text("ChargeGuard Lite", 26, Color.rgb(16,82,59)); title.setTypeface(null, 1); root.addView(title);
        root.addView(text("Safe charging assistant for low-memory Android phones", 14, Color.DKGRAY));
        batteryText = card("Reading battery status..."); root.addView(batteryText);
        addHeader("Quick charging mode");
        root.addView(text("Android does not allow a normal app to switch the phone into Safe Mode or stop other apps silently. These shortcuts help you reproduce the useful parts safely.", 14, Color.DKGRAY));
        addButton("Open Battery Saver", () -> openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS));
        addButton("Open Battery Usage / Restrict apps", () -> openSettings(Settings.ACTION_BATTERY_SETTINGS));
        addButton("Open Running Services", () -> openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        addButton("Lower brightness to 15%", this::lowerBrightness);
        addHeader("Storage and unused apps");
        root.addView(text("Grant Usage Access, then scan. Tap an app to open its system page where you can Disable, Force stop, Clear cache, or Clear storage. Disabling is reversible but availability depends on the phone maker.", 14, Color.DKGRAY));
        addButton("1. Grant Usage Access", () -> openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        addButton("2. Scan apps unused for 30+ days", this::scanUnusedApps);
        addButton("Open Android Storage Manager", () -> openSettings(Build.VERSION.SDK_INT >= 25 ? Settings.ACTION_INTERNAL_STORAGE_SETTINGS : Settings.ACTION_SETTINGS));
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        addHeader("Healthy charge checklist");
        root.addView(text("• Use the original or a good-quality charger and cable\n• Keep the phone cool and remove a thick case if hot\n• Turn off hotspot, GPS, gaming, and video while charging\n• Restrict suspicious background apps\n• Do not use task-killer or RAM-booster apps", 15, Color.DKGRAY));
        setContentView(scroll);
    }

    private void renderBattery(Intent i) {
        int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1), scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,100);
        int status=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1), plugged=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
        int temp=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0);
        float pct=scale>0?level*100f/scale:0, c=temp/10f;
        String state=(status==BatteryManager.BATTERY_STATUS_CHARGING?"Charging":status==BatteryManager.BATTERY_STATUS_FULL?"Full":"Not charging");
        String source=plugged==BatteryManager.BATTERY_PLUGGED_AC?"AC":plugged==BatteryManager.BATTERY_PLUGGED_USB?"USB":plugged==BatteryManager.BATTERY_PLUGGED_WIRELESS?"Wireless":"Battery";
        String warning=c>=42?"\nWarning: battery is hot. Unplug and let it cool.":c>=38?"\nWarm: reduce phone use while charging.":"\nTemperature looks normal.";
        batteryText.setText(String.format(Locale.getDefault(),"Battery: %.0f%%\n%s via %s\nTemperature: %.1f°C%s",pct,state,source,c,warning));
    }

    private void scanUnusedApps() {
        if (!hasUsageAccess()) { Toast.makeText(this,"Grant Usage Access first",Toast.LENGTH_LONG).show(); openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS); return; }
        list.removeAllViews();
        long end=System.currentTimeMillis(), start=end-90L*24*60*60*1000, cutoff=end-30L*24*60*60*1000;
        UsageStatsManager usm=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
        Map<String,UsageStats> stats=usm.queryAndAggregateUsageStats(start,end);
        PackageManager pm=getPackageManager();
        Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps=pm.queryIntentActivities(launcher,0);
        Map<String,ResolveInfo> unique=new TreeMap<>();
        for(ResolveInfo r:apps) if(!r.activityInfo.packageName.equals(getPackageName())) unique.put(r.activityInfo.packageName,r);
        int shown=0;
        for(Map.Entry<String,ResolveInfo> e:unique.entrySet()) {
            UsageStats s=stats.get(e.getKey()); long last=s==null?0:s.getLastTimeUsed();
            if(last==0 || last<cutoff) {
                String label=e.getValue().loadLabel(pm).toString();
                String when=last==0?"No recent usage record":"Last used "+DateFormat.getDateInstance().format(new Date(last));
                Button btn=new Button(this); btn.setAllCaps(false); btn.setText(label+"\n"+when); btn.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
                final String pkg=e.getKey(); btn.setOnClickListener(v -> openAppDetails(pkg)); list.addView(btn); shown++;
            }
        }
        if(shown==0) list.addView(text("No launcher apps matched the 30-day rule.",14,Color.DKGRAY));
        Toast.makeText(this,"Found "+shown+" potentially unused apps",Toast.LENGTH_SHORT).show();
    }

    private boolean hasUsageAccess() {
        AppOpsManager a=(AppOpsManager)getSystemService(APP_OPS_SERVICE);
        int mode=a.checkOpNoThrow("android:get_usage_stats",Process.myUid(),getPackageName());
        return mode==AppOpsManager.MODE_ALLOWED;
    }
    private void openAppDetails(String pkg) { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg))); }
    private void lowerBrightness() {
        WindowManager.LayoutParams lp=getWindow().getAttributes(); lp.screenBrightness=0.15f; getWindow().setAttributes(lp);
        Toast.makeText(this,"Brightness lowered for this app window",Toast.LENGTH_SHORT).show();
    }
    private void openSettings(String action) { try { startActivity(new Intent(action)); } catch(Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); } }
    private void requestNotifications() { if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7); }
    private void addHeader(String s) { TextView v=text(s,19,green); v.setTypeface(null,1); v.setPadding(0,dp(20),0,dp(6)); root.addView(v); }
    private void addButton(String s, final Runnable r) { Button b=new Button(this); b.setText(s); b.setAllCaps(false); b.setOnClickListener(v->r.run()); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(5),0,dp(5)); root.addView(b,p); }
    private TextView card(String s) { TextView v=text(s,17,Color.WHITE); v.setBackgroundColor(green); v.setPadding(dp(16),dp(16),dp(16),dp(16)); LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,dp(16),0,dp(4)); v.setLayoutParams(p); return v; }
    private TextView text(String s,int sp,int color) { TextView v=new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setLineSpacing(0,1.15f); return v; }
    private int dp(int n){ return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
    @Override protected void onDestroy(){ super.onDestroy(); if(batteryReceiver!=null) unregisterReceiver(batteryReceiver); handler.removeCallbacksAndMessages(null); }
}
