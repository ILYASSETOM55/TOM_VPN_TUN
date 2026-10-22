# TOM_TUNNEL 1.0.0

Projet Android natif Kotlin / Jetpack Compose, identité `com.tom.tunnel`.

## État honnête du livrable

Cette archive est une **base de projet codée de zéro**, pas encore une application VPN opérationnelle. L'interface, l'identité locale, le modèle de profil, un importeur initial d'URI/JSON, la validation de champs et le format d'export JSON sont présents. Le service Android refuse volontairement de déclarer une connexion réussie sans moteur réel.

**Le moteur Xray-core Android, le binaire natif et le pont de paquets TUN → Xray ne sont pas encore intégrés.** Par conséquent, le trafic du téléphone ne passe pas encore dans un tunnel Xray, les mesures ping/débit ne sont pas implémentées et les protections anti-fuite ne doivent pas être considérées comme actives. Ne distribuez pas cette version comme VPN fonctionnel.

## Environnement de compilation

- Android Studio récent
- JDK 17
- Android SDK 35
- Gradle 8.9 ou compatible avec Android Gradle Plugin 8.7.3

Ouvrir le dossier `TOM_TUNNEL` dans Android Studio puis lancer la synchronisation Gradle et `assembleDebug`.

## Protocole et sécurité

Le parseur initial couvre les formes URI VLESS/VMess/Trojan/SOCKS et un format JSON TOM_TUNNEL versionné. Les URI VMess au format Base64 historique et les particularités d'encodage doivent être complétées et testées. La validation actuelle n'est pas équivalente à `xray run -test` : elle ne remplace pas la validation par le moteur.

Les exports JSON contiennent les champs d’authentification nécessaires à une sauvegarde complète. Ils ne sont pas chiffrés : gardez-les privés et ne les publiez jamais dans GitHub ou un canal public.

## Canal officiel

https://t.me/FREE_INTERNET237

## Architecture prévue pour l'intégration Xray

1. Choisir et épingler une version officielle Xray-core.
2. Construire ou intégrer un artefact Android/ARM64 maintenu et compatible, avec notices de licence.
3. Définir une interface native stable (JNI/ gomobile ou wrapper officiellement maintenu).
4. Implémenter un pont TUN complet, routage IPv4/IPv6, DNS, protection des sockets et exclusions anti-boucle.
5. Tester avec un serveur Xray autorisé sur appareil réel.
6. Ne signaler CONNECTED qu'après validation du tunnel et d'une requête de test via le tunnel.

Ne pas placer de serveurs, UUID, mots de passe ou clés privées réels dans Git.


## Moteur Xray ajouté

Le projet contient maintenant un adaptateur Kotlin `LibXrayBridge`, un générateur de configuration Xray/TUN initial et le script `scripts/build-xray-android.sh` pour construire le wrapper natif officiel XTLS/libXray. Exécutez le script avant la compilation Android. L'AAR n'est pas inclus dans l'archive car il doit être compilé depuis la source officielle et validé dans l'environnement cible. Cette intégration de code n'est pas une preuve que le tunnel est opérationnel : `VpnService.protect()`, le cycle de vie TUN et les tests réseau réels restent indispensables. Voir `docs/XRAY_INTEGRATION.md`.

## Fonctions corrigées dans cette révision

- Le canal Telegram `https://t.me/FREE_INTERNET237` est accessible depuis l'en-tête et les paramètres.
- Les profils importés sont enregistrés localement avec leurs paramètres complets et peuvent être sélectionnés depuis la liste.
- L'export ouvre maintenant le sélecteur de fichiers Android et écrit réellement le JSON. **Attention :** le fichier exporté peut contenir un UUID ou un mot de passe ; ne le publiez pas.
- Le parseur gère désormais les liens VMess historiques encodés en Base64 (format JSON standard), en plus des liens VLESS/Trojan/SOCKS et du JSON TOM_TUNNEL.
- Le workflow CI crée les répertoires `.build` et `app/libs` avant de construire/copier l'AAR, évitant un échec dû à des dossiers absents.

## Limites qui restent à résoudre avant publication

Cette archive n'est pas encore un VPN opérationnel. Le service Android est volontairement en mode sûr et n'affiche pas une fausse connexion : le démarrage Xray natif n'est pas encore relié à un pont TUN complet et testé. Le build CI doit aussi être exécuté dans GitHub Actions pour confirmer la compatibilité de la version amont de libXray. Aucun test sur appareil réel ni test de fuite DNS/IPv6 n'a été effectué ici. N'utilisez pas de vrais identifiants dans les tests automatisés ou dans Git.
