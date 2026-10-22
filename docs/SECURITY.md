# Sécurité

- L'identité `TT-...` est un identifiant local aléatoire, pas une authentification.
- Le contenu importé est traité comme des données et non comme du code exécutable.
- Ne pas journaliser UUID, mots de passe, clés privées ou tokens.
- Une expiration locale est modifiable et ne remplace pas une validation serveur.
- Le service actuel ne route pas le trafic : ne pas activer de promesse de protection anti-fuite.
- Avant toute version publique, auditer le moteur natif, les licences, les dépendances, les logs et le routage.
