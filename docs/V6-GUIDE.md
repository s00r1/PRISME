# PRISME 6.0.0-beta1 — Guide

Cette bêta cible la DM100 rectangulaire sous Android complet 7.1.1 / API 25, en 640 × 480. Aucun root ni remplacement de firmware n’est nécessaire.

## Installer et migrer

1. Dans votre V5, exportez la configuration et conservez l’APK précédent.
2. Téléchargez l’APK signé de la Release `v6.0.0-beta1` et installez-le par-dessus PRISME. Ne désinstallez pas l’application pour mettre à jour.
3. La signature d’origine est conservée. Les applications, notes, tâches et réglages existants sont conservés ; l’accueil modulaire s’affiche par défaut.
4. Pour retrouver les 22 bureaux : **Modifier → Revenir au bureau classique**. Dans la recherche, cherchez « accueil modulaire » pour revenir à la V6.

Les profils JSON de versions antérieures restent acceptés. Les exports V6 utilisent le schéma 7 et peuvent ne pas être compris par les anciennes versions : conservez un export V5 pour le retour arrière. Android peut refuser un numéro de version inférieur : un retour V5 nécessite alors une procédure dédiée avec sauvegarde, sans désinstallation automatique. Ne désactivez pas Play Protect si une installation est bloquée ; la validation USB ne certifie pas ce service.

## Composer l’accueil

Les flèches du haut changent de page. Les sept types de bloc sont Horloge, Tâches, Minuteur, Musique, Carnet, Favoris et Montre. Un appui court ouvre la fonction ; un appui long ouvre les coordonnées et dimensions. Dans **Modifier → Déplacer les blocs au doigt**, glissez un bloc puis touchez **Terminer**. Les positions s’alignent sur une grille de quatre colonnes et quatre lignes.

Jusqu’à cinq pages et huit blocs par page. Les coordonnées vont de 0 à 3 et les dimensions de 1 à 4 ; un bloc ne peut ni sortir de la page ni chevaucher un autre bloc. Une modification invalide est refusée. Une étape d’annulation et de rétablissement est disponible. Les pages peuvent être vides, mais il reste toujours au moins une page.

Les palettes de l’accueil sont indépendantes du style du bureau classique. Les blocs Carnet et Favoris n’affichent pas leur contenu personnel sur l’accueil. Les notes/tâches restent partagées entre les pages.

## Minuteurs et outils

**Outils → Minuteurs nommés** propose huit minuteurs, avec durée initiale de 1 à 1 440 minutes. Pause, reprise, redémarrage et suppression sont indépendants. Les alarmes sont reprogrammées après redémarrage ; le firmware peut retarder une livraison. L’accueil et le panneau vérifient aussi les expirations lorsqu’ils sont ouverts.

Les minuteurs actifs ne sont pas importés depuis un thème ou une sauvegarde. La calculatrice accepte des nombres décimaux bornés et quatre opérations. Le convertisseur fonctionne sans réseau : longueur, masse, température et durée.

## Musique et permissions

Le mode simple utilise les commandes multimédias Android. Le mode enrichi, **Outils → Musique → Accès multimédia enrichi**, nécessite une autorisation Android explicite d’accès aux notifications. PRISME utilise les sessions multimédias, sans lire ni conserver les messages des notifications. La liste des lecteurs dépend des sessions publiées par les applications.

Le titre, l’artiste et les commandes disponibles dépendent du lecteur. Le mode simple reste utilisable sans cette autorisation. Désactiver le mode enrichi dans PRISME arrête son utilisation ; la permission système peut être révoquée dans Android. Le fonctionnement avec Spotify, Deezer et tous les navigateurs n’est pas certifié par le test d’une session locale.

## Sauvegardes et récupération

La configuration active exportée comprend les réglages du bureau, notes, tâches, centre de contrôle, volume, batterie, musique et modules V6, dont les pages. Les autorisations système, l’activation du dock, l’accès enrichi et les exécutions de minuteurs restent locaux. La banque de profils nommés n’est pas exportée récursivement.

Les sauvegardes automatiques se trouvent dans l’espace privé de l’application. Un point est créé à l’entrée en V6, puis au lancement après 24 heures ; dix points automatiques sont conservés. Les anciens exports V5 ne sont pas supprimés et restent accessibles via le sélecteur Android.

**Outils → Sauvegardes chiffrées** crée un coffre AES-256-GCM avec mot de passe d’au moins dix caractères. Il peut être exporté via le sélecteur Android puis importé depuis **Réglages → Profils → Importer une configuration**. Le mot de passe est indispensable : il n’y a ni serveur ni récupération à distance. Les exports JSON ordinaires restent en clair.

Les imports sont validés avant écriture ; un journal privé permet de revenir à l’état précédent après une interruption de restauration. Après trois exceptions non traitées consécutives, le mode de secours offre une reprise ou un retour au bureau classique avec les fonctions flottantes désactivées. Il ne répare ni une mémoire de stockage défectueuse ni le firmware.

## Superpositions et énergie

Sur l’accueil modulaire, les poignées et indicateurs flottants PRISME sont masqués pour libérer les blocs. Ils reviennent dans les autres applications selon les réglages autorisés. **Outils → Suspendre tous les éléments flottants** les suspend jusqu’à réactivation ; ce choix reste local.

Les nouveaux écrans limitent leur travail à leur période d’affichage. Les rafraîchissements de l’accueil s’arrêtent à l’extinction de l’écran. Sense et le dock hérités de la branche V6 restent facultatifs. Aucun gain chiffré d’autonomie n’est annoncé avant un essai prolongé.

## Éditeur PC

Ouvrez [l’éditeur d’accueil](https://s00r1.github.io/PRISME/home-studio.html). Chargez votre export JSON complet V5/V6, composez les pages puis exportez le profil. Sans fichier réel, seul l’aperçu est proposé. Le contenu du fichier n’est pas envoyé sur le réseau. Les sept types de bloc, limites et collisions sont vérifiés ; la montre effectue sa propre validation complète avant import.

Les interfaces V5 restent disponibles en sept langues. Les principaux libellés V6 sont ajoutés aux six catalogues traduits ; certains messages détaillés nouveaux restent en français dans cette première bêta.
