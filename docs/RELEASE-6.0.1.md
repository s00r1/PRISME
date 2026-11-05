# PRISME 6.0.1 — titre et pochette du morceau

Correctif stable pour la musique de PRISME 6. **VersionCode 25 · Android complet 7.1 / API 25 minimum.** Les 22 styles, les widgets, la météo et les outils V6 restent disponibles.

## Corrections

- Titre, artiste et album : reconnaissance des champs alternatifs quand les champs principaux du lecteur sont vides.
- Pochette réelle provenant de la session musicale, réduite à 256 pixels maximum et conservée uniquement en mémoire.
- Changement de morceau : remplacement de l’image ; une piste sans couverture ne conserve pas celle de la précédente.
- Panneau musical actualisé pendant la lecture et au retour des réglages Android. Widget, mini-lecteur, carte et centres de contrôle utilisent le même instantané.
- États distincts pour la fonction désactivée, l’autorisation Android manquante et l’absence de session.

## Activation

Deux choix sont nécessaires : **Outils → Musique → Activer titres et pochettes**, puis autoriser **PRISME · Sessions multimédias** dans Android. Revenir dans PRISME et lancer un morceau.

Ces choix restent locaux et ne peuvent pas être accordés par un profil importé. Android utilise son accès sensible aux notifications ; PRISME lit les sessions multimédias sans lire ni conserver les corps des messages. Le mode simple conserve ses commandes.

Les images intégrées et URI locales lisibles sont prises en charge. Aucun téléchargement de pochette HTTP/HTTPS n’est ajouté ; un lecteur peut ne pas fournir d’image exploitable.

## Validation et fichiers

**50 assertions musicales réussies sur la DM100**, puis titre, artiste et pochette Spotify réelle `ALBUM_ART` de **200 × 200 pixels** vérifiés. Panneau, widget et centre iOS inspectés. Suite Java : **67 663 assertions réussies**. Audit : **1 175 références Android**, aucune incompatible non protégée ou non résolue. [Rapport et limites](MUSIC-6.0.1-VALIDATION.md).

- **PRISME-6.0.1.apk** : correctif signé, à installer par-dessus PRISME.
- **PRISME-PC-Studio-6.0.1.zip** : paquet PC aligné, sans nouvelle fonction d’édition.
- **PRISME-6.0.1-sources.zip** : sources, tests et documentation.
- **SHA256SUMS.txt** : empreintes.

Exporter sa configuration avant mise à jour. Notes, tâches et dispositions sont conservées ; le schéma reste 7. Aucun morceau personnel, compte ou capture musicale personnelle n’est publié. Tous les lecteurs tiers ne sont pas certifiés.

[Activation et dépannage](V6-GUIDE.md#musique-et-autorisations) · [Confidentialité](PRIVACY.md)
