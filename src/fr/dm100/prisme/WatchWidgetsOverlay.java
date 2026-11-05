package fr.dm100.prisme;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Four optional watch-only cards. No network, no screen capture and no periodic background service. */
public final class WatchWidgetsOverlay extends FrameLayout {
    public interface Host { void action(String action); }
    private static final String[] SHOW={"showClock","showTasks","showTimer","showDevice"};
    private static final String[] XS={"clockX","tasksX","timerX","infoX"};
    private static final String[] YS={"clockY","tasksY","timerY","infoY"};
    private static final int[] DEFAULT_X={3,57,3,57},DEFAULT_Y={10,10,75,75};
    private final SharedPreferences options,positions;
    private final Host host;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final TextView[] cards=new TextView[4];
    private boolean live;
    private long telemetryAt;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!live)return;
        updateTexts();
        handler.postDelayed(this,TimerEngine.deadline(getContext())>0?1000:30000);
    }};
    public WatchWidgetsOverlay(Context context,Host host){
        super(context);this.host=host;
        options=context.getSharedPreferences("prisme_widgets",Context.MODE_PRIVATE);
        positions=context.getSharedPreferences("prisme_studio",Context.MODE_PRIVATE);
        setClipChildren(false);setClickable(false);setFocusable(false);
    }
    public void refresh(){
        telemetryAt=0;
        removeAllViews();
        for(int i=0;i<4;i++)cards[i]=null;
        DisplayMetrics metrics=getResources().getDisplayMetrics();
        int width=Math.max(1,getWidth()>0?getWidth():metrics.widthPixels);
        int height=Math.max(1,getHeight()>0?getHeight():metrics.heightPixels);
        int size=Math.max(30,Math.min(200,options.getInt("size",75)));
        int opacity=Math.max(20,Math.min(100,options.getInt("opacity",88)));
        int cardWidth=Math.min(width,Math.max(70,Math.round(134f*size/100f)));
        int cardHeight=Math.min(height,Math.max(31,Math.round(48f*size/100f)));
        for(int i=0;i<4;i++){
            if(!options.getBoolean(SHOW[i],false))continue;
            TextView card=new TextView(getContext());
            card.setGravity(Gravity.CENTER);
            card.setTextSize(TypedValue.COMPLEX_UNIT_PX,Math.max(13,Math.round(17f*size/100f)));
            card.setTextColor(Color.WHITE);
            card.setSingleLine(true);
            GradientDrawable bg=new GradientDrawable();
            bg.setColor(0xe4152430);bg.setCornerRadius(9);bg.setStroke(1,0x889ed9d4);
            card.setBackground(bg);card.setAlpha(opacity/100f);
            final int index=i;
            card.setOnClickListener(v->host.action(index==0?"deskclock":index==1?"checklist":index==2?"timer":"device"));
            int x=Math.max(0,Math.min(100,positions.getInt(XS[i],DEFAULT_X[i])));
            int y=Math.max(0,Math.min(100,positions.getInt(YS[i],DEFAULT_Y[i])));
            FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(cardWidth,cardHeight,Gravity.TOP|Gravity.LEFT);
            params.leftMargin=Math.round((width-cardWidth)*x/100f);
            params.topMargin=Math.round((height-cardHeight)*y/100f);
            addView(card,params);cards[i]=card;
        }
        updateTexts();
    }
    private void updateTexts(){
        if(cards[0]!=null)cards[0].setText(new SimpleDateFormat("HH:mm",I18n.locale()).format(new Date()));
        if(cards[1]!=null)cards[1].setText("✓ "+new ChecklistStore(getContext()).remaining()+" tâches");
        if(cards[2]!=null){
            long left=TimerEngine.remaining(getContext());
            cards[2].setText(left>0?"⏱ "+ExperienceController.duration(left):"⏱ Minuteur");
        }
        if(cards[3]!=null&&android.os.SystemClock.uptimeMillis()>=telemetryAt){
            DeviceSnapshot state=DeviceSnapshot.read(getContext());
            cards[3].setText("◈ "+(state.batteryPct<0?"--":state.batteryPct+"%")+" · "+(state.wifiConnected?"Wi-Fi":"Hors ligne"));
            telemetryAt=android.os.SystemClock.uptimeMillis()+30000;
        }
    }
    public void onHostResume(){live=true;handler.removeCallbacks(tick);handler.post(tick);}
    public void onHostPause(){live=false;handler.removeCallbacks(tick);}
    @Override protected void onDetachedFromWindow(){onHostPause();super.onDetachedFromWindow();}
}
