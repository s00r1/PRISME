package fr.dm100.prisme;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Cross-check Android's battery broadcast, fuel-gauge capacity and (when readable)
 * the standard Linux power-supply gauge. No fake percentages: -1 means unknown.
 */
public final class BatteryLevelReader {
    private BatteryLevelReader(){}

    public static final class Reading {
        public final int level, broadcast, property, sysfs, raw, scale;
        public final boolean charging;
        public final String source;
        Reading(int level,int broadcast,int property,int sysfs,int raw,int scale,
                boolean charging,String source) {
            this.level=level;this.broadcast=broadcast;this.property=property;this.sysfs=sysfs;
            this.raw=raw;this.scale=scale;this.charging=charging;this.source=source;
        }
        public String diagnostic(){
            return "Diffusion Android : "+raw+" / "+scale+" → "+format(broadcast)
                    +"\nCapacité BatteryManager : "+format(property)
                    +"\nCapacité système (sysfs) : "+format(sysfs)
                    +"\nCapacité charge/charge_full : "+format(readChargeRatio())
                    +"\nValeur retenue : "+format(level)+" ("+source+")"
                    +"\n\nSi toutes les sources indiquent 1 % alors que la montre est chargée,"
                    +" le firmware fournit une mesure incorrecte. Une vérification ADB sera nécessaire.";
        }
        private String format(int value){return value<0?"indisponible":value+" %";}
    }

    public static Reading read(Context context,Intent event){
        Intent status=event;
        if(status==null||!Intent.ACTION_BATTERY_CHANGED.equals(status.getAction())){
            try{status=context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));}
            catch(RuntimeException ignored){status=null;}
        }
        int raw=-1,scale=-1,broadcast=-1;
        boolean charging=false;
        if(status!=null){
            raw=status.getIntExtra(BatteryManager.EXTRA_LEVEL,-1);
            scale=status.getIntExtra(BatteryManager.EXTRA_SCALE,-1);
            if(raw>=0&&scale>0&&raw<=scale)
                broadcast=clamp(Math.round(raw*100f/scale));
            int state=status.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
            charging=state==BatteryManager.BATTERY_STATUS_CHARGING||
                    state==BatteryManager.BATTERY_STATUS_FULL;
        }
        int property=-1;
        try{
            BatteryManager manager=(BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
            if(manager!=null){
                int capacity=manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if(capacity>=0&&capacity<=100)property=capacity;
                if(status==null)charging=manager.isCharging();
            }
        }catch(RuntimeException ignored){}
        int sysfs=readSysfs();
        int cellRatio=readChargeRatio();
        int result=-1;
        String source="indisponible";

        if(broadcast>=0&&property>=0){
            if(Math.abs(broadcast-property)<=3){result=broadcast;source="diffusion Android";}
            else if(sysfs>=0&&Math.abs(sysfs-property)<=3){result=property;source="BatteryManager confirmé";}
            else if(sysfs>=0&&Math.abs(sysfs-broadcast)<=3){result=broadcast;source="diffusion confirmée";}
            else if(broadcast<=1&&property>4){result=property;source="BatteryManager (diffusion incohérente)";}
            else if(property<=1&&broadcast>4){result=broadcast;source="diffusion (capacité incohérente)";}
            else{result=property;source="BatteryManager";}
        }else if(property>=0){result=property;source="BatteryManager";}
        else if(broadcast>=0){result=broadcast;source="diffusion Android";}
        if(result<=1&&cellRatio>4){
            result=cellRatio;source="charge réelle / capacité nominale (jauge système)";
        }else if(result<=1&&sysfs>4&&(property<0||property<=1)&&(broadcast<0||broadcast<=1)){
            result=sysfs;source="jauge système (autres sources à 1 %)";
        }else if(result<0&&sysfs>=0){result=sysfs;source="jauge système";}
        // A charging system cannot be assumed to be full. Never fake 100% based on status.
        if(result==1&&status!=null&&
                status.getIntExtra(BatteryManager.EXTRA_STATUS,-1)==BatteryManager.BATTERY_STATUS_FULL){
            result=-1;source="mesures contradictoires (statut plein / capacité 1 %)";
        }
        return new Reading(result,broadcast,property,sysfs,raw,scale,charging,source);
    }
    private static int clamp(int value){return Math.max(0,Math.min(100,value));}

    private static Long readPositive(String path){
        File file=new File(path);
        if(!file.isFile()||!file.canRead())return null;
        try(BufferedReader reader=new BufferedReader(new FileReader(file))){
            String s=reader.readLine();
            if(s==null||s.length()>20)return null;
            long v=Long.parseLong(s.trim());
            return v>0?v:null;
        }catch(Exception ignored){return null;}
    }
    private static int readChargeRatio(){
        File base=new File("/sys/class/power_supply");
        File[] supplies=base.listFiles();
        if(supplies==null)return -1;
        int checked=0;
        for(File dir:supplies){
            if(++checked>16)break;
            if(dir==null||!dir.isDirectory())continue;
            String name=dir.getName().toLowerCase(java.util.Locale.ROOT);
            if(!name.contains("bat")&&!name.contains("bms"))continue;
            for(String[] pair:new String[][]{{"charge_now","charge_full"},{"energy_now","energy_full"},{"charge_counter","charge_full"}}){
                Long now=readPositive(new File(dir,pair[0]).getPath()),full=readPositive(new File(dir,pair[1]).getPath());
                if(now!=null&&full!=null&&full>0&&now<=full*2L){
                    int pct=clamp(Math.round(100d*now/full)>100?100:(int)Math.round(100d*now/full));
                    return pct;
                }
            }
        }
        return -1;
    }
    private static int readSysfs(){
        String[] paths={"/sys/class/power_supply/battery/capacity",
                "/sys/class/power_supply/Battery/capacity",
                "/sys/class/power_supply/bms/capacity"};
        for(String path:paths){
            File file=new File(path);
            if(!file.isFile()||!file.canRead())continue;
            try(BufferedReader reader=new BufferedReader(new FileReader(file))){
                String value=reader.readLine();
                if(value==null||value.length()>5)continue;
                int percent=Integer.parseInt(value.trim());
                if(percent>=0&&percent<=100)return percent;
            }catch(Exception ignored){}
        }
        File base=new File("/sys/class/power_supply");
        File[] supplies=base.listFiles();
        if(supplies!=null){
            int checked=0;
            for(File supply:supplies){
                if(++checked>16)break;
                if(supply==null||!supply.isDirectory())continue;
                String name=supply.getName().toLowerCase(java.util.Locale.ROOT);
                if(!name.contains("bat")&&!name.contains("bms"))continue;
                Long value=readPositive(new File(supply,"capacity").getPath());
                if(value!=null&&value<=100)return value.intValue();
            }
        }
        return -1;
    }
}
