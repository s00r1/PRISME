package fr.dm100.imoupaysage;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        handleLaunchIntent();
    }
    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent();
    }
    private void handleLaunchIntent() {
        if (!getIntent().getBooleanExtra("reglages", false) && Companion.permissions(this)) {
            activateAndLaunch("classique".equals(getIntent().getStringExtra("cible")) ? Companion.PHONE : Companion.HD);
            return;
        }
        render();
    }
    @Override public void onResume() {
        super.onResume();
        if (status != null) {
            updateStatus();
            if (Companion.enabled(this) && Companion.permissions(this)) {
                try { Companion.start(this); } catch (RuntimeException ignored) { }
            }
        }
    }
    private void render() {
        getWindow().setStatusBarColor(Color.rgb(15, 20, 28));
        getWindow().setNavigationBarColor(Color.rgb(15, 20, 28));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(15, 20, 28));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(22, 14, 22, 20);
        scroll.addView(body);
        TextView title = label("IMOU PAYSAGE", 28, Color.rgb(255, 150, 55));
        body.addView(title);
        status = label("", 19, Color.WHITE);
        body.addView(status);
        body.addView(label("L’écran reste horizontal dans Imou. Le compte et les caméras restent dans l’application officielle.", 17, Color.rgb(190, 201, 214)));
        row(body, "Activer et ouvrir Imou HD", new View.OnClickListener() {
            @Override public void onClick(View v) { activateAndLaunch(Companion.HD); }
        });
        row(body, "Ouvrir Imou classique", new View.OnClickListener() {
            @Override public void onClick(View v) { activateAndLaunch(Companion.PHONE); }
        });
        row(body, "Arrêter le maintien en paysage", new View.OnClickListener() {
            @Override public void onClick(View v) {
                Companion.stop(MainActivity.this);
                updateStatus();
                Toast.makeText(MainActivity.this, "Maintien arrêté", Toast.LENGTH_SHORT).show();
            }
        });
        if (!Settings.canDrawOverlays(this)) {
            row(body, "Autoriser la superposition", new View.OnClickListener() {
                @Override public void onClick(View v) { openSetting(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
            });
        }
        if (!Companion.usageAllowed(this)) {
            row(body, "Autoriser l’accès d’utilisation", new View.OnClickListener() {
                @Override public void onClick(View v) { openSetting(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)); }
            });
        }
        body.addView(label("L’accès d’utilisation sert uniquement à savoir quand Imou est au premier plan. Aucune connexion Internet, aucun mot de passe demandé.", 15, Color.rgb(151, 165, 184)));
        setContentView(scroll);
        updateStatus();
    }
    private TextView label(String text, int px, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
        t.setPadding(0, 4, 0, 9);
        return t;
    }
    private void row(LinearLayout body, String text, View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_PX, 18);
        b.setMinHeight(0); b.setMinimumHeight(0);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 50);
        p.bottomMargin = 5;
        body.addView(b, p); b.setOnClickListener(click);
    }
    private void updateStatus() {
        status.setText(!Companion.permissions(this) ? "Deux autorisations sont nécessaires ci-dessous."
                : Companion.enabled(this) ? "Activé · uniquement dans Imou" : "Prêt · maintien arrêté");
    }
    private void openSetting(Intent i) {
        // Remove the small system window before a sensitive permission screen.
        stopService(new Intent(this, LandscapeService.class));
        try { startActivity(i); }
        catch (ActivityNotFoundException missing) { Toast.makeText(this, "Ouvre les autorisations depuis les paramètres de la montre.", Toast.LENGTH_LONG).show(); }
    }
    private void activateAndLaunch(String target) {
        if (!Companion.permissions(this)) {
            if (status == null) render(); else updateStatus();
            Toast.makeText(this, "Accorde les deux autorisations ci-dessous.", Toast.LENGTH_LONG).show();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(target);
        if (launch == null) {
            if (status == null) render();
            Toast.makeText(this, "Cette version d’Imou n’est pas installée.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Companion.enabled(this, true);
            Companion.start(this);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            finish();
        } catch (RuntimeException problem) {
            Companion.stop(this);
            if (status == null) render(); else updateStatus();
            Toast.makeText(this, "Le maintien n’a pas démarré. Réessaie depuis cet écran.", Toast.LENGTH_LONG).show();
        }
    }
}
