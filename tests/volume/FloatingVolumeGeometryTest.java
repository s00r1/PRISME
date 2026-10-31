package fr.dm100.prisme.volume;

/** Bounds, hit targets and placement across actual and constrained watch displays. */
public final class FloatingVolumeGeometryTest {
    private static int assertions, configurations;
    private static void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        for(int[] screen:new int[][]{{640,480},{480,640},{320,320},{200,200},{200,640}})
        for(int size:new int[]{40,56,96})for(int layout=0;layout<3;layout++)
        for(boolean expanded:new boolean[]{false,true})for(boolean mute:new boolean[]{false,true})for(int shape=0;shape<4;shape++){
            configurations++;
            FloatingVolumeGeometry g=new FloatingVolumeGeometry(screen[0],screen[1],size,layout,expanded,mute,shape);
            check(g.width<=screen[0]&&g.height<=screen[1],"window stays on display");
            check(g.width>0&&g.height>0&&g.cellSize>0,"usable positive dimensions");
            check(g.cells.length==(layout==2&&!expanded?1:mute?4:3),"collapsed and expanded controls");
            int centers=0,plus=0,minus=0;
            for(int i=0;i<g.cells.length;i++){
                FloatingVolumeGeometry.Cell cell=g.cells[i];
                check(cell.left>=0&&cell.top>=0&&cell.left+cell.width<=g.width+.01f&&cell.top+cell.height<=g.height+.01f,"control inside window");
                check(cell.width==cell.height,"circle remains square");
                check(cell.contains(cell.left+cell.width/2,cell.top+cell.height/2),"center hit target");
                if(cell.role==0)centers++;if(cell.role==1)plus++;if(cell.role==-1)minus++;
                for(int j=0;j<i;j++){
                    FloatingVolumeGeometry.Cell b=g.cells[j];
                    check(cell.left>=b.left+b.width-.01f||cell.top>=b.top+b.height-.01f||b.left>=cell.left+cell.width-.01f||b.top>=cell.top+cell.height-.01f,"controls never overlap");
                }
            }
            check(centers==1&&plus==(g.collapsed?0:1)&&minus==(g.collapsed?0:1),"all required actions reachable");
            for(int percent:new int[]{0,25,50,75,100}){
                int x=g.xAt(percent),y=g.yAt(percent);
                check(x>=0&&x+g.width<=screen[0]&&y>=0&&y+g.height<=screen[1],"placement within display");
                check(Math.abs(g.xAt(g.xPercent(x))-x)<=1&&Math.abs(g.yAt(g.yPercent(y))-y)<=1,"saved position round trip");
            }
            check(g.snapX(0)==0&&g.snapX(g.maxX())==g.maxX(),"snap to reachable edges");
        }
        FloatingVolumeGeometry narrow=new FloatingVolumeGeometry(200,640,96,2,true,true,1);
        check(!narrow.horizontal,"deploy vertically when horizontal controls do not fit");
        check(narrow.cellSize==96,"fallback retains large touch targets");
        System.out.println("FloatingVolumeGeometryTest: "+assertions+" assertions passed; "+configurations+" configurations.");
    }
}
