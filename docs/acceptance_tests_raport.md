# Raport z testów integracyjnych — Live-GeoGuessr

## 1. Informacje podstawowe

| Pole | Wartość |
|---|---|
| Nazwa projektu | Live-GeoGuessr |
| Gałąź | `main` |
| Wersja aplikacji | 1.0 |
| Hash commita | build testowy z gałęzi `main` |
| Data wykonania testów | 15.06.2026 |
| Osoba wykonująca testy | Marcin Wiśniowski |
| Środowisko Firebase | produkcyjne |
| Urządzenie / emulator | Xiaomi Redmi Note 8 Pro |
| Wersja Androida | Android 11 |
| Rodzaj połączenia | Wi-Fi |

## 2. Cel testów

Celem testów integracyjnych było sprawdzenie współpracy aplikacji mobilnej Live-GeoGuessr z następującymi usługami i warstwami:

- Firebase Authentication,
- Cloud Firestore,
- Firebase Storage,
- Cloud Functions,
- warstwą repozytoriów aplikacji Android,
- interfejsem użytkownika,
- aparatem urządzenia,
- lokalizacją GPS.

Testy obejmowały główne przepływy biznesowe aplikacji oraz wybrane przypadki błędne i próby wykonania niedozwolonych operacji.

## 3. Zakres testów

Testami objęto:

- rejestrację i logowanie użytkownika,
- tworzenie i aktualizację profilu,
- wyszukiwanie użytkowników,
- wysyłanie i obsługę zaproszeń do znajomych,
- publikowanie postów ze zdjęciem i lokalizacją,
- odczyt postów znajomych,
- odgadywanie lokalizacji,
- naliczanie punktów,
- historię zgadnięć,
- usuwanie postów,
- usuwanie konta,
- kontrolę dostępu przez reguły Firestore,
- kontrolę dostępu przez reguły Storage,
- obsługę błędów sieciowych.

## 4. Środowisko testowe

### 4.1 Aplikacja mobilna

| Element | Wartość |
|---|---|
| System operacyjny | Android 11 |
| Model urządzenia | Xiaomi Redmi Note 8 Pro |
| Rozdzielczość ekranu | 2340 x 1080 |
| Wersja aplikacji | 1.0 |
| Tryb kompilacji | release |
| Sposób instalacji | Android Studio |

### 4.2 Backend

| Element | Wartość |
|---|---|
| Firebase Authentication | produkcyjne |
| Cloud Firestore | produkcyjne |
| Firebase Storage | produkcyjne |
| Cloud Functions | produkcyjne |
| Region Cloud Functions | us-central1 |

## 5. Kryteria oceny wyniku

| Status | Znaczenie |
|---|---|
| PASS | Wynik rzeczywisty jest zgodny z oczekiwanym |
| FAIL | Wynik rzeczywisty nie jest zgodny z oczekiwanym |
| BLOCKED | Test nie mógł zostać wykonany |
| NOT RUN | Test nie został wykonany |

---

## 6. Scenariusze testowe

### INT-01 — Rejestracja i utworzenie profilu

**Integracja:** Android → Firebase Authentication → Firestore

**Warunki początkowe:**

- konto testowe nie istnieje w Firebase Authentication,
- urządzenie ma aktywne połączenie z Internetem.

**Dane testowe:**

- adres e-mail: kamix004@gmail.com
- metoda uwierzytelnienia: e-mail

**Kroki:**

1. Uruchom aplikację.
2. Przejdź do ekranu rejestracji.
3. Wpisz adres e-mail i hasło.
4. Zatwierdź formularz rejestracji.
5. Sprawdź przekierowanie do głównego ekranu aplikacji.

**Oczekiwany rezultat:**

- konto zostaje utworzone w Firebase Authentication,
- dokument `users/{uid}` zostaje zapisany w Firestore,
- identyfikator `uid` odpowiada zalogowanemu użytkownikowi,
- statystyki początkowe mają poprawne wartości,
- znaczniki czasu są ustawione po stronie serwera.

**Rezultat rzeczywisty:**

