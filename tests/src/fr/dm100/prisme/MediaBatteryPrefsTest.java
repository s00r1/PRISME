package fr.dm100.prisme;
public final class MediaBatteryPrefsTest {
    static int checks;
    static void check(boolean ok){checks++;if(!ok)throw new AssertionError("check "+checks);}
    public static void main(String[] args){
        DashboardPrefsTest.MemoryContext c=new DashboardPrefsTest.MemoryContext();
        MusicPrefs m=new MusicPrefs(c);BatteryIndicatorPrefs b=new BatteryIndicatorPrefs(c);
        String[] keys={"style","size","opacity","x","y"};
        int[] mins={0,50,30,0,0},maxs={11,180,100,100,100};
        for(int i=0;i<keys.length;i++){
            m.putInt(keys[i],Integer.MIN_VALUE);check(m.number(keys[i],70)==mins[i]);
            m.putInt(keys[i],Integer.MAX_VALUE);check(m.number(keys[i],70)==maxs[i]);
            m.store().edit().putString(keys[i],"broken").apply();
            check(m.number(keys[i],mins[i])==mins[i]);
        }
        String[] bk={"style","scope","palette","size","opacity","x","y","low","high"};
        int[] lo={0,0,0,35,25,0,0,5,45},hi={23,2,11,200,100,100,100,40,90};
        for(int i=0;i<bk.length;i++){
            b.putInt(bk[i],Integer.MIN_VALUE);check(b.integer(bk[i],60)==lo[i]);
            b.putInt(bk[i],Integer.MAX_VALUE);check(b.integer(bk[i],60)==hi[i]);
            b.preferences().edit().putString(bk[i],"broken").apply();
            check(b.integer(bk[i],lo[i])==lo[i]);
        }
        for(int mode=-1;mode<=3;mode++){
            b.setDisplayMode(mode);check(b.displayMode()==Math.max(0,Math.min(2,mode)));
            check(b.enabled()==(mode>0));
        }
        b.preferences().edit().putString("enabled","broken").apply();check(b.bool("enabled",false)==false);
        m.store().edit().putString("enabled","broken").apply();check(!m.flag("enabled",false));
        m.putBoolean("enabled",true);b.putBoolean("enabled",false);check(m.flag("enabled",false));check(!b.enabled());
        b.putInt("low",20);b.putInt("high",65);b.putBoolean("chargingColor",true);
        for(int palette=0;palette<12;palette++){
            b.putInt("palette",palette);check(b.color(20,false)!=b.color(21,false));
            check(b.color(64,false)!=b.color(65,false));check(b.color(94,false)!=b.color(95,false));
            check(b.color(0,true)==b.color(100,true));
        }
        System.out.println("MediaBatteryPrefsTest: "+checks+" assertions passed.");
    }
}
