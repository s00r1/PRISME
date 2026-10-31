package android.content;
/** Documented flatten/unflatten shape, with the Android relative-class expansion. */
public final class ComponentName {
    private final String pkg, cls;
    private ComponentName(String pkg, String cls) { this.pkg = pkg; this.cls = cls; }
    public static ComponentName unflattenFromString(String id) {
        int split = id.indexOf('/');
        if (split < 0 || split + 1 >= id.length()) return null;
        String pkg = id.substring(0, split), cls = id.substring(split + 1);
        if (cls.charAt(0) == '.') cls = pkg + cls;
        return new ComponentName(pkg, cls);
    }
    public String flattenToString() { return pkg + "/" + cls; }
    public String getPackageName() { return pkg; }
    public String getClassName() { return cls; }
}