Rejestracja zakończyła się poprawnie. Konto zostało utworzone, profil użytkownika został zapisany w Firestore, a aplikacja przekierowała użytkownika do głównej części systemu.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-01.jpg`

**Uwagi:** Brak

---

### INT-02 — Logowanie istniejącego użytkownika

**Integracja:** Android → Firebase Authentication → Firestore

**Warunki początkowe:**

- użytkownik posiada aktywne konto,
- profil użytkownika istnieje w Firestore.

**Dane testowe:**

- użytkownik: kamix004
- metoda logowania: e-mail

**Kroki:**

1. Uruchom aplikację.
2. Wpisz adres e-mail i hasło.
3. Zatwierdź formularz logowania.
4. Sprawdź pobranie profilu użytkownika.

**Oczekiwany rezultat:**

- użytkownik zostaje uwierzytelniony,
- aplikacja pobiera jego profil,
- użytkownik przechodzi do głównej części aplikacji.

**Rezultat rzeczywisty:**

Logowanie zakończyło się poprawnie. Aplikacja pobrała profil użytkownika i wyświetliła główny ekran.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-01.png`

**Uwagi:** Brak

---

### INT-03 — Wyszukiwanie użytkownika

**Integracja:** Android → Firestore

**Warunki początkowe:**

- w bazie istnieje co najmniej dwóch użytkowników,
- użytkownik testowy jest zalogowany.

**Dane testowe:**

- szukany nickname: Marcin Wiśniowski

**Kroki:**

1. Przejdź do zakładki znajomych.
2. Otwórz ekran dodawania znajomego.
3. Wpisz szukany nickname.
4. Sprawdź listę wyników.

**Oczekiwany rezultat:**

- aplikacja zwraca pasującego użytkownika,
- dane profilu są poprawnie wyświetlane,
- wyniki nie zawierają zalogowanego użytkownika.

**Rezultat rzeczywisty:**

Wyszukiwanie zwróciło poprawny profil użytkownika. Dane zostały wyświetlone prawidłowo.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-02.png`

**Uwagi:** Brak

---

### INT-04 — Wysłanie zaproszenia do znajomych

**Integracja:** Android → Cloud Functions → Firestore

**Warunki początkowe:**

- USER-A i USER-B istnieją,
- użytkownicy nie są znajomymi,
- nie istnieje aktywne zaproszenie.

**Dane testowe:**

- nadawca: kamix004
- odbiorca: Marcin Wiśniowski

**Kroki:**

1. Zaloguj się jako USER-A.
2. Wyszukaj USER-B na ekranie dodawania znajomych.
3. Naciśnij przycisk wysłania zaproszenia.
4. Sprawdź listę zaproszeń wychodzących.

**Oczekiwany rezultat:**

- zaproszenie zostaje utworzone przez Cloud Function,
- dokument pojawia się w `friendRequests`,
- nadawca widzi zaproszenie wychodzące,
- odbiorca widzi zaproszenie przychodzące,

**Rezultat rzeczywisty:**

Zaproszenie zostało utworzone przez Cloud Function. Nadawca widzi zaproszenie wychodzące, a odbiorca zaproszenie przychodzące.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-02.png`
- `docs/evidence/INT-03.png`

**Uwagi:** Brak

---

### INT-05 — Akceptacja zaproszenia do znajomych

**Integracja:** Android → Cloud Functions → Firestore

**Warunki początkowe:**

- istnieje zaproszenie USER-A → USER-B,
- USER-B jest zalogowany.

**Dane testowe:**

- nadawca: kamix004
- odbiorca: Marcin Wiśniowski

**Kroki:**

1. Zaloguj się jako USER-B.
2. Przejdź do zaproszeń przychodzących.
3. Zaakceptuj zaproszenie od USER-A.
4. Sprawdź listę znajomych obu użytkowników.

**Oczekiwany rezultat:**

- zaproszenie zostaje zaakceptowane,
- powstaje relacja w `users/{uid}/friends` dla obu użytkowników,
- zaproszenie zostaje oznaczone jako obsłużone,
- liczniki znajomych zostają zaktualizowane.

**Rezultat rzeczywisty:**

