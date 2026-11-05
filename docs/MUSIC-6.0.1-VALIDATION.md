# Validation musique PRISME 6.0.1

**5 novembre 2026 · versionCode 25 · DM100 physique, Android 7.1.1 / API 25.** Ce rapport complète la [validation V6 initiale](V6-VALIDATION.md). Les inspections des 22 styles précédentes n’ont pas toutes été répétées pour ce correctif.

## Instrumentation réelle : 50 assertions réussies

`MusicValidation` utilise sa propre session **en pause**, sans audio focus ni commande au lecteur utilisateur. Sa préférence temporaire de lecteur et ses données sont restaurées dans `finally`.

| Groupe | Assertions |
| --- | ---: |
| URI locales admises ; réseau, fichier arbitraire et URI invalides refusés | 11 |
| Activation locale, accès Android et désactivation | 12 |
| Métadonnées, champs alternatifs, Unicode, images et changements de titre | 23 |
| Sélection et image via le gestionnaire Android réel | 3 |
| Restauration persistante des préférences | 1 |
| **Total** | **50** |

Les trois clés bitmap `DISPLAY_ICON`, `ART` et `ALBUM_ART` sont testées avec des aplats colorés. Les images 640 × 480 sont réduites à 256 pixels maximum. Une piste sans couverture retire l’ancienne image, l’artiste et l’album périmés ; des métadonnées vides ne conservent pas l’ancien titre.

**L’autorisation Android était accordée pendant cette passe.** Sans elle, le harnais peut tester l’extraction via son propre jeton, mais signale le gestionnaire `SKIPPED`. Il ne donne ni ne révoque d’autorisation système. [Commande et sources du harnais](../tests/device/README.md).

## Spotify et affichage

Une session **Spotify réellement active** a fourni titre, artiste et pochette `ALBUM_ART` de **200 × 200 pixels**, vérifiés dans les données de PRISME et à l’écran.

Inspectés physiquement : panneau musical, widget Musique et centre iOS. Le centre Android, le mini-lecteur et la carte compilent avec l’instantané partagé ; aucune nouvelle inspection physique exhaustive de ces surfaces n’est revendiquée.

Aucun nom de morceau écouté, artiste écouté, compte, historique ou capture musicale personnelle n’est publié.

## Compilation et modèles

La compilation du correctif réussit. La suite complète compte **67 663 assertions Java réussies**. L’audit du DEX contrôle **1 175 références publiques Android** : **0 incompatible non protégée**, **0 non résolue**. Cet audit statique ne prouve pas le comportement de tous les firmwares.

## Activation, migration et limites

Activer **Titres et pochettes** dans PRISME, puis accorder séparément l’accès Android à **PRISME · Sessions multimédias**. Ces choix ne sont pas importables. Les corps des notifications ne sont pas lus.

Les images restent en mémoire, sans export ni historique. Seules les pochettes intégrées ou URI locales lisibles sont chargées ; aucune image HTTP/HTTPS n’est téléchargée. Un lecteur peut omettre ses données ou refuser son image. Les autres lecteurs ne sont pas tous certifiés.

Mise à jour par-dessus PRISME, données conservées, schéma 7 inchangé. Le paquet PC est aligné sur la livraison, sans nouvelle fonction. Pas d’essai prolongé d’autonomie ou de validation exhaustive en veille profonde dans cette passe.

[Notes 6.0.1](RELEASE-6.0.1.md) · [Activation](V6-GUIDE.md#musique-et-autorisations) · [Confidentialité](PRIVACY.md)
