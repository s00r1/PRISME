# PRISME 6.0.1 — titre et pochette du morceau

Correctif stable pour la musique de PRISME 6. **VersionCode 25 · Android complet 7.1 / API 25 minimum.** Il corrige la récupération et l’affichage du titre, de l’artiste et de la pochette, tout en conservant les 22 styles, les 13 types de widgets, la météo et les outils de la V6.

## Ce qui change

- Le titre, l’artiste et l’album utilisent les champs alternatifs de la session lorsque les champs principaux du lecteur sont vides.
- La vraie pochette est lue depuis les métadonnées musicales, réduite à 256 pixels maximum sur son grand côté et conservée uniquement en mémoire. Les changements de morceau remplacent l’image ; une piste sans pochette ne garde pas celle de la précédente.
- Le panneau musical s’actualise pendant son affichage et au retour des réglages Android. Les autres surfaces musicales utilisent le même instantané de session : widget, mini-lecteur, carte et centres de contrôle.
- Les états « fonction désactivée », « autorisation Android manquante » et « aucune session disponible » sont distincts. Les commandes simples restent disponibles sans lecture des métadonnées.
- L’accès aux titres et pochettes est proposé directement dans les réglages musicaux. Une session détruite, un changement de lecteur ou la perte d’accès invalident les anciennes données.

## Activer les informations musicales

Deux choix sont nécessaires : **Outils → Musique → Activer titres et pochettes**, puis autoriser **PRISME · Sessions multimédias** dans le réglage Android qui s’ouvre. Revenir ensuite dans PRISME et lancer un morceau dans le lecteur souhaité. L’activation PRISME et l’autorisation Android restent locales : un profil importé ne peut accorder ni l’une ni l’autre.

Android utilise pour cela son accès sensible aux notifications. PRISME lit les sessions multimédias et leurs métadonnées, sans lire ni conserver les corps des messages. Aucun téléchargement de pochette depuis un lien HTTP/HTTPS, aucun historique d’écoute ou envoi de ces données n’est ajouté. Les images intégrées à la session et les URI locales lisibles sont prises en charge ; un lecteur peut ne pas fournir de couverture exploitable.

## Validation

Sur la DM100 sous Android 7.1.1 : **50 assertions musicales réussies**, avec session contrôlée, champs alternatifs, pochettes, changements de titre, autorisations et restauration des préférences. Une lecture **Spotify réelle** a également fourni titre, artiste et pochette `ALBUM_ART` de **200 × 200 pixels**. Le panneau musical, le widget et le centre iOS ont été inspectés sur la montre.

La suite de **67 663 assertions Java** passe. L’audit du DEX contrôle **1 175 références publiques Android**, sans référence incompatible non protégée ni référence non résolue. Les autres lecteurs et toutes les variantes d’affichage ne sont pas certifiés par ces essais. Les détails et limites sont dans le [rapport musique 6.0.1](MUSIC-6.0.1-VALIDATION.md).

## Fichiers de la Release

- **PRISME-6.0.1.apk** : correctif signé avec le certificat d’origine, à installer par-dessus PRISME sans désinstallation.
- **PRISME-PC-Studio-6.0.1.zip** : paquet PC aligné sur cette livraison, avec les éditeurs de widgets/cartes et le Studio classique. Aucun nouveau fonctionnement de l’éditeur n’est revendiqué par ce correctif musical.
- **PRISME-6.0.1-sources.zip** : sources de l’application, des éditeurs, tests et documents correspondant à cette livraison.
- **SHA256SUMS.txt** : empreintes des trois fichiers distribués.

Exporter sa configuration avant une mise à jour. Les notes, tâches, profils, styles et dispositions restent conservés ; le format de profil reste le schéma 7. Les pochettes et métadonnées de lecture ne sont pas exportées. Aucun titre de morceau personnel, nom d’artiste écouté, compte ou capture musicale personnelle n’est publié dans cette documentation.

[Activation et dépannage](V6-GUIDE.md#musique-et-autorisations) · [Confidentialité](PRIVACY.md) · [Fonctions de la V6](RELEASE-6.0.0.md)
