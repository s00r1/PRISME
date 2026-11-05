package fr.dm100.prisme;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.TypedValue;

/** Touch editor on the watch: drag cards, snap to a 5% grid, persist positions locally. */
public final class WatchStudio {
    private WatchStudio(){}
    private static final String[] SHOW={"showClock","showTasks","showTimer","showDevice"};
    private static final String[] LABEL={"Horloge","Tâches","Minuteur","État montre"};
    private static final String[] XS={"clockX","tasksX","timerX","infoX"};
    private static final String[] YS={"clockY","tasksY","timerY","infoY"};
    private static final int[] DX={3,57,3,57},DY={10,10,75,75};
    public static void open(Activity activity,DashboardPrefs dashboard,Runnable changed,Runnable themeChanged){
        SharedPreferences shown=activity.getSharedPreferences("prisme_widgets",Context.MODE_PRIVATE);
        SharedPreferences layout=activity.getSharedPreferences("prisme_studio",Context.MODE_PRIVATE);
        LinearLayout root=new LinearLayout(activity);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(8,8,8,8);
        TextView help=new TextView(activity);
        help.setText("Glisse une carte sur l'aperçu. Chaque carte s'aimante à la grille. Tous les 22 styles restent disponibles.");
        help.setTextColor(0xffd9e6ed);help.setTextSize(TypedValue.COMPLEX_UNIT_PX,15);root.addView(help);
        Editor canvas=new Editor(activity,layout,shown,changed);
        root.addView(canvas,new LinearLayout.LayoutParams(-1,Math.max(190,activity.getResources().getDisplayMetrics().heightPixels/2)));
        for(int i=0;i<4;i++){
            final int n=i;CheckBox c=new CheckBox(activity);
            c.setText("Afficher "+LABEL[i]);c.setTextColor(0xffe8ffff);
            c.setChecked(shown.getBoolean(SHOW[i],false));
            c.setOnCheckedChangeListener((v,checked)->{shown.edit().putBoolean(SHOW[n],checked).apply();canvas.invalidate();changed.run();});
            root.addView(c);
        }
        android.widget.Button pack=new android.widget.Button(activity);pack.setText("Packs de style et widgets");
        pack.setOnClickListener(v->{
            String[] names={"Minimal OLED","Console cyber","Quotidien Nova","Tablette et infos"};
            int[] themes={21,5,20,1};
            new AlertDialog.Builder(activity).setTitle("Packs PRISME").setItems(names,(dialog,selected)->{
                int id=themes[selected];dashboard.putInt("theme",id);
                android.content.SharedPreferences.Editor e=shown.edit();
                e.putBoolean("showClock",selected!=0).putBoolean("showTasks",selected>=2)
                    .putBoolean("showTimer",selected==1||selected==2).putBoolean("showDevice",selected==1||selected==3);
                e.putInt("size",selected==0?60:75).apply();
                changed.run();themeChanged.run();canvas.invalidate();
            }).setNegativeButton("Annuler",null).show();
        });root.addView(pack);
        TextView size=new TextView(activity);size.setText("Taille des widgets");size.setTextColor(0xffe8ffff);root.addView(size);
        SeekBar slider=new SeekBar(activity);slider.setMax(170);slider.setProgress(Math.max(0,shown.getInt("size",75)-30));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int value,boolean user){if(user){shown.edit().putInt("size",value+30).apply();canvas.invalidate();changed.run();}}
            public void onStartTrackingTouch(SeekBar bar){}
            public void onStopTrackingTouch(SeekBar bar){}
        });root.addView(slider);
        android.widget.ScrollView scroller=new android.widget.ScrollView(activity);
        scroller.setFillViewport(false);scroller.addView(root);
        new AlertDialog.Builder(activity).setTitle("◈ PRISME Studio V6")
            .setView(scroller).setPositiveButton("Terminer",(d,w)->changed.run())
            .setNeutralButton("Réinitialiser",(d,w)->{
                SharedPreferences.Editor e=layout.edit();
                for(int i=0;i<4;i++)e.putInt(XS[i],DX[i]).putInt(YS[i],DY[i]);
                e.apply();changed.run();
            }).show();
    }
    private static final class Editor extends View{
        private final SharedPreferences layout,shown;private final Runnable changed;
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final RectF[] bounds=new RectF[4];
        private int active=-1;private float ox,oy;
        Editor(Context c,SharedPreferences layout,SharedPreferences shown,Runnable changed){
            super(c);this.layout=layout;this.shown=shown;this.changed=changed;
            for(int i=0;i<4;i++)bounds[i]=new RectF();
            setBackgroundColor(0xff0c1926);
        }
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);float w=getWidth(),h=getHeight();
            paint.setColor(0x334cc8b1);paint.setStrokeWidth(1);
            for(int i=1;i<10;i++){float x=i*w/10,y=i*h/10;c.drawLine(x,0,x,h,paint);c.drawLine(0,y,w,y,paint);}
            float cw=Math.min(w,Math.max(62,120*shown.getInt("size",75)/100f));
            float ch=Math.min(h,Math.max(32,48*shown.getInt("size",75)/100f));
            paint.setTextSize(14);paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            for(int i=0;i<4;i++){
                float x=Math.max(0,(w-cw))*layout.getInt(XS[i],DX[i])/100f;
                float y=Math.max(0,(h-ch))*layout.getInt(YS[i],DY[i])/100f;
                bounds[i].set(x,y,x+cw,y+ch);
                paint.setColor(shown.getBoolean(SHOW[i],false)?(i==active?0xff287c83:0xff244b57):0xff283544);
                c.drawRoundRect(bounds[i],9,9,paint);
                paint.setStyle(Paint.Style.STROKE);paint.setColor(i==active?0xff85fff1:0xff839eac);
                c.drawRoundRect(bounds[i],9,9,paint);paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xfff1ffff);
                c.drawText(LABEL[i],x+6,y+ch/2+5,paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent event){
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){
                for(int i=3;i>=0;i--)if(bounds[i].contains(event.getX(),event.getY())){
                    active=i;ox=event.getX()-bounds[i].left;oy=event.getY()-bounds[i].top;
                    if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                    invalidate();return true;
                }
                return false;
            }
            if(active<0)return false;
            if(action==MotionEvent.ACTION_MOVE||action==MotionEvent.ACTION_UP){
                RectF b=bounds[active];
                float rangeX=Math.max(1,getWidth()-b.width()),rangeY=Math.max(1,getHeight()-b.height());
                int x=5*Math.round(Math.max(0,Math.min(100,100*(event.getX()-ox)/rangeX))/5f);
                int y=5*Math.round(Math.max(0,Math.min(100,100*(event.getY()-oy)/rangeY))/5f);
                layout.edit().putInt(XS[active],x).putInt(YS[active],y).apply();
                invalidate();changed.run();
                if(action==MotionEvent.ACTION_UP){active=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
                return true;
            }
            if(action==MotionEvent.ACTION_CANCEL){active=-1;if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;}
            return true;
        }
    }
}
