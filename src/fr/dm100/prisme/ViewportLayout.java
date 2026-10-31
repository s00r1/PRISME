package fr.dm100.prisme;

/** Pure geometry shared by drawing, touch and diagnostics. All dimensions are physical pixels. */
public final class ViewportLayout {
    public final float surfaceWidth, surfaceHeight;
    public final float contentLeft, contentTop, contentWidth, contentHeight;
    public final float navigationTop, navigationHeight;
    public final float scale, logicalWidth, logicalHeight, styleHeight, toolbarExtra;

    public ViewportLayout(float width, float height, int zoomPercent, int insetPixels,
                          int fitMode, boolean reserveVolumeRail) {
        this(width,height,zoomPercent,insetPixels,fitMode,reserveVolumeRail,false);
    }
    public ViewportLayout(float width, float height, int zoomPercent, int insetPixels,
                          int fitMode, boolean reserveVolumeRail, boolean compactNavigation) {
        surfaceWidth = Math.max(1f, width);
        surfaceHeight = Math.max(1f, height);
        navigationHeight = Math.min(compactNavigation ? 44f : 56f, Math.max(1f, surfaceHeight * .22f));
        navigationTop = surfaceHeight - navigationHeight;
        float inset = Math.max(0, Math.min(24, insetPixels));
        inset = Math.min(inset, Math.max(0, Math.min(surfaceWidth, navigationTop) / 4f));
        float rail = reserveVolumeRail ? Math.min(64f, surfaceWidth * .22f) : 0;
        contentLeft = inset;
        contentTop = inset;
        contentWidth = Math.max(1f, surfaceWidth - inset * 2 - rail);
        contentHeight = Math.max(1f, navigationTop - inset * 2);
        // A logical stage of at least 640px high keeps all twenty layouts usable on a short screen.
        // Zoom changes the logical space available, so grids reflow instead of being cut off.
        float base = Math.min(1f, Math.min(contentWidth / 360f, contentHeight / 640f));
        // Keep room for a 44px shortcut row even at the largest zoom on a small screen.
        base = Math.min(base, Math.max(.001f, (contentHeight - 44f) / 552f));
        if (fitMode == 1) base *= .9f;
        float zoom = Math.max(75, Math.min(140, zoomPercent)) / 100f;
        if (fitMode == 2) zoom = Math.min(1.4f, zoom * 1.1f);
        scale = Math.max(.001f, base * zoom);
        logicalWidth = contentWidth / scale;
        logicalHeight = contentHeight / scale;
        toolbarExtra = Math.max(0, 44f / scale - 54f);
        styleHeight = Math.max(1f, logicalHeight - toolbarExtra);
    }

    /** Extend decorative panels only; text and grid dimensions keep their safe layout. */
    public static float[] edgeCard(float x,float y,float width,float height,float stageW,float stageH) {
        float left=x,top=y,right=x+Math.max(0,width),bottom=y+Math.max(0,height);
        boolean full=x>=0&&x<=20&&right>=stageW-20&&right<=stageW+1;
        boolean tall=height>stageH*.3f&&width>48;
        if(full||tall&&x>=0&&x<=20)left=0;
        if(full||tall&&right>=stageW-20&&right<=stageW+1)right=stageW;
        if(full&&y>=0&&y<=20)top=0;
        if(full&&bottom>=stageH-20&&bottom<=stageH+1)bottom=stageH;
        return new float[]{left,top,right,bottom};
    }
    public float toViewX(float x) { return contentLeft + x * scale; }
    public float toViewY(float y) { return contentTop + y * scale; }
    public float toCanvasX(float x) { return (x - contentLeft) / scale; }
    public float toCanvasY(float y) { return (y - contentTop) / scale; }
    public boolean containsContent(float x, float y) {
        return x >= contentLeft && x < contentLeft + contentWidth
                && y >= contentTop && y < contentTop + contentHeight;
    }
    public float navigationCellLeft(int index) { return surfaceWidth * Math.max(0, Math.min(4, index)) / 4f; }
}