Zaproszenie zostało zaakceptowane. Relacja znajomości powstała po obu stronach, a licznik znajomych został zaktualizowany.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-02.png`
- `docs/evidence/INT-03.png`
- `docs/evidence/INT-04.png`
**Uwagi:** Brak

---

### INT-06 — Odrzucenie zaproszenia do znajomych

**Integracja:** Android → Cloud Functions → Firestore

**Warunki początkowe:**

- istnieje aktywne zaproszenie do znajomych,
- odbiorca zaproszenia jest zalogowany.

**Dane testowe:**

- nadawca: Marcin Wiśniowski
- odbiorca: kamix004

**Kroki:**

1. Zaloguj się jako odbiorca zaproszenia.
2. Przejdź do zaproszeń przychodzących.
3. Odrzuć zaproszenie.
4. Sprawdź listę znajomych i zaproszeń.

**Oczekiwany rezultat:**

- zaproszenie zostaje oznaczone jako odrzucone,
- relacja znajomości nie zostaje utworzona,
- obaj użytkownicy widzą aktualny stan.

**Rezultat rzeczywisty:**

Zaproszenie zostało odrzucone. Relacja znajomości nie została utworzona.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-05.png`
- `docs/evidence/INT-06.png`

**Uwagi:** Brak

---

### INT-07 — Publikowanie posta ze zdjęciem i lokalizacją

**Integracja:** aparat/GPS → Android → Storage → Firestore

**Warunki początkowe:**

- użytkownik jest zalogowany,
- aplikacja ma dostęp do aparatu,
- aplikacja ma dostęp do lokalizacji,
- urządzenie ma połączenie z Internetem.

**Dane testowe:**

- użytkownik: kamix004
- lokalizacja: lokalizacja pobrana z GPS
- rozmiar zdjęcia: poniżej 10 MB
- typ MIME: image/jpeg

**Kroki:**

1. Przejdź do ekranu aparatu.
2. Wykonaj zdjęcie.
3. Zatwierdź dodanie posta.
4. Poczekaj na wysłanie pliku do Storage.
5. Sprawdź widoczność posta w profilu.

**Oczekiwany rezultat:**

- zdjęcie zostaje zapisane w Firebase Storage,
- plik ma rozszerzenie `.jpg`,
- plik jest mniejszy niż 10 MB,
- dokument posta zostaje zapisany w `posts`,
- dokument zawiera poprawne współrzędne,
- `userId` odpowiada zalogowanemu użytkownikowi,
- licznik postów zostaje zaktualizowany.

**Rezultat rzeczywisty:**

Post został opublikowany poprawnie. Zdjęcie zapisano w Storage, dokument posta zapisano w Firestore, a licznik postów został zaktualizowany.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-07.png`
- `docs/evidence/INT-08.png`

**Uwagi:** Brak

---

### INT-08 — Odczyt posta przez znajomego

**Integracja:** Android → Firestore Rules → Storage Rules

**Warunki początkowe:**

- USER-A opublikował post,
- USER-B jest znajomym USER-A.

**Dane testowe:**

- autor posta: kamix004
- odbiorca posta: rendor535

**Kroki:**

1. Zaloguj się jako USER-B.
2. Przejdź do ekranu głównego.
3. Odśwież listę postów.
4. Otwórz post USER-A.

**Oczekiwany rezultat:**

- USER-B może odczytać dokument posta,
- USER-B może pobrać zdjęcie ze Storage,
- dane autora są poprawnie wyświetlane,
- aplikacja nie zgłasza błędu uprawnień.

**Rezultat rzeczywisty:**

Post znajomego został pobrany i wyświetlony poprawnie. Zdjęcie oraz dane autora są widoczne.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-09.png`

**Uwagi:** Brak

---

### INT-09 — Próba odczytu posta przez osobę obcą

**Integracja:** Android → Firestore Rules → Storage Rules

**Warunki początkowe:**

- USER-A opublikował post,
- USER-C nie jest znajomym USER-A,
- USER-C nie odgadł wcześniej tego posta.

**Dane testowe:**

- autor posta: kamix004
- użytkownik bez relacji: Marcin Wiśniowski

**Kroki:**

1. Zaloguj się jako USER-C.
2. Spróbuj pobrać post USER-A.
3. Sprawdź reakcję aplikacji.

**Oczekiwany rezultat:**

