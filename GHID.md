# NEO Launcher — ghid de pornire

Launcher Android futurist cu 4 moduri vizuale (Cyberpunk Neon, Hologramă de sticlă, HUD spațial, Matrix digital), efecte animate la atingere, wallpaper live, APK construit automat pe GitHub și panou de admin pentru reclame popup, banner, temă și actualizări.

Durează cam 20–30 de minute, o singură dată. Cel mai ușor e de pe un calculator.

---

## 1. Pune codul pe GitHub

1. Intră pe github.com → **New repository** → nume ex. `neo-launcher` → **Public** → Create.
2. În repo: **Add file → Upload files** și trage **tot conținutul** folderului `NeoLauncher` (nu folderul în sine). Apasă **Commit changes**.
3. Verifică să existe fișierul `.github/workflows/build.yml`. Folderele care încep cu punct se pierd uneori la upload. Dacă lipsește: **Add file → Create new file**, scrie ca nume `.github/workflows/build.yml` și lipește conținutul fișierului din arhivă.

## 2. Cheia de semnare (ca actualizările să se instaleze peste versiunea veche)

Deschide `SECRETE-GITHUB.txt` din arhiva separată **NeoLauncher-cheie-SECRETA.zip**.
În repo: **Settings → Secrets and variables → Actions → New repository secret** și adaugă cele 4 secrete:

| Nume | Valoare |
|---|---|
| `KEYSTORE_BASE64` | tot textul din `KEYSTORE_BASE64.txt` |
| `KEYSTORE_PASSWORD` | parola din fișier |
| `KEY_ALIAS` | `neo` |
| `KEY_PASSWORD` | parola din fișier |

Păstrează arhiva cu cheia într-un loc sigur și **nu o urca în repo**.

## 3. Firebase (serverul gratuit pentru admin)

1. console.firebase.google.com → **Add project** (Analytics poți dezactiva).
2. **Build → Realtime Database → Create database** → locație `europe-west1` → **Start in locked mode**. Copiază adresa bazei (ex. `https://neo-xxxx-default-rtdb.europe-west1.firebasedatabase.app`).
3. **Build → Authentication → Get started → Email/Password → Enable.**
4. **Authentication → Users → Add user**: emailul și parola ta de admin. Copiază **User UID** din listă.
5. **Realtime Database → Rules**: șterge tot, lipește conținutul `firebase-rules.json`, înlocuiește cele 3 apariții `PUNE_AICI_UID_ADMIN` cu UID-ul tău → **Publish**.
6. **⚙ Project settings → Your apps → </> (Web)** → dă-i un nume → Register. Copiază blocul `firebaseConfig` (îți trebuie la pasul 5).

## 4. Leagă aplicația de Firebase

În GitHub: **Settings → Secrets and variables → Actions → tab-ul Variables → New repository variable**

- Nume: `FIREBASE_DB_URL`
- Valoare: adresa bazei de la pasul 3.2

## 5. Panoul de admin

1. GitHub: **Settings → Pages** → Source: *Deploy from a branch* → Branch: `main`, folder `/docs` → Save.
2. După 1–2 minute, adminul e la `https://UTILIZATOR.github.io/neo-launcher/`
3. Firebase: **Authentication → Settings → Authorized domains → Add domain** → `UTILIZATOR.github.io`
4. Deschide adminul, lipește `firebaseConfig`, intră cu emailul și parola de admin.

## 6. Construiește și descarcă APK-ul

1. GitHub → tab-ul **Actions** → **Build APK** → **Run workflow** (pornește și singur la fiecare modificare).
2. După ~5 minute: pagina repo → **Releases** → descarcă `NeoLauncher.apk`.
3. Link permanent către ultima versiune (bun de pus în admin la „Actualizare” sau de trimis altora):
   `https://github.com/UTILIZATOR/neo-launcher/releases/latest/download/NeoLauncher.apk`

## 7. Instalare pe telefon

1. Deschide APK-ul și permite „Instalare din surse necunoscute” pentru browser.
2. Apasă butonul Home → alege **NEO Launcher** → **Întotdeauna**.
   (sau din aplicație: **◢ SYS → Setează NEO ca ecran principal**)

---

## Cum se folosește

- **Glisează în sus** sau butonul cu 9 puncte → toate aplicațiile, cu căutare.
- **Ține apăsat pe o aplicație** → adaugă/scoate din dock, info, dezinstalare.
- **Ține apăsat pe ecran** sau **◢ SYS** → setări: tema, nivelul efectelor (Oprit / Eco / Maxim), fundal, wallpaper live.
- **Atinge ecranul** → unde de lumină; în HUD stelele accelerează, în Matrix coloanele se trezesc sub deget.

## Ce controlezi din admin

| Secțiune | Ce face |
|---|---|
| Identitate și temă | textul din antet; tema implicită sau blocată pentru toți |
| Popup / Reclamă | imagine, titlu, mesaj, buton cu link, la câte ore apare, întârziere, maxim afișări; vezi afișări, click-uri și rata de click |
| Banner | o bandă cu text și link pe ecranul principal |
| Actualizare | anunță utilizatorii cu versiune veche și le dă linkul APK-ului nou (opțional obligatoriu) |

Telefoanele preiau modificările în maxim 5 minute, la întoarcerea pe ecranul principal.
Pentru o reclamă nouă apasă **NOU** la „ID campanie”, ca toți s-o vadă din nou și statisticile să pornească de la zero.

## Probleme frecvente

- **Build-ul e roșu în Actions:** deschide-l, copiază ultimele rânduri de eroare și trimite-mi-le.
- **„Permission denied” la Salvează în admin:** UID-ul din reguli nu e cel afișat sus în panou.
- **Adminul nu se loghează (auth/unauthorized-domain):** lipsește domeniul de la pasul 5.3.
- **Popup-ul nu apare:** verifică „Popup activ”, că imaginea/linkul încep cu `https://`, și că n-a apărut deja în intervalul de ore setat.
- **Actualizarea nu se instalează („conflict cu pachetul existent”):** secretele de semnare lipseau la un build. Dezinstalează o dată și instalează din nou; de acum încolo merge.
- **Adresa serverului:** dacă ai uitat variabila de la pasul 4, o poți scrie direct în aplicație la **◢ SYS → Server admin**.

## Notă despre reclame

Aplicația e distribuită direct (APK), deci regulile Google Play nu se aplică. Dacă vrei vreodată s-o publici în Play Store, popup-urile care apar peste ecranul principal intră sub politica Google despre reclame intruzive și ar trebui regândite.
