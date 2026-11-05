# PRISME PC Studio 6.0.1

L’éditeur PC prépare les **widgets des 22 bureaux** et les **cartes facultatives**, dans un navigateur moderne sous Windows, macOS ou Linux. Il fonctionne avec souris, tactile et champs numériques, sans compte, installation ou connexion directe à la montre. Le bureau classique sélectionné reste principal.

## Ouvrir l’éditeur

- **En ligne :** [PC Studio](https://s00r1.github.io/PRISME/home-studio.html).
- **Hors ligne :** extraire entièrement `PRISME-PC-Studio-6.0.1.zip`, puis ouvrir `index.html`. Sous Windows, `LANCER-EDITEUR.cmd` ouvre ce fichier dans le navigateur par défaut.

Le ZIP comprend les scripts, styles, 22 aperçus de référence, les deux éditeurs, ce guide, la licence et `FILES-SHA256.json`. Il fonctionne directement en `file://`, sans serveur, extension ou outil de développement. Seuls les liens facultatifs vers GitHub ou le site nécessitent une connexion lorsqu’on les ouvre.

## Commencer par un export complet

Sur la montre : **Réglages → Profils → Exporter ma configuration**. Conserver cet original et copier le JSON sur le PC. Les notes, tâches et favoris peuvent y figurer : ne pas publier le fichier dans une issue.

Charger ce fichier dans **Votre export JSON**. Les profils V5/V6, schémas 6 et 7, sont acceptés, y compris les exports des V5.1 à V5.4 sans les champs ajoutés ensuite en V6. Sans export complet, l’éditeur affiche une démonstration et désactive l’export. Il exige notamment les sections de notes, tâches, favoris, espaces, noms, historique et raccourcis, même lorsqu’elles sont vides. Un fichier partiel n’est pas transformé en sauvegarde de remplacement. Un coffre `.vault` doit être ouvert depuis PRISME ; l’éditeur ne demande pas sa phrase de passe. Taille maximale d’un profil JSON : 64 Kio.

## Onglet Widgets des 22 styles

Le catalogue propose **13 types**, avec jusqu’à **16 instances** dans la configuration :

| Widget | Aperçu et accès sur la montre |
| --- | --- |
| Météo | Conditions, tendance, cinq jours ou prochaines heures ; toucher ouvre les prévisions. |
| Horloge | Digitale, analogique, grands chiffres ou date/heure ; secondes et format 12/24 h. |
| Horloge mondiale | Heure d’un fuseau choisi et mêmes présentations que l’horloge. |
| Musique | Session Android et commandes disponibles selon le lecteur. |
| Batterie | Niveau réel communiqué par Android et état de charge. |
| Tâches | Résumé de la checklist et accès à l’organiseur. |
| Carnet | Accès au carnet, sans exposer le texte privé dans le widget. |
| Minuteurs | Résumé des minuteurs nommés et accès à leur gestion. |
| Calendrier | Date et accès aux outils de calendrier. |
| Système | Résumé local des ressources et accès au diagnostic. |
| Concentration | Accès au minuteur de concentration. |
| Raccourcis | Accès aux fonctions PRISME. |
| Chronomètre | Accès au chronomètre et à ses tours. |

La configuration initiale active Météo, Musique et Tâches ; la météo reste sans réseau jusqu’à votre consentement sur la montre. Les autres entrées sont disponibles à activer, et le chronomètre peut être ajouté depuis le catalogue. Retirer un widget conserve les données de son outil.

1. Choisir le **style de référence** et l’orientation pour la prévisualisation. Ce choix sert à l’aperçu ; il ne change pas le style actif du profil.
2. Choisir la zone commune : gauche, droite ou masquée. En paysage, sa largeur est de 144 à 208 px ; en portrait, elle devient une bande supérieure. Le bureau conserve sa surface et ses commandes.
3. Ajouter un widget ou en sélectionner un dans la liste ou l’aperçu.
4. Choisir son apparence : **Signature du bureau, Verre, Carte pleine, Contour, Minimal ou Néon** ; régler densité, présentation, accent, opacité du fond, arrondis et texte.
5. Régler titre, détails et options propres aux horloges. **Dans quels styles afficher ce widget ?** permet une sélection parmi les 22 styles ; une liste vide désigne tous les styles. Pour n’en afficher aucun, désactiver le widget.
6. Utiliser **Monter**, **Descendre**, **Retirer** ou **Annuler / rétablir**. L’annulation conserve une étape, partagée avec l’onglet des cartes.

L’aperçu utilise des données fictives, des captures de référence des bureaux et un rendu web simplifié. Il ne constitue ni une météo en direct ni une simulation d’Android. Le rendu définitif est celui de la montre.

### Météo : ce qui se prépare sur PC

Le PC règle les **°C/°F**, la fréquence de 15 à 180 minutes en premier plan, l’apparence et les styles dans lesquels montrer le widget. Il ne se connecte pas au fournisseur météo.

La **ville, les coordonnées, le fuseau de la ville, le cache et le consentement réseau restent exclusivement sur la montre**. Les paramètres météo portables ne contiennent que `fahrenheit` et `interval`. Sur la montre : **Réglages → Widgets → Ville et météo connectée**, activer Open-Meteo, rechercher une ville puis sélectionner le bon résultat. Une importation ne peut ni activer la connexion ni changer la ville.

## Onglet Cartes facultatives

Ce panneau reste distinct des 22 bureaux et de leur zone de widgets. Il comprend sept types de carte : Horloge, Tâches, Minuteur, Musique, Carnet, Favoris et Montre.

Choisir ou ajouter une page, modifier son nom, sélectionner un bloc puis régler sa colonne, sa ligne, sa largeur et sa hauteur. Glisser un bloc ou utiliser les champs numériques et **Appliquer au bloc**. Choisir une des six palettes et les grands textes si nécessaire.

La grille comporte quatre colonnes et quatre lignes. Les coordonnées vont de 0 à 3 ; les dimensions de 1 à 4. Maximum : cinq pages, huit cartes par page. Les chevauchements et sorties d’écran sont refusés ; une page au moins doit rester présente. Les exemples de musique, tâches et mesures sont fictifs.

## Exporter et appliquer

Cliquer **Exporter le profil V6** pour télécharger `PRISME-6-Studio.json`. Copier ce fichier sur la montre par USB, puis ouvrir **Réglages → Profils → Importer une configuration → Parcourir avec le sélecteur Android**. Sélectionner le fichier, lire le récapitulatif puis confirmer.

L’éditeur conserve les autres sections du fichier chargé, notamment notes, tâches, batterie et centre de contrôle. Il écrit les dispositions de cartes, la configuration de widgets et les préférences d’affichage météo. Le choix actuel bureau/cartes reste local lors de l’import sur la montre. Les autorisations Android, le consentement météo, la ville, l’accès musical enrichi et l’activation du dock ne sont pas accordés par un profil.

La montre effectue sa validation complète et crée un point de récupération avant application. Pour ouvrir les cartes, utiliser **Outils → Bureau et cartes** ; **Bureau** ou Retour Android ramène au style sélectionné. **Réglages → 22 styles** reste accessible.

## Éditeur classique conservé

Dans le ZIP, `classic-studio.html` ouvre le Studio historique : style de bureau et quatre widgets classiques à positionner. En ligne : [Studio classique](https://s00r1.github.io/PRISME/studio.html). Ce système historique reste disponible à côté des nouveaux widgets ; il utilise ses propres réglages. Pour composer les 13 nouveaux types et les cartes, revenir à `index.html`.

## Confidentialité et dépannage

Le profil est traité dans la mémoire du navigateur, sans envoi, analytique ou dépendance distante. Fermer l’onglet sans exporter perd les modifications non téléchargées. La version en ligne charge naturellement ses fichiers publics depuis le site ; le contenu du profil n’est pas transmis.

| Situation | Action |
| --- | --- |
| Exporter désactivé | Charger un export JSON complet provenant de PRISME. |
| Fichier refusé | Réexporter la configuration complète sur la montre ; ne pas retirer les sections vides. Vérifier schéma, structure et taille ; ne pas charger un `.vault`. |
| Widget absent | Vérifier activation, liste de styles et position de la zone ; la zone peut défiler. |
| Météo sans prévisions | Choisir une ville et autoriser la météo sur la montre, puis actualiser avec une connexion disponible. |
| Carte impossible à déplacer | Libérer les cases de destination ou réduire sa taille. |
| Changements absents sur la montre | Copier puis importer le dernier fichier exporté ; vérifier l’onglet ou le style concerné. |
| Page hors ligne incomplète | Extraire toute l’archive, conserver son arborescence puis ouvrir `index.html` hors du ZIP. |
| Téléchargement absent | Consulter les téléchargements du navigateur ; autoriser l’action pour ce fichier local si nécessaire. |

Les éditeurs sont distribués sous licence MIT. `FILES-SHA256.json` donne l’empreinte des fichiers contenus ; les sommes de contrôle du ZIP et de l’APK sont fournies séparément pour la Release V6.
