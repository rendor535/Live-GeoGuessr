# Dokumentacja operacyjna – Live GeoGuessr

## 1. Cel dokumentu

Dokument opisuje sposób uruchamiania, konfigurowania, testowania, wdrażania i utrzymywania aplikacji Live GeoGuessr.

Dokument jest przeznaczony dla członków zespołu projektowego oraz osób odpowiedzialnych za dalszy rozwój. 

---

## 2. Architektura systemu

* aplikacja mobilna Android napisana w Kotlinie z użyciem Jetpack Compose,
* Firebase Authentication,
* Cloud Firestore,
* Firebase Storage,
* Firebase Cloud Functions,
* GitHub Actions wykorzystywany do automatycznego budowania i testowania aplikacji.

Cloud Functions są uruchamiane w regionie:

```text
us-central1
```

Główny katalog aplikacji Android:

```text
LiveGeoGuessr/
```

Katalog backendu:

```text
LiveGeoGuessr/functions/
```

---

## 3. Wymagane oprogramowanie

Do uruchomienia projektu wymagane są:

* Android Studio,
* JDK 17,
* Android SDK,
* Node.js,
* npm,
* Firebase CLI,
* Git.

Sprawdzenie zainstalowanych wersji:

```bash
java -version
node --version
npm --version
firebase --version
git --version
```

Wersje Node.js i bibliotek backendowych powinny być zgodne z konfiguracją znajdującą się w plikach:

```text
functions/package.json
functions/package-lock.json
```

---

## 4. Konfiguracja projektu Firebase

Projekt wykorzystuje plik:

```text
LiveGeoGuessr/app/google-services.json
```

Plik zawiera konfigurację połączenia aplikacji Android z projektem Firebase.

Plik nie powinien zawierać prywatnych kluczy administracyjnych. W środowisku GitHub Actions jest tworzony na podstawie sekretu:

```text
GOOGLE_SERVICES_JSON
```

Projekt Firebase używany przez Firebase CLI można sprawdzić poleceniem:

```bash
firebase use
```

Lista dostępnych projektów:

```bash
firebase projects:list
```

Wybór projektu:

```bash
firebase use NAZWA_PROJEKTU
```

---

## 5. Uruchomienie aplikacji Android

Przejście do katalogu projektu:

```bash
cd LiveGeoGuessr
```

Zbudowanie wersji debug:

```bash
./gradlew assembleDebug
```

W systemie Windows:

```powershell
gradlew.bat assembleDebug
```

Wygenerowany plik APK znajduje się w katalogu:

```text
app/build/outputs/apk/debug/
```

Aplikację można również uruchomić bezpośrednio z Android Studio na emulatorze lub fizycznym urządzeniu.

---

## 6. Uruchamianie testów aplikacji Android

Uruchomienie wszystkich testów jednostkowych wersji debug:

```bash
./gradlew testDebugUnitTest
```

Zbudowanie aplikacji wraz z testami:

```bash
./gradlew build
```

Raport testów jednostkowych znajduje się w katalogu:

```text
app/build/reports/tests/testDebugUnitTest/
```

---

## 7. Instalacja zależności Cloud Functions

Przejście do katalogu backendu:

```bash
cd LiveGeoGuessr/functions
```

Instalacja zależności zgodnie z plikiem `package-lock.json`:

```bash
npm ci
```

---

## 8. Budowanie Cloud Functions

Budowanie kodu TypeScript:

```bash
npm run build
```

Dostępne skrypty można sprawdzić w pliku:

```text
functions/package.json
```

lub poleceniem:

```bash
npm run
```

---

## 9. Uruchamianie testów Cloud Functions

Testy backendu uruchamiane są z katalogu:

```text
LiveGeoGuessr/functions
```

Standardowe polecenie:

```bash
npm test
```

Jeżeli projekt zawiera osobne skrypty testowe, ich listę można sprawdzić poleceniem:

```bash
npm run
```

Przed uruchomieniem testów należy zainstalować zależności:

```bash
npm ci
```

---

## 10. Lokalne uruchomienie Firebase Emulator Suite

Przejście do katalogu zawierającego plik `firebase.json`:

```bash
cd LiveGeoGuessr
```

Uruchomienie emulatorów:

```bash
firebase emulators:start
```

Uruchomienie wybranych emulatorów:

```bash
firebase emulators:start --only functions,firestore,auth,storage
```

Emulatory pozwalają testować backend, reguły bezpieczeństwa i operacje na danych bez modyfikowania środowiska produkcyjnego.

---

## 11. Wdrażanie Cloud Functions

Przed wdrożeniem należy:

1. zainstalować zależności,
2. zbudować projekt,
3. uruchomić testy,
4. sprawdzić wybrany projekt Firebase.

Polecenia:

```bash
cd LiveGeoGuessr/functions
npm ci
npm run build
npm test
cd ..
firebase use
firebase deploy --only functions
```

Wdrożenie pojedynczej funkcji:

```bash
firebase deploy --only functions:NAZWA_FUNKCJI
```

---

## 12. Wdrażanie reguł Firestore

Reguły znajdują się w pliku:

```text
LiveGeoGuessr/firestore.rules
```

Wdrożenie reguł:

```bash
firebase deploy --only firestore:rules
```

Reguły należy wdrożyć po każdej zmianie dotyczącej:

* dostępu do profili użytkowników,
* postów,
* zgadnięć,
* znajomych,
* zaproszeń do znajomych,
* statystyk użytkownika.

Przed wdrożeniem reguły powinny zostać przetestowane przy użyciu Firebase Emulator Suite.

