package fr.dm100.prisme;

/** Offline regression checks: a first edge tap must never launch an application. */
public final class ControlCenterGestureTest {
    private static int assertions;
    private static void check(boolean value, String label) {
        assertions++;
        if (!value) throw new AssertionError(label);
    }
    private static float[] start(int edge, float w, float h) {
        return edge == 0 ? new float[]{w / 2, 3} : edge == 1 ? new float[]{w / 2, h - 3}
                : edge == 2 ? new float[]{3, h / 2} : new float[]{w - 3, h / 2};
    }
    private static float[] moved(int edge, float[] start, float amount, float across) {
        return edge == 0 ? new float[]{start[0] + across, start[1] + amount}
                : edge == 1 ? new float[]{start[0] + across, start[1] - amount}
                : edge == 2 ? new float[]{start[0] + amount, start[1] + across}
                : new float[]{start[0] - amount, start[1] + across};
    }
    public static void main(String[] args) {
        for (int[] size : new int[][]{{640, 480}, {480, 640}, {320, 320}}) for (int edge = 0; edge < 4; edge++) {
            float[] first = start(edge, size[0], size[1]);
            ControlCenterGesture g = new ControlCenterGesture();
            check(g.begin(first[0], first[1], size[0], size[1], 1000, true, edge, 40, 0, 55), "edge claimed");
            float[] next = moved(edge, first, 65, 3);
            check(g.move(next[0], next[1]) == ControlCenterGesture.OPEN, "inward swipe opens");
            g.begin(first[0], first[1], size[0], size[1], 2000, true, edge, 40, 3, 55);
            next = moved(edge, first, 2, 75);
            check(g.move(next[0], next[1]) == ControlCenterGesture.PASS, "perpendicular movement returns pagination or scrolling");
            check(g.end(next[0], next[1], 2200) == ControlCenterGesture.PASS, "released gesture never opens later");
            g.begin(first[0], first[1], size[0], size[1], 3000, true, edge, 40, 0, 55);
            next = moved(edge, first, -30, 0);
            check(g.move(next[0], next[1]) == ControlCenterGesture.PASS, "outward opening is rejected");
            check(!g.begin(first[0], first[1], size[0], size[1], 4000, false, edge, 40, 3, 55), "disabled never intercepts");
            check(!g.begin(first[0], first[1], size[0], size[1], 4000, true, edge, 40, 2, 55), "button-only never intercepts");
            check(!g.begin(size[0] / 2, size[1] / 2, size[0], size[1], 5000, true, edge, 40, 0, 55), "ordinary content is untouched");

            g.begin(first[0], first[1], size[0], size[1], 6000, true, edge, 40, 1, 55);
            check(g.end(first[0], first[1], 6030) == ControlCenterGesture.CLAIM, "first double-tap is consumed, not forwarded to an app");
            g.begin(first[0], first[1], size[0], size[1], 6150, true, edge, 40, 1, 55);
            check(g.end(first[0], first[1], 6180) == ControlCenterGesture.OPEN, "second edge tap opens");
            g.begin(first[0], first[1], size[0], size[1], 7000, true, edge, 40, 1, 55);
            check(g.end(first[0], first[1], 7030) == ControlCenterGesture.CLAIM, "new first tap consumed");
            g.cancel();
            g.begin(first[0], first[1], size[0], size[1], 7100, true, edge, 40, 1, 55);
            check(g.end(first[0], first[1], 7130) == ControlCenterGesture.CLAIM, "pause or pointer cancellation removes old first tap");
            g.begin(first[0], first[1], size[0], size[1], 8000, true, edge, 40, 1, 55);
            check(g.end(first[0], first[1], 8030) == ControlCenterGesture.CLAIM, "expired tap restarts the pair");
            g.begin(first[0], first[1], size[0], size[1], 8500, true, edge, 40, 0, 90);
            next = moved(edge, first, 89, 0);
            check(g.move(next[0], next[1]) == ControlCenterGesture.CLAIM, "configured swipe threshold is respected");
            next = moved(edge, first, 92, 0);
            check(g.end(next[0], next[1], 8700) == ControlCenterGesture.OPEN, "UP can complete a fast swipe");

            float[] outward = moved(edge, new float[]{0, 0}, -70, 0);
            check(ControlCenterGesture.isClosingSwipe(edge, outward[0], outward[1], 55), "close moves toward the panel's attached edge");
            float[] inward = moved(edge, new float[]{0, 0}, 70, 0);
            check(!ControlCenterGesture.isClosingSwipe(edge, inward[0], inward[1], 55), "opposite close gesture is ignored");
        }
        // The bottom hot edge is the content edge above fixed PRISME navigation.
        ControlCenterGesture g = new ControlCenterGesture();
        check(!g.begin(320, 465, 640, 424, 1000, true, 1, 40, 1, 55), "fixed navigation retains its first tap");
        check(g.begin(320, 420, 640, 424, 1000, true, 1, 40, 1, 55), "bottom gesture remains available above navigation");
        check(g.end(320, 420, 1020) == ControlCenterGesture.CLAIM, "bottom double-tap first step is consumed");
        g.begin(320, 420, 640, 424, 1100, true, 0, 480, 1, 55);
        check(g.end(320, 420, 1120) == ControlCenterGesture.CLAIM, "changing edge invalidates a pending pair");

        for (int[] size : new int[][]{{640, 480}, {480, 640}, {320, 320}, {1, 1}})
            for (int w : new int[]{65, 100}) for (int h : new int[]{55, 95, 100}) for (int position = 0; position < 4; position++) {
                ControlCenterGesture.PanelBounds b = ControlCenterGesture.panelBounds(size[0], size[1], w, h, position);
                check(b.width > 0 && b.height > 0, "panel has a measurable area");
                check(b.left >= 0 && b.top >= 0 && b.left + b.width <= size[0] && b.top + b.height <= size[1], "panel never escapes its host");
                check(position != 0 || b.top == 0, "top attached");
                check(position != 1 || b.top + b.height == size[1], "bottom attached");
                check(position != 2 || b.left == 0, "left attached");
                check(position != 3 || b.left + b.width == size[0], "right attached");
            }
        System.out.println("ControlCenterGestureTest: " + assertions + " assertions passed");
    }
}
