package fr.dm100.prisme.volume;

/** Physical-pixel geometry, independent of the watch firmware's reported application density. */
public final class FloatingVolumeGeometry {
    public static final int PLUS = 1, CENTER = 0, MINUS = -1, MUTE = 2;
    public final int screenWidth, screenHeight, width, height;
    public final boolean horizontal, collapsed;
    public final float cellSize;
    public final Cell[] cells;
    public static final class Cell {
        public final int role;
        public final float left, top, width, height;
        Cell(int role, float left, float top, float width, float height) {
            this.role = role; this.left = left; this.top = top; this.width = width; this.height = height;
        }
        public boolean contains(float x, float y) { return x >= left && x < left + width && y >= top && y < top + height; }
    }
    public FloatingVolumeGeometry(int screenWidth, int screenHeight, int requestedSize, int layout,
                                  boolean expanded, boolean showMute, int shape) {
        this.screenWidth = Math.max(1, screenWidth); this.screenHeight = Math.max(1, screenHeight);
        collapsed = layout == 2 && !expanded;
        int size = Math.max(40, Math.min(96, requestedSize));
        int count = collapsed ? 1 : showMute ? 4 : 3;
        float padding = 3, gap = shape == 1 || shape == 3 ? Math.max(3, size * .065f) : 1;
        float longSide = count * size + (count - 1) * gap + padding * 2;
        boolean row = layout == 1 || layout == 2;
        if (!collapsed && layout == 2 && longSide > this.screenWidth - 8 && longSide <= this.screenHeight - 8) row = false;
        horizontal = row;
        float desiredW = row ? longSide : size + padding * 2;
        float desiredH = row ? size + padding * 2 : longSide;
        float scale = Math.min(1f, Math.min(Math.max(1, this.screenWidth - 8) / desiredW, Math.max(1, this.screenHeight - 8) / desiredH));
        cellSize = Math.max(.1f, size * scale); padding *= scale; gap *= scale;
        width = Math.max(1, Math.min(this.screenWidth, (int)Math.ceil(desiredW * scale)));
        height = Math.max(1, Math.min(this.screenHeight, (int)Math.ceil(desiredH * scale)));
        cells = new Cell[count];
        int[] roles = collapsed ? new int[]{CENTER} : showMute ? new int[]{PLUS, CENTER, MINUS, MUTE} : new int[]{PLUS, CENTER, MINUS};
        for (int i = 0; i < count; i++) {
            float along = padding + i * (cellSize + gap);
            cells[i] = new Cell(roles[i], row ? along : padding, row ? padding : along, cellSize, cellSize);
        }
    }
    public int maxX() { return Math.max(0, screenWidth - width); }
    public int maxY() { return Math.max(0, screenHeight - height); }
    public int xAt(int percentage) { return Math.round(maxX() * bound(percentage) / 100f); }
    public int yAt(int percentage) { return Math.round(maxY() * bound(percentage) / 100f); }
    public int xPercent(int x) { return maxX() == 0 ? 0 : bound(Math.round(clamp(x, 0, maxX()) * 100f / maxX())); }
    public int yPercent(int y) { return maxY() == 0 ? 0 : bound(Math.round(clamp(y, 0, maxY()) * 100f / maxY())); }
    public int snapX(int x) { return x < maxX() / 2f ? 0 : maxX(); }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int bound(int percent) { return clamp(percent, 0, 100); }
}
