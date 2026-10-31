package fr.dm100.prisme;

import android.content.ComponentName;
import android.graphics.Bitmap;

/** One launchable activity. Its stable identity survives label and theme changes. */
public final class AppEntry {
    public final String id;
    public final String packageName;
    public final String label;
    public final ComponentName component;
    public Bitmap icon;

    public AppEntry(ComponentName component, String label, Bitmap icon) {
        if (component == null) throw new IllegalArgumentException("Composant manquant");
        this.component = component;
        this.id = component.flattenToString();
        this.packageName = component.getPackageName();
        this.label = label == null || label.trim().length() == 0 ? packageName : label.trim();
        this.icon = icon;
    }

    public AppEntry(ComponentName component, String label) {
        this(component, label, null);
    }
}