- USER-C nie może odczytać dokumentu posta,
- USER-C nie może pobrać zdjęcia,
- operacja kończy się kontrolowanym błędem,
- aplikacja nie ulega awarii.

**Rezultat rzeczywisty:**

Reguły dostępu zablokowały odczyt posta przez osobę obcą. Aplikacja obsłużyła odmowę dostępu bez awarii.

**Status:** PASS

**Dowody:**


**Uwagi:** Brak

---

### INT-10 — Wysłanie odpowiedzi i naliczenie punktów

**Integracja:** Android → Cloud Functions → Firestore

**Warunki początkowe:**

- USER-A opublikował aktywny post,
- USER-B jest znajomym USER-A,
- USER-B nie odgadł wcześniej tego posta.

**Dane testowe:**

- identyfikator posta: post testowy dostępny w aplikacji
- wskazana szerokość geograficzna: wartość wybrana na mapie
- wskazana długość geograficzna: wartość wybrana na mapie

**Kroki:**

1. Zaloguj się jako USER-B.
2. Otwórz post znajomego.
3. Wskaż lokalizację na mapie.
4. Wyślij odpowiedź.
5. Sprawdź ekran wyniku.

**Oczekiwany rezultat:**

- Cloud Function przyjmuje odpowiedź,
- odległość jest obliczana po stronie backendu,
- powstaje dokument `guesses/{uid}_{postId}`,
- wynik zostaje zapisany,
- statystyki użytkownika zostają zaktualizowane,
- aplikacja wyświetla wynik.

**Rezultat rzeczywisty:**

Odpowiedź została zapisana poprawnie. Backend obliczył odległość i punkty, a aplikacja wyświetliła wynik.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-10.png`

**Uwagi:** Brak

---

### INT-11 — Ponowna próba odgadnięcia tego samego posta

**Integracja:** Android → Cloud Functions → Firestore

**Warunki początkowe:**

- użytkownik odgadł już wskazany post.

**Dane testowe:**

- użytkownik: USER-B
- post: wcześniej odgadnięty post USER-A

**Kroki:**

1. Otwórz wcześniej odgadnięty post.
2. Spróbuj ponownie wysłać odpowiedź.
3. Sprawdź komunikat i stan danych w Firestore.

**Oczekiwany rezultat:**

- ponowna próba zostaje odrzucona,
- nie powstaje drugi dokument zgadnięcia,
- punkty nie są naliczane ponownie,
- aplikacja wyświetla kontrolowany komunikat.

**Rezultat rzeczywisty:**

Ponowna próba została odrzucona. Nie utworzono drugiego zgadnięcia i nie naliczono punktów ponownie.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-10.png`

**Uwagi:** Brak

---

### INT-12 — Pobieranie historii własnych zgadnięć

**Integracja:** Android → Firestore Query → Firestore Rules

**Warunki początkowe:**

- użytkownik wykonał co najmniej jedno zgadnięcie.

**Dane testowe:**

- użytkownik: rendor535

**Kroki:**

1. Zaloguj się jako użytkownik z historią zgadnięć.
2. Przejdź do ekranu odgadniętych postów.
3. Odśwież listę.
4. Sprawdź widoczne wyniki.

**Oczekiwany rezultat:**

- użytkownik widzi własne zgadnięcia,
- zapytanie ogranicza wyniki do `userUid == currentUid`,
- użytkownik nie otrzymuje zgadnięć innych osób,
- odgadnięte posty mogą być odczytane zgodnie z regułami.

**Rezultat rzeczywisty:**

Historia zgadnięć została pobrana poprawnie. Lista zawiera wyłącznie dane zalogowanego użytkownika.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-11.png`

**Uwagi:** Brak

---

### INT-13 — Usunięcie własnego posta

**Integracja:** Android → Firestore → Storage

**Warunki początkowe:**

- użytkownik posiada opublikowany post.

**Dane testowe:**

- użytkownik: USER-A
- post: własny post testowy

**Kroki:**

1. Zaloguj się jako autor posta.
2. Przejdź do listy własnych postów.
3. Usuń wybrany post.
4. Sprawdź listę postów po odświeżeniu.

**Oczekiwany rezultat:**

- dokument posta zostaje usunięty,
- odpowiadający mu plik zostaje usunięty ze Storage,
- post znika z interfejsu,
- użytkownik bez uprawnień nie może usunąć posta.

**Rezultat rzeczywisty:**

Własny post został usunięty poprawnie. Post zniknął z interfejsu, a dane powiązane zostały usunięte.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-13.png`
- `docs/evidence/INT-12.png`

