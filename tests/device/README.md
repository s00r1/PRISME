# Tests sur la DM100 réelle

Ce paquet d’instrumentation séparé ne fait pas partie de l’APK utilisateur. Il doit être signé avec la même clé que PRISME. Il teste les modèles dans le processus Android : import V5 réel si un export local existe, refus d’un profil invalide, journal de restauration, chiffrement Android 7, pause/reprise et livraison d’alarme, puis commandes d’une session multimédia synthétique.

Il conserve les préférences en mémoire et les rétablit dans un bloc `finally`. Sauvegardez néanmoins votre configuration avant un essai : une interruption complète du processus peut empêcher ce bloc de finir. Si huit minuteurs existent déjà, libérez un emplacement de test. Les notifications de test sont retirées et les alarmes existantes reprogrammées à la fin.

Depuis la racine du dépôt, compiler avec le même script et les mêmes paramètres SDK/JDK que PRISME :

```powershell
./build.ps1 -ProjectDirectory tests/device -Keystore $env:PRISME_KEYSTORE -OutputFile PRISME-QA.apk
adb install -r tests/device/PRISME-QA.apk
adb shell am instrument -w fr.dm100.prisme.qa/.WatchValidation
adb uninstall fr.dm100.prisme.qa
```

Les mots de passe de signature passent par les variables documentées dans BUILD.md. Ne publiez pas ce paquet avec la Release utilisateur.

L’essai multimédia enrichi requiert l’accès Android aux notifications accordé à PRISME. Sans cet accès, le rapport signale ce sous-test comme non vérifié. Rétablissez ensuite le choix d’autorisation initial. Le test ne lit ni ne journalise les messages des notifications.

`INSTRUMENTATION_CODE: -1` correspond à Activity.RESULT_OK ; lire aussi `result=PASS` et `preferences=restored`. Un échec bloque la validation de la livraison jusqu’à résolution.

## Météo et captures des 22 styles

`adb shell am instrument -w fr.dm100.prisme.qa/.WeatherValidation` teste une recherche de Paris et une prévision HTTPS réelle sur Android, puis le cache et la désactivation. Les trois magasins météo sont rétablis dans `finally`. Ce test active temporairement le réseau et nécessite Internet ; Paris est une fixture de test, pas la position de l’utilisateur.

`VisualFixture` propose `-e action save`, `-e action style -e id 0` (0 à21), `-e action restore`. Créez d’abord le point privé, capturez chaque bureau et terminez toujours par restore. Les captures du catalogue installé sont privées : ne les publiez pas. La restauration réactive le bureau choisi et conserve la disposition des cartes. Ce harnais ne remplace pas les gestes au doigt ni l’inspection des images.
