package fr.dm100.prisme;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** 24 distinct vector displays, drawn locally; no touch interception or external assets. */
public final class BatteryIndicatorView extends View {
    private final BatteryIndicatorPrefs prefs;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private int level=-1;
    private boolean charging;
    public BatteryIndicatorView(Context c,BatteryIndicatorPrefs prefs){super(c);this.prefs=prefs;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void update(int percent,boolean onCharge){level=percent<0?-1:Math.min(100,percent);charging=onCharge;invalidate();}
    public int desiredWidth(){return Math.round(156*prefs.integer("size",70)/100f);}
    public int desiredHeight(){return Math.round(56*prefs.integer("size",70)/100f);}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(desiredWidth(),desiredHeight());}
    private void rect(Canvas c,float l,float t,float r,float b,float radius,int color){
        p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);
    }
    private void outline(Canvas c,float l,float t,float r,float b,float radius,int color,float stroke){
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setColor(color);c.drawRoundRect(new RectF(l,t,r,b),radius,radius,p);p.setStyle(Paint.Style.FILL);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color,boolean bold){
        p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("monospace",Typeface.NORMAL));
        p.setTextSize(size);c.drawText(value,x,y,p);
    }
    private void circle(Canvas c,float x,float y,float r,int color){
        p.setColor(color);p.setStyle(Paint.Style.FILL);c.drawCircle(x,y,r,p);
    }
    private void ring(Canvas c,float x,float y,float r,float stroke,float start,float sweep,int color){
        p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawArc(new RectF(x-r,y-r,x+r,y+r),start,sweep,false,p);p.setStyle(Paint.Style.FILL);
    }
    private void line(Canvas c,float x1,float y1,float x2,float y2,float stroke,int color){
        p.setColor(color);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);p.setStyle(Paint.Style.STROKE);
        c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);
    }
    private void polygon(Canvas c,float[] points,int color,boolean stroke){
        Path path=new Path();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.close();
        p.setColor(color);p.setStyle(stroke?Paint.Style.STROKE:Paint.Style.FILL);p.setStrokeWidth(3);
        c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
    }
    @Override protected void onDraw(Canvas original){
        super.onDraw(original);
        Canvas c=original;
        int col=prefs.color(level,charging),soft=0x663b4c62,ink=0xffedf8ff;
        float ratio=Math.max(0,level)/100f;
        int style=prefs.integer("style",0);
        boolean percent=prefs.bool("percent",true);
        String digits=level<0?"--":String.valueOf(level);
        String label=percent?digits+"%":"";
        c.save();c.scale(getWidth()/156f,getHeight()/56f);
        if(prefs.bool("backdrop",true))rect(c,0,1,155,55,16,0xc9151b2b);
        switch(style){
            case 0: // horizontal system icon
                outline(c,12,13,84,43,6,col,3);rect(c,85,22,92,34,2,col);
                rect(c,17,18,17+62*ratio,38,3,col);text(c,label,101,36,19,ink,true);break;
            case 1: // glass pill
                rect(c,6,9,150,47,19,0x88465a74);rect(c,10,13,10+136*ratio,43,15,col);
                text(c,label,55,37,24,ink,true);break;
            case 2: // full ring
                ring(c,35,28,22,7,-90,360,soft);ring(c,35,28,22,7,-90,360*ratio,col);
                text(c,label,70,36,24,col,true);break;
            case 3: // arc of a dial
                ring(c,41,43,31,8,185,170,soft);ring(c,41,43,31,8,185,170*ratio,col);
                text(c,label,79,36,24,ink,true);break;
            case 4: // segments
                for(int i=0;i<10;i++)rect(c,9+i*11,14,17+i*11,43,3,i<Math.round(level/10f)?col:soft);
                text(c,label,121,35,13,ink,true);break;
            case 5: // vertical bar
                outline(c,15,7,43,49,6,col,3);rect(c,20,12+32*(1-ratio),38,44,3,col);
                text(c,label,58,38,28,ink,true);break;
            case 6: // percentage XXL
                text(c,label,12,43,39,col,true);break;
            case 7: // digits
                outline(c,6,10,149,47,5,col,1.4f);
                text(c,level<0?"---":(level<10?"00":level<100?"0":"")+level,14,37,28,col,true);
                text(c,percent?"PCT":"BATT",97,33,13,ink,false);break;
            case 8: // neon strips
                rect(c,7,10,148,46,12,0x66262442);outline(c,7,10,148,46,12,col,3);
                rect(c,13,40,13+129*ratio,44,2,col);text(c,label,47,34,25,col,true);break;
            case 9: // pixel blocks
                for(int i=0;i<8;i++){int x=9+i*14;rect(c,x,12,x+11,40,0,i<Math.round(ratio*8)?col:soft);}
                text(c,label,121,35,13,ink,true);break;
            case 10: // mechanical needle
                ring(c,39,42,30,6,190,160,soft);ring(c,39,42,30,6,190,160*ratio,col);
                double angle=Math.toRadians(190+160*ratio);
                line(c,39,42,39+(float)Math.cos(angle)*24,42+(float)Math.sin(angle)*24,3,col);
                text(c,label,80,36,24,ink,true);break;
            case 11: // super slim progress
                text(c,label,14,32,20,col,true);rect(c,12,38,143,43,3,soft);
                rect(c,12,38,12+131*ratio,43,3,col);break;
            case 12: // terminal prompt
                rect(c,4,6,151,50,5,0xff0a161a);text(c,"> BAT "+digits+(charging?"+":"")+"%",12,35,20,col,true);break;
            case 13: // hexagon
                polygon(c,new float[]{12,28,27,7,54,7,69,28,54,49,27,49},col,true);
                text(c,digits,23,35,18,ink,true);text(c,percent?"% CELL":"CELL",77,35,17,col,true);break;
            case 14: // energy bolt
                polygon(c,new float[]{32,5,16,29,32,28,23,51,52,22,36,24,44,5},col,false);
                text(c,label,68,37,28,ink,true);break;
            case 15: // wave
                for(int i=0;i<18;i++){float x=11+i*7.5f;float top=23-(float)Math.sin(i*.65)*9;
                    line(c,x,top,x,top+11,3,i/18f<ratio?col:soft);}
                text(c,label,109,48,14,col,true);break;
            case 16: // luminous dots
                for(int i=0;i<14;i++)circle(c,13+i*10,26,4.3f,i<Math.ceil(ratio*14)?col:soft);
                text(c,label,58,49,14,ink,true);break;
            case 17: // radar
                ring(c,35,28,22,2,0,360,soft);ring(c,35,28,15,2,0,360,soft);
                ring(c,35,28,22,5,-90,360*ratio,col);line(c,13,28,57,28,1,col);
                text(c,label,73,36,24,col,true);break;
            case 18: // shield
                polygon(c,new float[]{34,5,57,14,53,38,34,51,15,38,11,14},col,true);
                rect(c,22,34-19*ratio,46,34,3,col);text(c,label,73,36,23,ink,true);break;
            case 19: // matrix meter
                for(int i=0;i<12;i++)for(int j=0;j<3;j++)rect(c,9+i*9,11+j*11,15+i*9,18+j*11,0,i<Math.round(ratio*12)?col:soft);
                text(c,label,120,34,14,col,true);break;
            case 20: // cyber future
                polygon(c,new float[]{11,9,133,9,149,28,133,47,11,47,2,28},col,true);
                rect(c,14,40,14+112*ratio,44,1,col);text(c,label,48,34,25,col,true);break;
            case 21: // OLED
                text(c,label,9,39,35,col,false);circle(c,139,26,6,col);break;
            case 22: // hairline
                text(c,digits+(percent?"%":""),9,27,18,col,true);
                line(c,10,37,145,37,2,soft);line(c,10,37,10+135*ratio,37,4,col);break;
            default: // 23 chronograph
                for(int i=0;i<12;i++){double rad=Math.toRadians(i*30-90);
                    float x=33+(float)Math.cos(rad)*22,y=28+(float)Math.sin(rad)*22;
                    circle(c,x,y,2,i/12f<ratio?col:soft);}
                ring(c,33,28,14,4,-90,360*ratio,col);text(c,label,69,36,24,col,true);break;
        }
        if(charging&&prefs.bool("chargingMark",true))text(c,"⚡",132,17,13,col,true);
        c.restore();
    }
}