**Uwagi:** Brak

---


### INT-14 — Próba bezpośredniego zapisu do chronionych kolekcji

**Integracja:** klient → Firestore Rules

**Warunki początkowe:**

- użytkownik jest zalogowany.

**Dane testowe:**

- użytkownik: aktywny użytkownik testowy

**Kroki:**

1. Spróbuj bezpośrednio utworzyć dokument w `guesses`.
2. Spróbuj bezpośrednio utworzyć dokument w `friendRequests`.
3. Spróbuj bezpośrednio utworzyć dokument w `users/{uid}/friends`.
4. Spróbuj zmodyfikować istniejący post.

**Oczekiwany rezultat:**

- każda operacja zostaje odrzucona,
- operacje wymagające zaufania są możliwe wyłącznie przez Cloud Functions,
- dane w Firestore pozostają bez zmian.

**Rezultat rzeczywisty:**

Bezpośredni zapis do chronionych kolekcji został zablokowany. Dane pozostały bez zmian.

**Status:** PASS

**Dowody:**


**Uwagi:** Brak

---

### INT-15 — Obsługa braku połączenia z siecią

**Integracja:** Android → warstwa repozytorium → Firebase

**Warunki początkowe:**

- użytkownik jest zalogowany,
- aplikacja jest uruchomiona,
- połączenie z Internetem zostaje wyłączone.

**Dane testowe:**

- tryb sieci: brak połączenia

**Kroki:**

1. Uruchom aplikację przy aktywnym połączeniu.
2. Wyłącz Wi-Fi oraz transmisję danych.
3. Spróbuj wykonać operację wymagającą Firebase.
4. Sprawdź komunikat aplikacji.

**Oczekiwany rezultat:**

- aplikacja rozpoznaje brak połączenia,
- użytkownik otrzymuje czytelny komunikat,
- aplikacja nie ulega awarii,
- operacja może zostać ponowiona po odzyskaniu połączenia.

**Rezultat rzeczywisty:**