---

## 13. Wdrażanie indeksów Firestore

Konfiguracja indeksów znajduje się w pliku:

```text
LiveGeoGuessr/firestore.indexes.json
```

Wdrożenie indeksów:

```bash
firebase deploy --only firestore:indexes
```

Indeksy są wymagane między innymi dla zapytań łączących filtrowanie i sortowanie, na przykład pobierania postów użytkownika posortowanych według daty utworzenia.

---

## 14. Wdrażanie reguł Firebase Storage

Reguły znajdują się w pliku:

```text
LiveGeoGuessr/storage.rules
```

Wdrożenie reguł:

```bash
firebase deploy --only storage
```

Reguły kontrolują dostęp do:

```text
posts/{uid}/{postId}.jpg
avatars/{uid}/profile_<timestamp>.jpg
```

Reguły powinny ograniczać:

* właściciela przesyłanego pliku,
* dozwolony typ pliku,
* maksymalny rozmiar pliku,
* możliwość odczytu i usuwania danych.

---

## 15. Pełne wdrożenie konfiguracji Firebase

Wdrożenie funkcji, reguł i indeksów:

```bash
firebase deploy --only functions,firestore:rules,firestore:indexes,storage
```

Pełne wdrożenie powinno być wykonywane tylko wtedy, gdy wszystkie elementy zostały wcześniej zweryfikowane.

---

## 16. GitHub Actions

Projekt wykorzystuje workflow GitHub Actions do automatycznego:

* budowania aplikacji Android,
* uruchamiania testów jednostkowych,
* budowania Cloud Functions,
* uruchamiania testów backendu,
* generowania artefaktu APK.

Workflow uruchamia się podczas:

* wysłania zmian na gałąź `main`,
* wysłania zmian na gałąź `develop`,
* utworzenia lub aktualizacji Pull Requesta do wskazanych gałęzi,
* ręcznego uruchomienia przez `workflow_dispatch`, jeżeli zostało skonfigurowane.

Pliki workflow znajdują się w katalogu:

```text
.github/workflows/
```

W repozytorium używane są między innymi workflow:

```text
android.yml
firebase-functions-ci.yml
```

---

## 17. Sekrety GitHub Actions

Sekrety konfiguruje się w ustawieniach repozytorium:

```text
Settings → Secrets and variables → Actions
```

Projekt może wymagać następujących sekretów:

```text
GOOGLE_SERVICES_JSON
```

Jeżeli aplikacja jest podpisywana w GitHub Actions, wymagane mogą być również:

```text
CI_KEYSTORE_BASE64
CI_KEYSTORE_PASSWORD
CI_KEY_ALIAS
CI_KEY_PASSWORD
```

---

## 18. Generowanie podpisanego APK

Podpisany APK wymaga pliku keystore oraz konfiguracji podpisu w Gradle.

Lokalny plik keystore nie powinien być przesyłany do repozytorium.

Odcisk SHA-1 klucza można sprawdzić poleceniem:

```bash
keytool -list -v -keystore NAZWA_PLIKU.jks -alias NAZWA_ALIASU
```

Odcisk SHA-1 należy dodać w ustawieniach aplikacji Android w Firebase, szczególnie gdy aplikacja korzysta z logowania Google.

---

## 19. Najczęstsze problemy

### Brak pliku `google-services.json`

Objaw:

```text
File google-services.json is missing
```

Rozwiązanie:

* pobrać plik z Firebase Console,
* umieścić go w katalogu `app`,
* w GitHub Actions utworzyć go z sekretu `GOOGLE_SERVICES_JSON`.

### Brak wymaganego indeksu Firestore

Objaw:

```text
The query requires an index
```

Rozwiązanie:

* utworzyć indeks wskazany w komunikacie,
* zapisać go w `firestore.indexes.json`,
* wdrożyć indeksy poleceniem:

```bash
firebase deploy --only firestore:indexes
```

### Logowanie Google nie działa w APK z CI

Należy sprawdzić:

* SHA-1 klucza użytego do podpisania APK,
* konfigurację Firebase Authentication,
* obecność właściwego `google-services.json`,
* nazwę pakietu aplikacji.

---

## 20. Procedura wydania nowej wersji

1. Pobrać najnowsze zmiany z gałęzi `develop`.
2. Uruchomić testy aplikacji Android.
3. Uruchomić testy Cloud Functions.
4. Zbudować aplikację.
5. Sprawdzić działanie logowania i głównych funkcji.
6. Utworzyć Pull Request z `develop` do `main`.
7. Poczekać na zakończenie workflow GitHub Actions.
8. Przeprowadzić przegląd kodu.
9. Scalić zmiany.
10. Wdrożyć wymagane elementy Firebase.
11. Sprawdzić logi po wdrożeniu.

---

## 21. Osoby odpowiedzialne

| Obszar              | Odpowiedzialność                         |
| ------------------- | ---------------------------------------- |
| Aplikacja Android   | rozwój interfejsu i logiki aplikacji     |
| Firebase Functions  | logika backendowa                        |
| Firestore i Storage | struktura danych i reguły bezpieczeństwa |
| GitHub Actions      | automatyzacja budowania i testowania     |
| Dokumentacja        | aktualizacja instrukcji operacyjnych     |

Dokumentację należy aktualizować po zmianie:

* struktury projektu,
* poleceń uruchomieniowych,
* wersji środowiska,
* sekretów,
* workflow CI/CD,
* nazw funkcji,
* procedury wdrożenia.
