# Intégration du moteur Xray

TOM_TUNNEL utilise le wrapper officiel [XTLS/libXray](https://github.com/XTLS/libXray), qui intègre Xray-core pour Android via gomobile. La compilation du moteur natif est un prérequis : il n'est pas possible de prétendre qu'un binaire Go est intégré simplement en ajoutant une dépendance Kotlin.

## Construire l'AAR natif

Prérequis : Git, Go, Python 3, Android SDK/NDK configurés.

```bash
./scripts/build-xray-android.sh
```

Le script appelle `python3 build/main.py android` du dépôt officiel puis copie l'AAR obtenu vers `app/libs/libXray.aar`. Le dépôt d'application ne contient pas de binaire précompilé non vérifié. GitHub Actions exécute le même processus de construction.

## API utilisée

`LibXrayBridge` envoie les enveloppes JSON `apiVersion: 3` aux méthodes `testXray`, `runXray`, `stopXray`, `getXrayState` et `xrayVersion` documentées par libXray. La configuration du moteur reçoit `xray.tun.fd` dans la propriété `env` racine, comme documenté par Xray-core.

## Limites restantes avant une version de production

- Le mécanisme Android `VpnService.protect()` doit être relié au contrôleur socket-protect de libXray. Sans cela, les connexions sortantes du moteur peuvent boucler dans le VPN.
- Le cycle de vie complet doit garder le descripteur TUN ouvert tant que le moteur tourne et arrêter Xray avant de fermer l'interface.
- Le format d'inbound TUN et la configuration générée doivent être validés avec la version précise de Xray embarquée.
- Le routage complet IPv4/IPv6, les DNS, les changements réseau, le kill switch et le test de trafic doivent être vérifiés sur appareil réel.
- Les champs REALITY/flow, transport HTTP/H2 et paramètres VMess/Trojan doivent être validés par les constructeurs du moteur. Ce générateur initial ne remplace pas la validation native.
- L'API de libXray ne garantit pas sa stabilité; la version et le commit doivent être épinglés avant une publication reproductible.

Ne jamais afficher CONNECTÉ avant que `runXray` ait réussi et qu'une requête de test ait été vérifiée via le tunnel.
