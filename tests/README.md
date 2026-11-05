# Tests PRISME 6

Depuis la racine du dépôt :

```powershell
python scripts/fetch-test-dependency.py
./tests/run-all.ps1
node tests/home-model.cjs
node tests/dashboard-widget-model.cjs
node tests/website.cjs
python tests/api25_audit.py --self-test
```

JDK17+ et PowerShell7+ requis. La dépendance JSON de test est épinglée et son empreinte vérifiée ; aucun SDK Android ni montre n’est requis pour les modèles. Ces tests emploient le code de production avec des adaptateurs mémoire et ne modifient pas les données utilisateur.

## Ce qui est vérifié

- Préférences historiques et validation des profils ; recherche, checklist, 22 styles et configuration d’écran.
- Géométrie sur 378 configurations de modèle, gestes, volume flottant et bord à bord.
- Cœur V6 : grille et collisions, import/refus atomique, journal de récupération, annulation, temps monotone et redémarrage simulé, pause sous la seconde, calculs et chiffrement (mot de passe erroné et fichiers altérés).
- Météo : Celsius/Fahrenheit, horodatage/cache, données incomplètes, codes inconnus, bornes et refus de localisation/consentement/endpoint importés.
- Widgets : schéma strict, géométrie dédiée, filtres par style, identités stables après réordre/suppression, mutations atomiques, historique annulation/rétablissement et préférences corrompues.
- JavaScript : modèles cartes/widgets et galerie (chargements concurrents, erreurs HTTP, reprise et zoom).

La suite courante exécute **67 663 assertions Java**, **1 775 cartes JS** et **123 widgets JS** et **314 profils PC**, ainsi que les scénarios de galerie. Les nombres décrivent des cas de modèle, pas des configurations physiquement essayées.

## Android et vérification visuelle

[Paquet instrumentation](device/README.md) séparé : import V5, alarme, chiffrement et session musicale synthétique sur Android 7.1. `WeatherValidation` fait de vrais appels HTTPS et rétablit ses préférences. `VisualFixture` sert à parcourir les 22 styles en conservant un point de retour privé.

`api25_audit.py --history PATH --apk PATH` vérifie les références du DEX final contre l’historique public du SDK. Les références API 26 ne sont admises que dans des helpers isolés, avec contrôles des gardes source et appelants DEX. Ses 16 contrôles internes ne remplacent pas les essais Android.

Les parcours Edge réels, le ZIP `file://`, les captures et leurs limites sont décrits dans [V6-VALIDATION.md](../docs/V6-VALIDATION.md). La validation de modèle ne prouve pas autonomie, fluidité, compatibilité de tous les lecteurs ou comportement du firmware.
