package fr.dm100.prisme;

/** Regression tests for the display/touch contract, independent of Android rendering. */
public final class ViewportLayoutTest {
    private static int checks;
    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void near(float actual, float expected, String message) {
        require(Math.abs(actual - expected) < .005f, message + ": " + actual + " != " + expected);
    }
    private static void testSurface(int width, int height, int zoom, int inset, int mode, boolean rail) {
        ViewportLayout v = new ViewportLayout(width, height, zoom, inset, mode, rail);
        String id = width + "x" + height + "/" + zoom + "/" + inset + "/" + mode + "/" + rail;
        require(v.scale > 0 && !Float.isInfinite(v.scale) && !Float.isNaN(v.scale), "finite scale " + id);
        require(v.contentLeft >= 0 && v.contentTop >= 0, "nonnegative origin " + id);
        require(v.contentWidth > 0 && v.contentHeight > 0, "usable content " + id);
        require(v.contentLeft + v.contentWidth <= width + .005f, "content inside screen " + id);
        require(v.contentTop + v.contentHeight <= v.navigationTop + .005f, "content avoids fixed navigation " + id);
        near(v.navigationTop + v.navigationHeight, height, "navigation fills bottom " + id);
        require(v.navigationHeight >= 44, "navigation touch height " + id);
        require((54f + v.toolbarExtra) * v.scale >= 43.995f, "shortcut row keeps a usable height " + id);
        near(v.styleHeight + v.toolbarExtra, v.logicalHeight, "shortcut expansion has its own layout space " + id);
        if (width >= 480 && height >= 480)
            require(v.styleHeight >= 448f, "watch styles retain their minimum layout space " + id);
        for (int i = 0; i < 4; i++) {
            float left = v.navigationCellLeft(i), right = v.navigationCellLeft(i + 1);
            require(right - left >= 44, "navigation touch width " + id);
            require(left >= 0 && right <= width, "navigation stays on screen " + id);
        }
        for (float fx : new float[]{0f, .01f, .25f, .5f, .99f, 1f}) {
            for (float fy : new float[]{0f, .01f, .25f, .5f, .99f, 1f}) {
                float x = v.logicalWidth * fx, y = v.logicalHeight * fy;
                near(v.toCanvasX(v.toViewX(x)), x, "x touch roundtrip " + id);
                near(v.toCanvasY(v.toViewY(y)), y, "y touch roundtrip " + id);
                if (fx < 1f && fy < 1f)
                    require(v.containsContent(v.toViewX(x), v.toViewY(y)), "interior hit accepted " + id);
            }
        }
        require(!v.containsContent(width / 2f, v.navigationTop + v.navigationHeight / 2f), "footer not interpreted as style touch " + id);
        if (inset == 0 && !rail) {
            near(v.contentLeft, 0, "no default side band " + id);
            near(v.contentWidth, width, "full default width " + id);
        }
    }
    public static void main(String[] args) {
        int[][] sizes = {{640,480},{480,640},{400,400},{320,320},{240,240},{800,480},{480,800}};
        for (int[] size : sizes)
            for (int zoom : new int[]{75,100,140})
                for (int inset : new int[]{0,12,24})
                    for (int mode = 0; mode < 3; mode++)
                        for (boolean rail : new boolean[]{false,true})
                            testSurface(size[0], size[1], zoom, inset, mode, rail);
        ViewportLayout small = new ViewportLayout(640,480,75,0,0,false);
        ViewportLayout large = new ViewportLayout(640,480,140,0,0,false);
        require(large.scale > small.scale, "zoom increases actual drawing scale");
        require(large.logicalWidth < small.logicalWidth, "zoom allows layout reflow instead of crop");
        near(large.navigationTop, small.navigationTop, "zoom does not move navigation");
        for (float bad : new float[]{0f,-1f}) {
            ViewportLayout v = new ViewportLayout(bad,bad,100,24,0,false);
            require(v.scale > 0 && !Float.isNaN(v.logicalWidth), "layout before first measurement remains finite");
        }
        System.out.println("ViewportLayoutTest: " + checks + " assertions passed; 378 display configurations.");
    }
}