Brak połączenia został obsłużony poprawnie. Aplikacja nie uległa awarii i umożliwiła ponowienie operacji po przywróceniu sieci.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-14.png`

**Uwagi:** Brak

---

### INT-16 — Usunięcie konta i danych powiązanych

**Integracja:** Android → Cloud Functions → Authentication → Firestore → Storage

**Warunki początkowe:**

- użytkownik posiada konto,
- użytkownik posiada profil,
- użytkownik posiada co najmniej jeden post,
- użytkownik posiada relacje lub zgadnięcia.

**Dane testowe:**

- użytkownik: Marcin Wiśniowski

**Kroki:**

1. Zaloguj się na konto testowe.
2. Przejdź do ustawień.
3. Wybierz usunięcie konta.
4. Potwierdź operację.
5. Sprawdź wylogowanie i usunięcie danych powiązanych.

**Oczekiwany rezultat:**

- konto zostaje usunięte z Firebase Authentication,
- profil użytkownika zostaje usunięty,
- jego posty zostają usunięte,
- pliki użytkownika zostają usunięte ze Storage,
- relacje znajomości i zaproszenia zostają usunięte,
- powiązane dane nie pozostają osierocone.

**Rezultat rzeczywisty:**

Konto oraz dane powiązane zostały usunięte poprawnie. Aplikacja wylogowała użytkownika po zakończeniu operacji.

**Status:** PASS

**Dowody:**

- `docs/evidence/INT-15.png`
- `docs/evidence/INT-16.png`

**Uwagi:** Brak

---

## 7. Zbiorcze wyniki

| ID | Scenariusz | Status | Uwagi |
|---|---|---|---|
| INT-01 | Rejestracja i utworzenie profilu | PASS | Brak |
| INT-02 | Logowanie istniejącego użytkownika | PASS | Brak |
| INT-03 | Wyszukiwanie użytkownika | PASS | Brak |
| INT-04 | Wysłanie zaproszenia do znajomych | PASS | Brak |
| INT-05 | Akceptacja zaproszenia do znajomych | PASS | Brak |
| INT-06 | Odrzucenie zaproszenia do znajomych | PASS | Brak |
| INT-07 | Publikowanie posta ze zdjęciem i lokalizacją | PASS | Brak |
| INT-08 | Odczyt posta przez znajomego | PASS | Brak |
| INT-09 | Próba odczytu posta przez osobę obcą | PASS | Brak |
| INT-10 | Wysłanie odpowiedzi i naliczenie punktów | PASS | Brak |
| INT-11 | Ponowna próba odgadnięcia tego samego posta | PASS | Brak |
| INT-12 | Pobieranie historii własnych zgadnięć | PASS | Brak |
| INT-13 | Usunięcie własnego posta | PASS | Brak |
| INT-14 | Próba niedozwolonej modyfikacji profilu | PASS | Brak |
| INT-15 | Próba bezpośredniego zapisu do chronionych kolekcji | PASS | Brak |
| INT-16 | Obsługa braku połączenia z siecią | PASS | Brak |
| INT-17 | Usunięcie konta i danych powiązanych | PASS | Brak |

**Liczba wykonanych testów:** 16

**Testy zakończone sukcesem:** 16

**Testy zakończone niepowodzeniem:** 0

**Testy zablokowane:** 0

**Testy niewykonane:** 0

## 8. Wykryte błędy

Nie wykryto błędów podczas wykonywania testów integracyjnych.

| ID błędu | Powiązany test | Priorytet | Opis | Kroki odtworzenia | Status |
|---|---|---|---|---|---|
| Brak | Nie dotyczy | Nie dotyczy | Nie wykryto błędów | Nie dotyczy | Nie dotyczy |

## 9. Testy regresyjne

Nie wykonywano testów regresyjnych, ponieważ w trakcie testów integracyjnych nie wykryto błędów wymagających poprawek i ponownej weryfikacji.

| ID błędu | Powtórzony test | Wynik przed poprawką | Wynik po poprawce | Uwagi |
|---|---|---|---|---|
| Brak | Nie dotyczy | Nie dotyczy | Nie dotyczy | Brak |


## 10. Ograniczenia testów

- Testy zostały wykonane ręcznie.
- Testy nie są automatycznie uruchamiane przy każdym commicie.
- Nie wykonano testów dużego obciążenia.
- Nie wykonano testów na wszystkich wersjach Androida.
- Nie wykonano testów na wszystkich rozmiarach ekranów.
- Wyniki zależą od konfiguracji użytego środowiska Firebase.
- Testy wykonano na jednym fizycznym urządzeniu.
- Testy regresyjne nie były wykonywane, ponieważ nie wykryto błędów wymagających ponownej weryfikacji.

## 11. Wnioski

Wszystkie zaplanowane scenariusze testów integracyjnych zakończyły się wynikiem pozytywnym. Aplikacja poprawnie współpracuje z Firebase Authentication, Cloud Firestore, Firebase Storage oraz Cloud Functions.

Główne przepływy aplikacji, czyli rejestracja, logowanie, obsługa profilu, dodawanie znajomych, publikowanie postów, zgadywanie lokalizacji, naliczanie punktów, historia zgadnięć oraz usuwanie danych, działają zgodnie z oczekiwaniami.

Nie wykryto błędów funkcjonalnych ani problemów z uprawnieniami. Reguły Firestore i Storage poprawnie blokują niedozwolone operacje, a aplikacja obsługuje odmowę dostępu oraz brak połączenia bez awarii.

Zalecane dalsze testy obejmują automatyzację wybranych scenariuszy, testy na większej liczbie urządzeń, testy na różnych wersjach Androida oraz testy obciążeniowe backendu Firebase.

## 12. Akceptacja raportu

| Rola | Osoba | Data | Akceptacja |
|---|---|---|---|
| Osoba wykonująca testy | Marcin Wiśniowski | 15.06.2026 | TAK |
| Lider projektu | Kamil Węgrzyn | 15.06.2026 | TAK |
| Osoba odpowiedzialna za backend | Marcin Wiśniowski | 15.06.2026 | TAK |
