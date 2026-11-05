# Validation du correctif musique PRISME 6.0.1

**5 novembre 2026 · version 6.0.1 · versionCode 25.** Appareil physique : DM100, Android 7.1.1 / API 25. Ce rapport couvre le correctif titre, artiste et pochette. Il complète le [rapport V6 initial](V6-VALIDATION.md), dont les inspections des 22 styles restent historiques : elles n’ont pas toutes été répétées pour ce correctif.

## Problème corrigé

La V6 initiale ne représentait aucune pochette dans son instantané musical. Le panneau lisait le titre et l’artiste à son ouverture sans les actualiser ensuite ; seuls les champs principaux étaient reconnus. Une absence d’activation locale ou d’autorisation Android pouvait aussi être présentée comme une absence de musique.

Le correctif suit les métadonnées et l’état de la session choisie, reconnaît les champs alternatifs, traite la pochette en mémoire et distingue les états d’accès. Les commandes simples restent indépendantes de l’accès enrichi.

## Instrumentation Android réelle : 50 assertions réussies

`MusicValidation` a été exécuté sur la montre avec l’accès Android disponible. Il crée sa propre session contrôlée **en pause**, sans audio focus ni commande au lecteur de l’utilisateur. La préférence de lecteur est temporairement dirigée vers cette session, puis restaurée dans `finally`.

| Groupe | Assertions | Vérifications |
| --- | ---: | --- |
| URI de pochette | 11 | Ressources locales admises ; HTTP/HTTPS, fichier arbitraire, URI invalide, vide, absente ou excessive refusés. |
| Activation et accès | 12 | Opt-in local, état Android accordé, absence de session quand désactivé et retrait immédiat des données après désactivation locale. |
| Métadonnées et images contrôlées | 23 | Titres/artistes/albums, champs alternatifs, Unicode, trois clés bitmap, taille bornée, changements de titre et absence de données périmées. |
| Gestionnaire Android réel | 3 | Sélection du lecteur préféré, autorisation reflétée dans l’état et pochette obtenue par le chemin normal du gestionnaire. |
| Restauration | 1 | Préférences musicales d’origine rétablies et persistées. |
| **Total** | **50** | **Réussite sur Android 7.1.1.** |

Les images de test sont de simples aplats rouge, bleu et vert. Les clés `DISPLAY_ICON`, `ART` et `ALBUM_ART` sont testées indépendamment. Un changement de piste vérifie que la nouvelle image remplace la précédente ; une piste sans image retire la couverture, l’artiste et l’album précédents. Les métadonnées vides ne conservent pas l’ancien titre. Les images de 640 × 480 pixels du test sont réduites à au plus 256 pixels sur leur grand côté.

Le même harnais peut fonctionner sans accès aux sessions des autres applications en lisant son propre jeton de session. Dans ce cas, le rapport indique `manager=SKIPPED` et le parcours du gestionnaire n’est pas validé. **La passe de 50 assertions décrite ici disposait bien de l’autorisation Android.** Le harnais ne donne ni ne révoque lui-même cette autorisation et ne lit aucun corps de notification.

Sources et commande reproductible : [tests/device](../tests/device/README.md). Lire les champs `result`, `manager`, `assertions` et `preferences`, pas seulement le code de fin de l’instrumentation.

## Spotify réel et inspection sur la montre

Une session Spotify réellement active sur cette DM100 a été vérifiée, séparément de la session synthétique : le titre et l’artiste sont disponibles, ainsi qu’un bitmap de pochette **200 × 200 pixels** dans la clé **`ALBUM_ART`**. Le résultat a été contrôlé par le chemin de données de PRISME et par son affichage.

Surfaces inspectées physiquement pendant cette passe :

- Panneau musical : informations réelles et pochette.
- Widget Musique : informations réelles et pochette dans la zone des widgets.
- Centre de contrôle iOS : présentation musicale alimentée par la même session.

Le centre Android, le mini-lecteur et la carte musicale utilisent le même instantané et compilent avec le correctif ; ils n’ont pas tous fait l’objet d’une nouvelle inspection physique pendant cette passe. Le test d’une source de données partagée ne remplace pas une vérification visuelle de chaque présentation.

Aucun nom de morceau écouté, nom d’artiste écouté, compte Spotify, historique d’écoute ou capture musicale personnelle n’est publié ici. Les données réelles n’ont pas été transformées en fixtures publiques.

## Compilation, modèles et audit API

- Compilation Android réussie ; APK de correctif portant la version **6.0.1 / code 25**.
- Suite complète de **67 663 assertions Java** réussie.
- Audit du DEX : **1 175 références publiques Android contrôlées**, **0 référence incompatible non protégée**, **0 référence non résolue**.

L’audit vérifie les références publiques et les frontières de versions prises en charge par l’outil. Il ne constitue pas une preuve générale de comportement de tous les firmwares ou de tous les lecteurs. Les nombres de cas de modèle ne sont pas des nombres de configurations physiquement essayées.

## Activation, données et migration

L’utilisateur active **Titres et pochettes** dans PRISME, puis accorde séparément l’accès Android à **PRISME · Sessions multimédias**. Ni l’activation locale ni l’autorisation système ne peuvent provenir d’un import de profil. Un retour des réglages Android actualise l’affichage ; une désactivation retire les métadonnées et la pochette mises en cache.

Le mode simple conserve les commandes multimédias sans accès aux titres et pochettes d’un autre lecteur. Les images sont réduites et conservées en mémoire, sans cache disque, export, historique ou envoi vers un serveur. Les URI `content://` et `android.resource://` restent soumises aux droits de leur fournisseur ; les liens HTTP/HTTPS et chemins de fichier arbitraires ne sont pas chargés. Le réseau météo ne sert pas à télécharger des pochettes musicales.

La mise à jour s’installe par-dessus PRISME ; les données et dispositions V6 sont conservées et le schéma de profil reste 7. Le paquet PC 6.0.1 aligne les noms et documents de la livraison : il n’ajoute pas une nouvelle fonction d’édition musicale.

## Limites

Ce résultat ne certifie pas tous les lecteurs, toutes leurs versions ou tous les firmwares. Une application peut omettre ses métadonnées, refuser l’accès à son image locale ou ne fournir qu’une URL distante ; PRISME n’invente alors ni titre de morceau ni pochette. Aucune récupération réseau de ces URL n’est ajoutée par ce correctif.

Pas d’essai prolongé d’autonomie, de validation exhaustive en veille profonde ou de comparaison chiffrée de fluidité dans cette passe. Les anciennes validations de styles et d’outils de la V6 sont consultables dans le rapport initial et ne sont pas annoncées comme de nouveaux essais du correctif.

[Notes 6.0.1](RELEASE-6.0.1.md) · [Activer la musique](V6-GUIDE.md#musique-et-autorisations) · [Confidentialité](PRIVACY.md)
