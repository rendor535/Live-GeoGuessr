Ocena projektu Live-GeoGuessr
Stan na 150620262001

Live-GeoGuessr oceniam jako rozbudowaną aplikację mobilną Android, przygotowaną w Kotlinie oraz Jetpack Compose, której główną ideą jest publikowanie zdjęć powiązanych z lokalizacją GPS oraz odgadywanie miejsca ich wykonania przez innych użytkowników. Aplikacja ma charakter społecznościowo-lokalizacyjny: użytkownik wykonuje zdjęcie, zapisuje lokalizację, udostępnia post znajomym, a inni użytkownicy próbują wskazać miejsce wykonania zdjęcia na mapie. Projekt wykorzystuje Firebase Authentication, Cloud Firestore, Firebase Storage oraz Firebase Cloud Functions, co jest rozwiązaniem sensownym dla aplikacji typu online-first. W takim modelu dane mają wartość przede wszystkim jako dane aktualne, współdzielone i synchronizowane z backendem, dlatego brak pełnej lokalnej bazy danych nie jest sam w sobie krytycznym problemem architektonicznym. Dla tej aplikacji ważniejsze są poprawna autoryzacja, komunikacja z backendem, obsługa zdjęć, map, lokalizacji, relacji znajomych oraz bezpieczeństwo dostępu do danych. 

Za szczególnie wartościowe uznaję realną integrację aplikacji mobilnej z Firebase, wykorzystanie funkcji urządzenia, takich jak aparat, GPS i mapy, oraz przygotowanie wielu ekranów funkcjonalnych w Jetpack Compose. Pozytywnie oceniam zastosowanie architektury zbliżonej do MVVM, użycie ViewModeli, StateFlow, repozytoriów danych oraz Hilt do wstrzykiwania zależności. Projekt został też wyraźnie poprawiony między wersjami: dodano testy jednostkowe repozytoriów  
i ViewModeli, rozbudowano dokumentację, przygotowano changelogi, dokumentację kontraktów API, raport testów akceptacyjnych, politykę prywatności oraz konfigurację CI/CD. Dobrze oceniam również to, że backend nie ogranicza się do prostego użycia Firestore po stronie klienta, lecz obejmuje Firebase Functions, walidację danych, reguły bezpieczeństwa Firestore/Storage i logikę operacji krytycznych, takich jak zgadywanie lokalizacji czy usuwanie konta. 

Najważniejsze braki projektu dotyczą nie tyle samej idei aplikacji, ile jej dojrzałości inżynierskiej. Nie wymagam w tym projekcie pełnej lokalnej bazy danych jako warunku poprawności, ponieważ Live-GeoGuessr jest aplikacją społecznościową zależną od aktualnych danych z backendu. Warto byłoby jednak dodać lekki cache ostatnich postów, profilu użytkownika i ustawień, aby poprawić komfort pracy przy słabym połączeniu. Podobnie pełny tryb offline nie jest kluczowy dla tej aplikacji, ale powinna istnieć lepsza obsługa stanów braku połączenia, mechanizmu ponawiania operacji i czytelnych komunikatów dla użytkownika. Istotniejsze braki to: brak powiadomień push, brak automatycznych testów UI/Compose, brak testów end-to-end, brak testów Firebase Functions, niepełna instrukcja uruchomienia projektu od zera oraz sporadyczne dowody na systematyczne code review. Nie stwierdziłem również prób żadnego prompt injection. Widziałem, że chyba agents.md był w gitignore, czyli pomagaliście sobie jakimś LLM’em - i ok, bo to też potrafi być użyteczne narzędzie i powinniście umieć z niego korzystać, tylko lepiej jak jest to robione jawnie i odpowiednio generowane zmiany są oznaczane w dokumentacji, bo łatwiej szukać błędów później. Pozatym mógłbym sprawdzić, czy dobrze skonfigurowaliście taki plik agenst.md. 

