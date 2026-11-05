# PRISME 6 — Architecture et suite du projet

Version : **6.0.1 / versionCode 25**. Les 22 bureaux sont l’entrée principale. Le panneau de cartes est une option explicite, avec retour permanent au bureau sélectionné. Un import de disposition ne remplace pas ce bureau et ne peut activer les règles automatiques Sense. Le statut de publication et les vérifications de l’APK sont consignés séparément dans les notes de livraison et de validation.

## Modules livrés

- 22 styles avec sélection compacte, recherche, aperçu et accès direct depuis les réglages.
- Cartes facultatives : pages, taille/position, six palettes, annulation et grands textes.
- Éditeur PC en ligne et paquet hors ligne autonome pour les cartes et les bureaux classiques.
- Huit minuteurs nommés, musique simple/enrichie facultative, calculatrice et conversions.
- Points de récupération privés, validation des imports, journal et coffres chiffrés.
- Mode de secours, gestion des superpositions et diagnostic local.
- Widgets classiques, Sense, dock et Link PC issus de la base existante.
- Moteur de widgets commun aux 22 styles : météo, horloge, musique, batterie, tâches, carnet, minuteurs, calendrier, horloge mondiale, système, concentration, raccourcis et chronomètre. Treize types, jusqu’à seize instances identifiées indépendamment ; ajout, retrait, activation, ordre et filtre par style.
- Réglages par widget : six apparences, densité, variantes, accent, opacité, arrondi, texte, titre et détails ; options d’horloge et fuseau mondial. Une zone réservée latérale en paysage ou supérieure en portrait conserve une surface distincte au bureau. Dernière modification annulable/rétablissable.
- Météo Open-Meteo facultative : ville choisie manuellement, température et ressenti, humidité, vent, prochaines heures, cinq jours, unités, fréquence au premier plan et cache horodaté.
- Centres de contrôle distincts : modules à cercles et niveaux verticaux pour iOS ; grandes tuiles Material à deux colonnes et niveaux horizontaux pour Android. Le lecteur et les curseurs sont des modules indépendants. Les commandes choisies, leur ordre et les gestes sont conservés.

## Correctif musical 6.0.1

Le chemin `MediaAccess` vérifie séparément l’activation locale et l’autorisation Android. `MusicPlayback` suit la session choisie, ses changements de métadonnées et son arrêt ; une resynchronisation périodique couvre les lecteurs ou firmwares qui omettent des callbacks. Le panneau ne reste plus figé sur le morceau de son ouverture.

Les titres, artistes et albums reconnaissent les champs principaux et les champs d’affichage alternatifs. `MediaArtwork` traite les bitmaps `DISPLAY_ICON`, `ART` et `ALBUM_ART`, ainsi que les URI locales autorisées, en dehors du dessin de l’interface. Le cache en mémoire est borné et lié à la session et au morceau. Un changement de piste ou la perte d’accès retire l’ancienne image. Aucun corps de notification, cache disque ou téléchargement HTTP/HTTPS de pochette n’est utilisé. Une couverture absente reste un état explicite.

Les différentes surfaces musicales présentent le même instantané : panneau, zone de widgets, mini-lecteur, carte et centres de contrôle. Le test Android `MusicValidation` utilise sa propre session contrôlée en pause, sans audio focus ni commande au lecteur utilisateur, pour vérifier extraction, changements de piste, pochettes et accès. Le parcours du gestionnaire réel est signalé séparément lorsque l’autorisation Android est absente.

## Frontières des données et du réseau

Le manifeste déclare désormais `android.permission.INTERNET` pour la météo. Le réseau météo est désactivé par défaut et exige un accord explicite dans ses réglages. La recherche transmet le texte de ville à `geocoding-api.open-meteo.com` ; les prévisions transmettent les coordonnées de la ville choisie à `api.open-meteo.com`, en HTTPS. Le fournisseur reçoit aussi l’adresse IP. Aucune permission GPS ou de localisation n’est déclarée. Notes, tâches et autres données de profil ne sont pas envoyées à ce service. Les données et conditions du fournisseur restent accessibles sur [Open-Meteo](https://open-meteo.com/).

Le consentement réseau, la ville, les coordonnées et le cache météo ne sont pas des paramètres de thème. Le profil exporte la disposition des widgets, l’unité météo et l’intervalle, sans importer de consentement ou de localisation. Les exports antérieurs sans ces champs restent acceptés. Le cache conserve sa date et son état périmé ; une erreur de connexion ne devient pas une mesure simulée. La désactivation empêche les nouvelles requêtes, conserve le cache et invalide les résultats de prévision encore en vol.

## Préserver le bureau lors du retrait des widgets

`WidgetStore` écrit seulement dans `prisme_widget_engine`. L’éditeur retire une instance par son identifiant stable et actualise l’affichage ; il n’écrit pas le style `theme` et n’active pas les cartes. `MainActivity.layoutDashboardWidgets()` réserve la zone uniquement si au moins un widget est visible pour le style choisi. Une liste vide, une zone masquée ou un filtre sans widget rend au bureau toute sa surface disponible. La navigation des 22 styles reste donc indépendante du catalogue et de la disposition des widgets.

## Travaux futurs, non annoncés comme disponibles

Fonds personnels avec recadrage, dossiers, widgets Android tiers, progression musicale, agenda enrichi avec synchronisation, rappels récurrents, tâches avec échéances, historique/estimation de batterie, compagnon téléphone et bibliothèque de thèmes téléchargés. Le calendrier livré affiche une date locale et ouvre un calendrier local ; il ne revendique pas une synchronisation de compte. La météo de la V6 est décrite ci-dessus et n’est plus une fonction future.

Les améliorations futures doivent préserver la navigation des 22 bureaux. La validation combine modèles, essais Android et inspection des captures sur la DM100 ; les chiffres de tests ne remplacent pas cette inspection ni un essai prolongé d’autonomie.

[Guide](V6-GUIDE.md) · [PC](PC-STUDIO.md) · [Validation](V6-VALIDATION.md) · [Build](BUILD.md) · [Confidentialité](PRIVACY.md)
