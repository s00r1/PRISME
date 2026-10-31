package fr.dm100.prisme;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Scalable native icons; no fonts, downloaded images or newer rendering APIs. */
public final class ControlCenterIcon extends View {
    private final String id;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private int ink=0xfff4f8ff;
    private Drawable drawable;
    public ControlCenterIcon(Context context,String id){
        super(context);this.id=id==null?"apps":id;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        try{
            if(this.id.startsWith("app:")){
                ComponentName component=ComponentName.unflattenFromString(this.id.substring(4));
                if(component!=null)drawable=context.getPackageManager().getActivityIcon(component);
            } else {
                int resource=resource(this.id);
                if(resource!=0)drawable=context.getResources().getDrawable(resource).mutate();
            }
        }catch(Exception ignored){}
    }
    public void setInk(int color){if(ink!=color){ink=color;invalidate();}}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if(drawable!=null){
            int size=Math.max(1,Math.min(getWidth(),getHeight())-2),left=(getWidth()-size)/2,top=(getHeight()-size)/2;
            drawable.setBounds(left,top,left+size,top+size);
            if(!id.startsWith("app:"))drawable.setColorFilter(ink,PorterDuff.Mode.SRC_IN);
            drawable.draw(canvas);return;
        }
        canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);
        float scale=Math.min(getWidth(),getHeight())/32f;canvas.scale(scale,scale);
        p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.2f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(id){
            case "wifi":case "hotspot":
                arc(canvas,-14,-9,14,19,222,96);arc(canvas,-9,-4,9,14,222,96);arc(canvas,-4,1,4,9,222,96);
                p.setStyle(Paint.Style.FILL);canvas.drawCircle(0,9,1.7f,p);break;
            case "bluetooth":
                line(canvas,-8,-8,8,7);line(canvas,8,7,0,14);line(canvas,0,14,0,-14);line(canvas,0,-14,8,-7);line(canvas,8,-7,-8,8);break;
            case "airplane":{
                Path path=new Path();path.moveTo(-2,-13);path.lineTo(2,-13);path.lineTo(3,-3);path.lineTo(13,4);
                path.lineTo(13,7);path.lineTo(3,3);path.lineTo(2,11);path.lineTo(6,14);path.lineTo(-6,14);
                path.lineTo(-2,11);path.lineTo(-3,3);path.lineTo(-13,7);path.lineTo(-13,4);path.lineTo(-3,-3);path.close();canvas.drawPath(path,p);break;
            }
            case "sound":case "volume":case "volumepill":{
                Path speaker=new Path();speaker.moveTo(-13,-4);speaker.lineTo(-7,-4);speaker.lineTo(0,-10);speaker.lineTo(0,10);speaker.lineTo(-7,4);speaker.lineTo(-13,4);speaker.close();canvas.drawPath(speaker,p);
                arc(canvas,-3,-8,11,8,-60,120);arc(canvas,-6,-13,17,13,-60,120);break;
            }
            case "data":
                line(canvas,-6,-12,-6,11);line(canvas,-11,-7,-6,-12);line(canvas,-1,-7,-6,-12);
                line(canvas,6,-11,6,12);line(canvas,1,7,6,12);line(canvas,11,7,6,12);break;
            case "location":{
                Path pin=new Path();pin.moveTo(0,14);pin.cubicTo(-3,9,-10,2,-10,-4);pin.cubicTo(-10,-17,10,-17,10,-4);pin.cubicTo(10,2,3,9,0,14);canvas.drawPath(pin,p);canvas.drawCircle(0,-4,3,p);break;
            }
            case "rotation":
                arc(canvas,-12,-12,12,12,190,260);line(canvas,-12,-2,-12,-10);line(canvas,-12,-10,-4,-10);
                rect.set(-4,-7,4,7);canvas.drawRoundRect(rect,1.5f,1.5f,p);break;
            case "dnd":{
                Path moon=new Path();moon.moveTo(4,-12);moon.cubicTo(-14,-10,-13,13,3,13);moon.cubicTo(8,13,12,10,14,6);moon.cubicTo(1,10,-4,-2,4,-12);canvas.drawPath(moon,p);break;
            }
            case "screenlamp":
                rect.set(-7,-12,7,12);canvas.drawRoundRect(rect,2,2,p);line(canvas,-3,8,3,8);
                line(canvas,-12,-5,-15,-5);line(canvas,12,-5,15,-5);line(canvas,-11,-11,-14,-14);line(canvas,11,-11,14,-14);break;
            case "display":
                canvas.drawCircle(0,0,6,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;line(canvas,(float)Math.cos(a)*10,(float)Math.sin(a)*10,(float)Math.cos(a)*14,(float)Math.sin(a)*14);}break;
            case "music":{
                Path play=new Path();play.moveTo(-11,-9);play.lineTo(1,0);play.lineTo(-11,9);play.close();canvas.drawPath(play,p);
                line(canvas,7,-9,7,9);line(canvas,13,-9,13,9);break;
            }
            case "settings":{
                canvas.drawCircle(0,0,8,p);canvas.drawCircle(0,0,3,p);
                for(int i=0;i<8;i++){double a=i*Math.PI/4;line(canvas,(float)Math.cos(a)*9,(float)Math.sin(a)*9,(float)Math.cos(a)*13,(float)Math.sin(a)*13);}break;
            }
            case "clock":case "alarms":case "timer":case "stopwatch":
                canvas.drawCircle(0,1,11,p);line(canvas,0,1,0,-6);line(canvas,0,1,5,4);
                if(!"clock".equals(id)){line(canvas,-4,-13,4,-13);line(canvas,0,-13,0,-10);}break;
            case "apps":case "styles":case "search":case "capture":
                if("search".equals(id)){canvas.drawCircle(-3,-3,8,p);line(canvas,3,3,12,12);break;}
                for(int row=0;row<2;row++)for(int col=0;col<2;col++){rect.set(-12+col*14,-12+row*14,-2+col*14,-2+row*14);canvas.drawRoundRect(rect,2,2,p);}break;
            default:
                rect.set(-10,-10,10,10);canvas.drawRoundRect(rect,4,4,p);line(canvas,-5,0,5,0);line(canvas,0,-5,0,5);break;
        }
        canvas.restore();
    }
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
    private void arc(Canvas c,float l,float t,float r,float b,float start,float sweep){rect.set(l,t,r,b);c.drawArc(rect,start,sweep,false,p);}
    private static int resource(String id){
        switch(id){
            case "battery":return android.R.drawable.ic_lock_idle_charging;
            case "camera":case "capture":return android.R.drawable.ic_menu_camera;
            case "calculator":return android.R.drawable.ic_menu_manage;
            case "notes":return android.R.drawable.ic_menu_edit;
            case "vpn":return android.R.drawable.ic_lock_lock;
            case "accessibility":return android.R.drawable.ic_menu_myplaces;
            default:return 0;
        }
    }
}
