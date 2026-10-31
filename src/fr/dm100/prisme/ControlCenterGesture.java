package fr.dm100.prisme;

/** State machine for edge gestures. Independent of Android, drawing and application actions. */
public final class ControlCenterGesture {
    public static final int PASS = 0, CLAIM = 1, OPEN = 2;
    private static final long DOUBLE_TAP_MS = 360, TAP_MS = 300;
    private static final float SLOP = 12;
    private int edge, opening;
    private float distance, startX, startY;
    private boolean candidate;
    private long downAt, firstTapAt;
    private float firstTapX, firstTapY;

    public boolean begin(float x, float y, float width, float height, long at,
                         boolean enabled, int edge, int edgeSize, int opening, int swipeDistance) {
        if (this.edge != Math.max(0, Math.min(3, edge)) || this.opening != Math.max(0, Math.min(3, opening))) firstTapAt = 0;
        this.edge = Math.max(0, Math.min(3, edge));
        this.opening = Math.max(0, Math.min(3, opening));
        distance = Math.max(24, swipeDistance);
        startX = x; startY = y; downAt = at;
        candidate = enabled && this.opening != 2 && isInEdge(x, y, width, height, this.edge, Math.max(8, edgeSize));
        if (!candidate) firstTapAt = 0;
        return candidate;
    }
    public boolean isCandidate() { return candidate; }
    public int move(float x, float y) {
        if (!candidate) return PASS;
        float dx = x - startX, dy = y - startY;
        float along = inwardDistance(edge, dx, dy), across = perpendicularDistance(edge, dx, dy);
        if (opening != 1 && along >= distance && along > Math.abs(across) * 1.25f) {
            candidate = false; firstTapAt = 0; return OPEN;
        }
        if (along < -SLOP || Math.abs(across) > SLOP && Math.abs(across) > Math.abs(along) * 1.15f) {
            candidate = false; firstTapAt = 0; return PASS;
        }
        return CLAIM;
    }
    public int end(float x, float y, long at) {
        int moved = move(x, y);
        if (moved != CLAIM) return moved;
        candidate = false;
        if (opening != 1) return PASS;
        boolean tap = at >= downAt && at - downAt <= TAP_MS
                && Math.abs(x - startX) <= SLOP && Math.abs(y - startY) <= SLOP;
        if (!tap) { firstTapAt = 0; return CLAIM; }
        if (firstTapAt > 0 && at - firstTapAt <= DOUBLE_TAP_MS && at >= firstTapAt
                && Math.abs(x - firstTapX) <= 32 && Math.abs(y - firstTapY) <= 32) {
            firstTapAt = 0; return OPEN;
        }
        firstTapAt = at; firstTapX = x; firstTapY = y;
        return CLAIM;
    }
    public void cancel() { candidate = false; firstTapAt = 0; }
    public static boolean isInEdge(float x, float y, float width, float height, int edge, float thickness) {
        if (x < 0 || y < 0 || x > width || y > height) return false;
        if (edge == 0) return y <= thickness;
        if (edge == 1) return y >= height - thickness;
        if (edge == 2) return x <= thickness;
        return x >= width - thickness;
    }
    public static float inwardDistance(int edge, float dx, float dy) {
        return edge == 0 ? dy : edge == 1 ? -dy : edge == 2 ? dx : -dx;
    }
    public static float perpendicularDistance(int edge, float dx, float dy) {
        return edge <= 1 ? dx : dy;
    }
    public static boolean isClosingSwipe(int panelPosition, float dx, float dy, float threshold) {
        float outward = -inwardDistance(panelPosition, dx, dy);
        return outward >= Math.max(24, threshold)
                && outward > Math.abs(perpendicularDistance(panelPosition, dx, dy)) * 1.25f;
    }
    public static final class PanelBounds {
        public final int left, top, width, height;
        PanelBounds(int left, int top, int width, int height) {
            this.left = left; this.top = top; this.width = width; this.height = height;
        }
    }
    public static PanelBounds panelBounds(int hostWidth, int hostHeight, int widthPercent, int heightPercent, int position) {
        int hostW = Math.max(1, hostWidth), hostH = Math.max(1, hostHeight);
        int width = Math.max(1, Math.min(hostW, Math.round(hostW * Math.max(1, Math.min(100, widthPercent)) / 100f)));
        int height = Math.max(1, Math.min(hostH, Math.round(hostH * Math.max(1, Math.min(100, heightPercent)) / 100f)));
        int left = (hostW - width) / 2, top = (hostH - height) / 2;
        if (position == 0) top = 0;
        else if (position == 1) top = hostH - height;
        else if (position == 2) left = 0;
        else if (position == 3) left = hostW - width;
        return new PanelBounds(left, top, width, height);
    }
}