Reasumując, w zakresie oceny wspólnej projekt otrzymuje 84/100 punktów, co odpowiada ocenie 4.5. Punktacja uwzględnia fakt, że aplikacja jest funkcjonalna, ma dobrze dobrany backend, poprawnie wykorzystuje funkcje urządzenia i realizuje założenia aplikacji mobilnej znacznie powyżej poziomu minimalnego. Ocena nie jest niższa z powodu braku pełnej lokalnej bazy danych, ponieważ przy tym typie aplikacji nie byłaby ona centralnym elementem rozwiązania. Projekt traci punkty przede wszystkim za elementy, które realnie podniosłyby jego jakość profesjonalną: testy UI, testy backendu, powiadomienia, bardziej kompletne CI/CD dla Firebase Functions, lepszą obsługę błędów, pełniejszą instrukcję wdrożenia oraz lepiej udokumentowany proces pracy zespołu. 

Podział ról nie został formalnie zapisany w repozytorium w postaci jednoznacznego dokumentu zespołowego, dlatego ocena indywidualna została oparta na widocznych artefaktach pracy: historii zmian, pull requestach, branchach, dokumentacji, plikach źródłowych i charakterze wykonanych zadań. Na tej podstawie przyjmuję następujący najbardziej prawdopodobny podział odpowiedzialności: Kamiks0320 odpowiadał za rolę Lidera/PM lub obszar dokumentacyjno-organizacyjny, Sfafg za frontend i warstwę interfejsu użytkownika, natomiast rendor535 za backend, Firebase, CI/CD, testy i integrację techniczną. Ocena indywidualna odnosi się do widocznego wkładu w repozytorium, a nie do deklaracji zespołu. 

Rola Lidera/PM została oceniona na 95/100 punktów, co odpowiada ocenie 5.0. Wysoko oceniam przygotowanie opisu produktu, user stories, materiałów demonstracyjnych, changelogów, raportów testów, polityki prywatności oraz końcowej dokumentacji projektu. Projekt ma czytelny zakres funkcjonalny, zrozumiałą koncepcję produktu i dobrze przygotowane materiały prezentacyjne. Ograniczenia tej roli dotyczą głównie braku formalnej tablicy sprintów, jednoznacznej roadmapy, dokumentu odpowiedzialności zespołu oraz słabo widocznego procesu code review. Mimo tych braków wkład PM/dokumentacyjny jest bardzo dobrze udokumentowany i miał istotne znaczenie dla końcowej jakości projektu.

Aby rola Lidera/PM była jeszcze pełniej udokumentowana, należałoby uzupełnić projekt o jednoznaczny dokument TEAM.md lub CONTRIBUTING.md z przypisaniem ról i odpowiedzialności, backlog z priorytetami, milestone’y powiązane z wersjami, krótką roadmapę rozwoju, opis sprintów oraz reguły akceptacji pull requestów. Najbardziej wartościowe byłoby nie samo dopisanie kolejnych opisów, lecz pokazanie rzeczywistego procesu zarządzania jakością: komentarzy do PR, decyzji projektowych, uzasadnień zmian zakresu i listy problemów rozwiązanych przed oddaniem projektu. 
 

Rola Frontend Developera została oceniona na 88/100 punktów, co odpowiada ocenie 4.5. Frontend obejmuje wiele ekranów funkcjonalnych, poprawną nawigację, wykorzystanie Jetpack Compose, Material 3, ViewModeli i stanów UI. Pozytywnie oceniam implementację ekranów logowania, profilu, znajomych, aparatu, publikowania zdjęcia, zgadywania lokalizacji, listy postów i ustawień. W tej ocenie nie traktuję braku pełnej lokalnej bazy danych jako zasadniczej wady frontendowej, ponieważ dla tej aplikacji ważniejsze są integracja z backendem, obsługa aparatu, lokalizacji, map i aktualnych danych użytkowników. Punktację ogranicza natomiast brak automatycznych testów UI, niepełna strategia dostępności, częściowo niespójna obsługa błędów oraz brak w pełni jednolitego wzorca prezentacji stanów loading/error/empty w całej aplikacji. 
 

Aby rola Frontend Developera została oceniona na 5.0, należałoby dodać automatyczne testy UI/Compose dla kluczowych przepływów: logowania, publikowania zdjęcia, wyboru lokalizacji, zgadywania, obsługi znajomych i ustawień. Oczekiwałbym również ujednolicenia modelu stanów ekranu, np. przez wspólny wzorzec Loading/Error/Empty/Content, dodania bardziej przyjaznych komunikatów błędów zamiast technicznych wyjątków, rozszerzenia obsługi dostępności przez TalkBack, contentDescription, odpowiednie kontrasty i wielkości elementów dotykowych oraz dopracowania zachowania aplikacji przy słabym lub utraconym połączeniu z siecią. Dodatkowym elementem na ocenę maksymalną byłby lekki cache wybranych danych UI, np. profilu, ostatnich postów i ustawień, nie jako pełna baza offline, lecz jako poprawa doświadczenia użytkownika. 

