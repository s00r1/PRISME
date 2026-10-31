package fr.dm100.prisme;
public final class EdgeToEdgeTest {
    static int n;
    static void check(boolean b,String label){n++;if(!b)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int[] size:new int[][]{{640,480},{480,640},{320,320}})for(int zoom:new int[]{75,100,110,140}){
            ViewportLayout old=new ViewportLayout(size[0],size[1],zoom,0,0,false);
            ViewportLayout compact=new ViewportLayout(size[0],size[1],zoom,0,0,false,true);
            check(compact.navigationHeight==44,"touchable compact navigation");
            check(compact.contentHeight>old.contentHeight,"more style area");
            check(compact.contentWidth==size[0],"all screen width available");
            float w=compact.logicalWidth,h=compact.logicalHeight;
            float[] header=ViewportLayout.edgeCard(16,16,w-32,190,w,h);
            check(header[0]==0&&header[1]==0&&header[2]==w,"header reaches both sides and top");
            float[] bar=ViewportLayout.edgeCard(12,h-64,w-24,54,w,h);
            check(bar[0]==0&&bar[2]==w&&bar[3]==h,"toolbar fills bottom edge");
            float[] small=ViewportLayout.edgeCard(32,90,44,44,w,h);
            check(small[0]==32&&small[1]==90&&small[2]==76&&small[3]==134,"small controls keep their exact bounds");
            float[] sidebar=ViewportLayout.edgeCard(15,70,90,h-170,w,h);
            check(sidebar[0]==0&&sidebar[2]==105,"sidebar extends only its outside edge");
            float[] button=ViewportLayout.edgeCard(30,10,80,46,w,h);
            check(button[0]==30&&button[1]==10,"short inset buttons remain untouched");
            float x=35,y=40;
            check(Math.abs(compact.toCanvasX(compact.toViewX(x))-x)<.001&&Math.abs(compact.toCanvasY(compact.toViewY(y))-y)<.001,"hit mapping is reversible");
        }
        ViewportLayout auto=new ViewportLayout(640,480,100,0,0,false);
        ViewportLayout comfort=new ViewportLayout(640,480,100,0,2,false);
        check(comfort.scale>auto.scale,"comfort mode actually increases size");
        System.out.println("EdgeToEdgeTest: "+n+" assertions passed.");
    }
}
