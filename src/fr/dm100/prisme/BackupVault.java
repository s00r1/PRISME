package fr.dm100.prisme;

import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
import javax.crypto.*;
import javax.crypto.spec.*;
import org.json.*;

/** Portable password-protected exports, independent of a device-bound keystore. */
public final class BackupVault {
  private BackupVault() {}

  private static final int ITERATIONS = 120000;

  private static byte[] random(int size) {
    byte[] b = new byte[size];
    new SecureRandom().nextBytes(b);
    return b;
  }

  private static SecretKey key(char[] password, byte[] salt) throws GeneralSecurityException {
    PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
    try {
      byte[] encoded =
          SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
      try {
        return new SecretKeySpec(encoded, "AES");
      } finally {
        Arrays.fill(encoded, (byte) 0);
      }
    } finally {
      spec.clearPassword();
    }
  }

  public static JSONObject seal(JSONObject profile, char[] password) throws Exception {
    if (password.length < 10) throw new GeneralSecurityException("10 caractères minimum");
    byte[] plain = profile.toString().getBytes(StandardCharsets.UTF_8);
    if (plain.length > 65536) throw new GeneralSecurityException("Sauvegarde trop grande");
    byte[] salt = random(16), iv = random(12);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
    cipher.updateAAD("PRISME-VAULT-1".getBytes(StandardCharsets.UTF_8));
    try {
      return new JSONObject()
          .put("prismeVault", 1)
          .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
          .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
          .put("data", Base64.encodeToString(cipher.doFinal(plain), Base64.NO_WRAP));
    } finally {
      Arrays.fill(plain, (byte) 0);
    }
  }

  public static JSONObject open(JSONObject vault, char[] password) throws Exception {
    if (vault.toString().length() > 100000
        || vault.length() != 4
        || !(vault.opt("prismeVault") instanceof Number)
        || ((Number) vault.get("prismeVault")).doubleValue() != 1
        || !(vault.opt("salt") instanceof String)
        || !(vault.opt("iv") instanceof String)
        || !(vault.opt("data") instanceof String))
      throw new GeneralSecurityException("Format chiffré invalide");
    byte[] salt = Base64.decode(vault.getString("salt"), Base64.NO_WRAP),
        iv = Base64.decode(vault.getString("iv"), Base64.NO_WRAP),
        data = Base64.decode(vault.getString("data"), Base64.NO_WRAP);
    if (salt.length != 16 || iv.length != 12 || data.length > 65552 || data.length < 16)
      throw new GeneralSecurityException("Format chiffré invalide");
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE, key(password, salt), new GCMParameterSpec(128, iv));
    cipher.updateAAD("PRISME-VAULT-1".getBytes(StandardCharsets.UTF_8));
    byte[] plain = cipher.doFinal(data);
    try {
      return new JSONObject(new String(plain, StandardCharsets.UTF_8));
    } finally {
      Arrays.fill(plain, (byte) 0);
    }
  }
}