Rola Backend Developera została oceniona na 84/100 punktów, co odpowiada ocenie 4.5. Backend oparty na Firebase Authentication, Firestore, Storage i Cloud Functions jest dobrze dopasowany do charakteru aplikacji. Dla systemu społecznościowego, w którym użytkownicy publikują zdjęcia, zgadują lokalizacje i korzystają z relacji znajomych, centralny backend jest rozwiązaniem uzasadnionym. Pozytywnie oceniam implementację funkcji chmurowych, reguły bezpieczeństwa, walidację danych, obsługę zgadywania, usuwanie konta oraz przeniesienie części operacji krytycznych poza klienta. Brak klasycznego ORM i migracji nie powinien być silnie penalizowany, ponieważ projekt korzysta z modelu Firestore, w którym struktura danych i wdrożenie różnią się od aplikacji relacyjnych. Ocenę ogranicza natomiast brak automatycznych testów Firebase Functions, brak pełnego pipeline’u deploymentowego backendu oraz niepełna dokumentacja operacyjna środowiska Firebase. 

Aby rola Backend Developera została oceniona na 5.0, należałoby dodać automatyczne testy Firebase Functions obejmujące walidację danych wejściowych, autoryzację, odmowę operacji niedozwolonych, zgadywanie lokalizacji, naliczanie punktów, usuwanie konta i obsługę błędów. Warto byłoby również rozszerzyć CI/CD o część backendową: lint TypeScript, build funkcji, uruchamianie testów Functions, test reguł Firestore/Storage oraz opcjonalny deployment na środowisko testowe. Dodatkowo oczekiwałbym dokumentacji operacyjnej backendu: opisu kolekcji Firestore, indeksów, reguł dostępu, wymaganych zmiennych środowiskowych, procedury wdrożenia, procedury rollbacku oraz zasad monitorowania błędów. Przy modelu Firebase nie jest konieczny klasyczny ORM, ale na ocenę maksymalną potrzebny byłby jawnie opisany model danych, wersjonowanie zmian w strukturze kolekcji oraz testy reguł bezpieczeństwa. 

Podsumowanie punktacji 

Obszar / rola | Punkty | Ocena |
Komentarz


Architektura i kod | 18/20 | bardzo dobra

MVVM, Hilt, StateFlow, wiele ekranów, dobra modularność; brak pełnej
warstwy use case.


Dane i komunikacja 16/20 dobra plus

Firebase jest trafnym wyborem dla aplikacji online-first; brak Room nie
jest krytyczny, ale przydałby się cache i lepsza obsługa offline.


UI/UX | 12.5/15 | dobra plus

Spójny interfejs, Material 3, tryb jasny/ciemny; dostępność i komunikaty
błędów wymagają dopracowania.


Funkcjonalności dodatkowe | 11/15 | dobra

Kamera, GPS, mapy, auth i funkcje zaawansowane; brak powiadomień push.


Testy i jakość | 13/15 | bardzo dobra

Dobre testy jednostkowe i CI; brak testów UI/E2E/backendu.


Dokumentacja i prezentacja | 13.5/15 | bardzo dobra

README, changelogi, API, raport testów, screenshoty; instrukcja
uruchomienia niepełna.


Ocena zespołowa | 84/100 | 4.5

Projekt dojrzały funkcjonalnie i architektonicznie spójny z ideą
aplikacji online-first.


Lider/PM | 95/100 | 5.0

Bardzo dobra dokumentacja i organizacja materiałów; słabsza formalizacja
sprintów i ról.


Frontend Developer | 88/100 | 4.5

Rozbudowany frontend Compose; do oceny 5.0 brakuje głównie testów UI,
dostępności i standaryzacji stanów aplikacji.


Backend Developer | 84/100 | 4.5

Trafny backend Firebase dla aplikacji społecznościowej; do oceny 5.0
brakuje testów Functions, backendowego CI/CD i dokumentacji operacyjnej.
