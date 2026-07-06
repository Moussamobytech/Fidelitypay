# Génération et Utilisation des SDKs FidelityPay

Ce dossier contient les scripts permettant de générer automatiquement les SDKs (Software Development Kits) pour le backend FidelityPay. Ces SDKs facilitent l'intégration de la passerelle de paiement dans d'autres applications.

## 1. Générer les SDKs

Avant de lancer la génération, **assurez-vous que votre backend Spring Boot est en cours d'exécution** sur le port `8060`.

Pour générer (ou mettre à jour) les SDKs, exécutez le script depuis ce dossier :

```bash
./generate-sdks.sh
```

Une fois la génération terminée, tous les SDKs compilés se trouveront dans le dossier `../sdk/`.

---

## 2. Utiliser le SDK dans vos autres projets (en Local)

Puisque les SDKs générés ne sont pas encore publiés sur des registres publics (NPM, Maven Central, NuGet, etc.), voici comment les intégrer localement pour chaque langage cible.

### 🟢 TypeScript & JavaScript (Node.js, Angular, React...)

Le générateur crée un projet NPM complet (identique pour TypeScript et Javascript, avec les typages inclus).

**Étape A : Créer un lien symbolique global (NPM Link)**
Dans votre terminal, allez dans le dossier du SDK généré et créez un lien :
```bash
# Pour TypeScript
cd ../sdk/typescript
npm link

# Ou pour Javascript pur
cd ../sdk/javascript
npm link
```

**Étape B : Lier le SDK à votre projet externe**
Allez dans le projet où vous souhaitez utiliser FidelityPay :
```bash
cd /chemin/vers/votre/projet
npm link fidelitypay-sdk
```

**Étape C : Utilisation dans le code**
```typescript
import { Configuration, MerchantPayInApi, MerchantPaymentRequest } from 'fidelitypay-sdk';

const config = new Configuration({
    basePath: 'http://localhost:8060', 
});
const api = new MerchantPayInApi(config);

// api.initiate('VOTRE_CLE_API', 'IDEMPOTENCY_KEY', request_data);
```

### ☕ Java (Spring Boot, Android...)

Le SDK Java généré est un projet Maven.

**Étape A : Installer le SDK dans votre cache Maven local (`~/.m2`)**
Le script de génération a déjà compilé le code, mais pour l'installer formellement :
```bash
cd ../sdk/java
mvn clean install -DskipTests
```

**Étape B : L'ajouter à votre projet externe**
Ajoutez cette dépendance dans le fichier `pom.xml` du projet cible :
```xml
<dependency>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-java-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 📱 Flutter & Dart

Le SDK Flutter/Dart peut être lié directement via son chemin sur le disque dur.

**Étape A : Référencer le dossier local**
Dans le fichier `pubspec.yaml` de votre application mobile Dart ou Flutter, ajoutez la dépendance en pointant vers le dossier généré :
```yaml
dependencies:
  openapi:
    path: ../chemin/vers/Fidelitypay/sdk/flutter
    # Ou path: ../chemin/vers/Fidelitypay/sdk/dart si vous générez du dart pur
```

**Étape B : Importer et utiliser**
```dart
import 'package:openapi/api.dart';

var defaultApiClient = defaultApiClient;
defaultApiClient.basePath = 'http://localhost:8060';

var apiInstance = MerchantPayInApi();
// apiInstance.initiate(...);
```

### 🟣 C# (.NET Core, ASP.NET, Xamarin...)

Le SDK C# généré contient les fichiers sources (`.cs`) et un fichier projet `.csproj`.

**Étape A : Référencer le projet (Project Reference)**
Si votre application est dans la même solution ou sur la même machine, vous pouvez utiliser le CLI .NET pour ajouter une référence au SDK directement :
```bash
cd /chemin/vers/votre/projet_csharp
dotnet add reference /chemin/vers/Fidelitypay/sdk/csharp/src/Org.OpenAPITools/Org.OpenAPITools.csproj
```

**Alternative : Générer un package NuGet local**
Vous pouvez aussi compiler le SDK en fichier `.nupkg` :
```bash
cd ../sdk/csharp
dotnet pack -c Release
```
Cela générera un fichier `Org.OpenAPITools.1.0.0.nupkg` que vous pourrez importer via le gestionnaire de paquets NuGet local de Visual Studio.

**Étape B : Importer et utiliser**
```csharp
using Org.OpenAPITools.Api;
using Org.OpenAPITools.Client;
using Org.OpenAPITools.Model;

Configuration config = new Configuration();
config.BasePath = "http://localhost:8060";

var apiInstance = new MerchantPayInApi(config);
// apiInstance.Initiate(...);
```

---

## 3. Déployer en Production (Distribution)

Si vous souhaitez distribuer ces SDKs à des marchands tiers (ex: vos clients) pour qu'ils puissent les installer facilement sans télécharger le code source :
- **JS / TS** : Lancez `npm publish` dans le dossier pour l'envoyer sur *NPM*.
- **Java** : Configurez le `pom.xml` pour publier sur *Maven Central* ou *GitHub Packages*.
- **Flutter / Dart** : Lancez `dart pub publish` pour l'envoyer sur *pub.dev*.
- **C#** : Utilisez `dotnet nuget push` pour l'envoyer sur *NuGet.org*.
