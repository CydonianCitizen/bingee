# AGENTS.md

## 1. Scopo del progetto

Questo repository contiene un'app Android open source, local-first, per tenere traccia di film, serie TV e, in una fase successiva, anime.

Stato corrente del codice: v1.3.0 / versionCode 11, scope concordato verificato e chiuso il
22 ottobre 2026 dopo audit globale finale Ponytail e allineamento documentale. History H1–H6 e la prima
prova TalkBack documentano la precedente UI mensile. La timeline orizzontale continua, compatta
e alternata approvata il 21 ottobre 2026 ha proprie verifiche automatiche e visuali. Il 22 ottobre
il proprietario conferma TalkBack su Dettagli e timeline attuale e approva questa interazione,
su HONOR / Android 13, 360 dp/font 1.0, UI inglese. Integrazione/offline/concorrenza passano nello
scope documentato: 11 regressioni native, due verifiche in processi distinti dopo seed separato,
535 JVM e gate host, senza modifiche di produzione. Il successivo punto 4 verifica 415 test Android
completi senza errori/salti, 15 test Home per lingua EN/IT e otto configurazioni Home controllate
a 320/360/411/600 dp, chiaro/scuro e font 1.0/1.3/1.5. Release non firmata con R8 effettivamente
eseguito; 535 JVM invariati riusati, Spotless/lint/debug/test passano. App personale e dati
preservati; pacchetti temporanei rimossi e inventario, nove impostazioni, dimensioni/densità
verificati invariati. Le varianti Home sono composizioni sul telefono, non prove su tablet fisico
o TalkBack manuale Home. L'audit non rileva nuovi blocchi dimostrati; limiti e rinvii restano
nel record di chiusura della roadmap. L'hardening globale v1.3.4 non è certificato da questa chiusura.
Il proprietario esclude la verifica su tablet dallo scope concordato: non è un'attività residua
o un requisito per chiudere v1.3.0. TalkBack manuale Dettagli/History resta completato e accettato.
Nessuna firma, installazione release, pubblicazione, commit o push eseguiti per questi gate.
Lo scope concordato di v1.2.5 / versionCode 9 resta verificato e chiuso.
La manutenzione v1.2.6 è verificata e chiusa nello scope concordato il 19 ottobre 2026, senza
modifiche di produzione o incremento della versione applicativa. Restano i rinvii documentati in roadmap.
Room 9 e backup JSON v3 con restore v1/v2/v3 restano invariati.
TMDB è l'unico provider runtime; animazione e anime presenti su TMDB seguono i normali percorsi
Movie/TV Series. Non esistono integrazione Jikan o modelli anime dedicati. La
[roadmap canonica](BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md) distingue lavoro completato,
prestazioni ancora da migliorare e gate di release. Le sezioni MVP descrivono i vincoli originari;
lo stato implementato è documentato in [architecture](docs/architecture.md).

La promessa fondamentale del prodotto è:

> La cronologia dell'utente appartiene all'utente e resta utilizzabile anche senza account, backend proprietario o continuità del servizio.

L'app deve permettere di:

- cercare titoli tramite provider esterni;
- aggiungerli alla libreria locale;
- registrare film, stagioni ed episodi visti;
- consultare le prossime uscite dei titoli seguiti;
- ricevere notifiche locali;
- esportare e ripristinare i propri dati.

La prima piattaforma è Android. iOS non fa parte dell'MVP Android e non deve condizionare prematuramente l'implementazione.

---

## 2. Priorità decisionali

Quando requisiti o documenti sono ambigui, applicare questo ordine di priorità:

1. integrità e portabilità dei dati dell'utente;
2. correttezza funzionale;
3. semplicità architetturale;
4. testabilità;
5. accessibilità e qualità UX;
6. prestazioni misurabili;
7. estensibilità futura;
8. raffinatezza visiva.

Non introdurre complessità per casi futuri non ancora pianificati.

---

## 3. Principi non negoziabili

### 3.1 Local-first

- Nessun account obbligatorio.
- Nessun backend proprietario nell'MVP.
- Libreria, progressi, rating, preferenze ed eventi di uscita devono essere persistiti localmente.
- Le schermate principali devono leggere lo stato da Room tramite `Flow`.
- La Home non deve dipendere da una chiamata remota sincrona per essere visualizzata.
- La perdita di connettività non deve impedire la consultazione dei dati già salvati.

### 3.2 Dati portabili

- L'export JSON completo e il relativo restore sono requisiti di prodotto, non funzioni secondarie.
- Il formato di backup deve essere versionato.
- Non esportare API key, token, identificativi del dispositivo o dati tecnici non necessari.
- Le importazioni devono essere validate prima di modificare il database.
- Il restore deve essere transazionale: o riesce interamente oppure non modifica lo stato persistito.

### 3.3 Nessun segreto nel repository

Non committare:

- API key;
- token;
- file locali contenenti credenziali;
- `local.properties`;
- keystore o password di firma;
- dati reali degli utenti.

Le credenziali necessarie allo sviluppo devono essere lette da configurazione locale esclusa da Git.

### 3.4 Provider esterni isolati

- La UI e il dominio non devono dipendere dai DTO di TMDB o Jikan.
- Ogni provider deve avere client, DTO, mapper e gestione errori separati.
- Gli identificativi esterni devono essere sempre qualificati dalla fonte.
- Non assumere che due provider rappresentino la stessa opera nello stesso modo.
- Non introdurre deduplicazione automatica TMDB/Jikan finché non esiste una specifica e una suite di test dedicata.

---

## 4. Scope corrente

### MVP Android

Il primo percorso verticale usa TMDB e comprende:

- configurazione della API key TMDB;
- ricerca di film e serie TV;
- dettaglio titolo;
- aggiunta/rimozione dalla libreria;
- stagioni ed episodi per le serie;
- stato visto/non visto;
- rating a livello di opera;
- calendario locale delle prossime uscite;
- aggiornamento periodico tramite WorkManager;
- notifiche locali;
- export e restore JSON;
- tema chiaro/scuro e accessibilità di base.

### Fuori scope per l'MVP

Non implementare salvo milestone esplicita:

- account e autenticazione;
- backend e sincronizzazione cloud;
- componenti social;
- commenti, follower o messaggistica;
- disponibilità streaming/JustWatch;
- raccomandazioni personalizzate;
- sincronizzazione Android/iOS;
- Jikan e strutture specifiche per anime;
- import automatico da servizi terzi o formati non pianificati; l'import manuale sperimentale del
  solo profilo TV Time JSON ZIP documentato è già approvato da
  [ADR 0019](docs/adr/0019-tv-time-import-implementation.md);
- deduplicazione cross-provider;
- precisione delle notifiche al minuto;
- architettura a microservizi;
- eccessiva modularizzazione Gradle.

---

## 5. Stack e vincoli tecnici

Usare, salvo decisione documentata tramite ADR:

- Kotlin;
- Jetpack Compose;
- Material 3;
- coroutines e `Flow`;
- Room;
- Retrofit e OkHttp;
- Hilt;
- WorkManager;
- Navigation Compose;
- test unitari JVM;
- test strumentali solo per flussi ad alto valore o integrazioni Android-specifiche.

Prima di aggiungere una libreria:

1. verificare se il problema è già risolvibile con AndroidX/Kotlin;
2. motivare il beneficio concreto;
3. valutare manutenzione, licenza e dimensione;
4. evitare dipendenze che duplicano responsabilità già presenti.

---

## 6. Organizzazione del codice

Preferire inizialmente un modular monolith leggero. Non creare molti moduli Gradle senza necessità misurabile.

Struttura consigliata:

```text
app/
  core/
    common/
    model/
    database/
    network/
    designsystem/
  data/
    tmdb/
    library/
    settings/
    importexport/
  domain/
    search/
    details/
    library/
    calendar/
    progress/
  feature/
    onboarding/
    home/
    search/
    details/
    library/
    settings/
    importexport/
```

Regole:

- dipendenze dirette dalla UI verso Retrofit o DAO sono vietate;
- i composable non contengono logica di persistenza o networking;
- i ViewModel espongono uno `UiState` immutabile;
- gli eventi UI entrano tramite metodi espliciti o intent tipizzati;
- i repository sono il confine tra dominio e fonti dati;
- i mapper non vanno nascosti dentro composable o ViewModel;
- usare classi piccole e coese invece di “manager” generici.

---

## 7. Modello dati di dominio

Non usare un unico `MediaItem` per ricerca, dettaglio, database e UI.

Separare almeno:

- `MediaSearchResult`;
- `MediaDetails`;
- `LibraryEntry`;
- `Season`;
- `Episode`;
- `WatchProgress`;
- `ReleaseEvent`;
- `ExternalMediaRef`.

Identità esterna minima:

```kotlin
enum class MediaSource { TMDB, IMDB }

data class ExternalMediaRef(
    val source: MediaSource,
    val externalId: String,
)
```

Regole:

- non usare il solo `externalId` come chiave globale;
- il tipo media completa l'identità del titolo: TMDB può assegnare lo stesso ID a un film e a una
  serie non correlati. `IMDB` è un riferimento qualificato, non un secondo provider runtime;
  la navigazione richiede il riferimento TMDB. TVDB, IMDb e UUID TV Time di provenienza import
  restano nei namespace separati dell'import, secondo ADR 0019;
- non usare titolo e anno come identità persistente;
- mantenere un ID locale stabile per entità possedute dal database;
- salvare gli ID esterni di stagioni ed episodi quando il provider li espone;
- gestire speciali e stagione zero senza assunzioni implicite.

---

## 8. Database Room

### Requisiti

- Schema esportato e versionato nel repository.
- Migrazioni esplicite: non usare destructive migration nelle build di produzione.
- Foreign key e indici dichiarati consapevolmente.
- Scritture multi-tabella racchiuse in transazioni.
- Date e timestamp archiviati in formati non ambigui.
- Query osservabili tramite `Flow` per lo stato usato dalla UI.
- Ogni modifica futura allo schema richiede incremento versione, migrazione non distruttiva, schema esportato aggiornato e test di migrazione.

### Entità concettuali attese

- `media_entries`;
- `external_refs`;
- `seasons`;
- `episodes`;
- `library_entries`;
- `watch_progress`;
- `release_events`;
- `sync_metadata` o timestamp equivalenti;
- preferenze tramite DataStore, salvo ragione specifica per Room.

La ragione approvata per le preferenze portabili è il restore transazionale: tema, lingua, spoiler,
sei modalità della raccolta e categorie/anticipo notifiche sono in Room
([ADR 0031](docs/adr/0031-portable-user-preferences-backup-v3-room-v8.md)). Enablement
notifiche, first-run e cursori tecnici di refresh restano in DataStore; i permessi Android restano
gestiti dal sistema. Lo schema reale versionato è sotto `app/schemas/`, distinto da questo elenco
concettuale e dai numeri storici delle milestone.

Non salvare un solo `next_air_date` per titolo: il calendario deve poter rappresentare più eventi.

---

## 9. Networking e cache

### Ricerca

- La ricerca è remota e non richiede cache persistente lunga.
- Applicare debounce lato UI/ViewModel.
- Cancellare la richiesta precedente quando cambia la query.
- Distinguere chiaramente stato vuoto, caricamento, risultato vuoto ed errore.

### Dettagli

- Usare strategia cache-first con una politica di freshness esplicita.
- Mostrare i dati locali disponibili anche se il refresh remoto fallisce.
- Non cancellare dati validi in seguito a un errore temporaneo del provider.

### Errori

Mappare gli errori infrastrutturali in errori di dominio/UI comprensibili:

- connettività assente;
- autenticazione/API key non valida;
- rate limit;
- risposta non valida;
- contenuto non trovato;
- errore sconosciuto.

Non mostrare stack trace o messaggi Retrofit direttamente all'utente.

---

## 10. WorkManager e notifiche

- WorkManager aggiorna dati e calendario; non è una garanzia di esecuzione esatta.
- Ogni worker deve essere idempotente.
- Usare retry e backoff solo per errori temporanei.
- Non ritentare indefinitamente errori di configurazione, come API key assente o invalida.
- Elaborare i titoli in lotti per limitare chiamate e durata del lavoro.
- Aggiornare `lastCheckedAt`/stato equivalente.
- La UI deve offrire anche aggiornamento manuale.
- Le notifiche devono rispettare i permessi Android e le preferenze dell'utente.
- Non notificare più volte lo stesso evento senza una ragione esplicita.

---

## 11. API key TMDB

- La key viene fornita dall'utente.
- Il formato supportato è il TMDB API Read Access Token; una v3 query API key non è supportata.
- Non inserirla in sorgenti, risorse, manifest o build pubbliche.
- Non includerla nei log, nei crash report, negli export o nei test snapshot.
- Conservare preferibilmente configurazione e segreto separati.
- Se si usa cifratura, basarla su Android Keystore e mantenerla piccola e testabile.
- Se la key manca, l'app deve mostrare uno stato configurabile e non andare in crash.
- Inserire l'attribuzione TMDB richiesta nelle informazioni dell'app.

---

## 12. UI, Compose e accessibilità

- I composable devono essere, quando possibile, stateless e previewable.
- Separare route/state holder da contenuto visuale.
- Non passare NavController nei componenti riutilizzabili.
- Usare string resources per tutto il testo utente.
- Non hardcodare dimensioni o colori senza motivazione.
- Supportare font scaling ragionevole.
- Aggiungere content description quando l'elemento non è decorativo.
- Garantire target di tocco adeguati.
- Fornire stati loading, empty, error e offline.
- Evitare animazioni che spostano in modo involontario l'intero layout.
- Material 3 Expressive può guidare lo stile, ma non deve bloccare l'MVP né ridurre l'accessibilità.

---

## 13. Concorrenza e stato

- Usare structured concurrency.
- Non creare scope globali non gestiti.
- Le operazioni concorrenti devono avere ownership e cancellazione chiare.
- I repository espongono funzioni `suspend` per operazioni one-shot e `Flow` per osservazione.
- Non usare `runBlocking` nel codice di produzione.
- Evitare race condition tra toggle rapidi, refresh e aggiornamenti del worker.
- Le scritture ottimistiche devono poter essere riconciliate o annullate in caso di fallimento.

---

## 14. Test

Ogni milestone deve aggiungere test proporzionati al rischio.

Priorità:

1. mapper DTO → dominio;
2. regole di progressione visto/non visto;
3. query e transazioni Room;
4. policy cache/freshness;
5. generazione e deduplicazione degli eventi di uscita;
6. validazione export/import;
7. gestione errori;
8. ViewModel e state reducer;
9. worker idempotenti.

Per ogni bug corretto aggiungere, quando praticabile, un test di regressione.

I test non devono dipendere da API reali. Usare fake o MockWebServer.

---

## 15. Qualità, build e definition of done

Prima di considerare completato un task, eseguire i comandi disponibili nel repository per:

- formattazione/lint;
- test unitari;
- test Room/migrazioni pertinenti;
- build debug;
- eventuali test strumentali richiesti dalla milestone.

Una modifica è completata solo se:

- soddisfa i criteri di accettazione;
- non introduce segreti;
- non rompe i flussi già completati;
- aggiunge o aggiorna i test necessari;
- aggiorna documentazione e schema quando cambia una decisione pubblica;
- lascia `git status` privo di artefatti non intenzionali.

Non dichiarare test o build riusciti senza averli eseguiti.

---

## 16. Modalità operativa per Codex

Per ogni task:

1. leggere `AGENTS.md`, il piano milestone e i file rilevanti;
2. controllare lo stato Git e non sovrascrivere modifiche utente;
3. riassumere l'obiettivo e individuare i criteri di accettazione;
4. ispezionare il codice prima di proporre nuove astrazioni;
5. implementare il più piccolo incremento verticale completo;
6. aggiungere test nello stesso cambiamento;
7. eseguire verifiche mirate e poi la build prevista;
8. controllare diff e file generati;
9. riportare cosa è cambiato, test eseguiti, limiti e rischi residui.

### Divieti operativi

- Non riscrivere grandi aree del progetto senza necessità.
- Non cambiare versioni SDK, plugin o dipendenze non pertinenti al task.
- Non usare placeholder silenziosi che simulano funzioni completate.
- Non disabilitare test per ottenere una build verde.
- Non rimuovere codice o dati utente senza evidenziare l'impatto.
- Non applicare migrazioni distruttive come scorciatoia.
- Non committare automaticamente salvo richiesta esplicita.
- Non aprire PR o pubblicare release salvo richiesta esplicita.

### Gestione delle ambiguità

Se un dettaglio non è specificato:

- scegliere la soluzione minima coerente con questo documento;
- annotare l'assunzione nel report finale;
- creare un ADR solo se la decisione è strutturale o difficile da invertire;
- non bloccare il lavoro per preferenze cosmetiche reversibili.

---

## 17. Git e commit

Quando viene richiesto un commit:

- un commit deve rappresentare un cambiamento coerente;
- usare messaggi in forma imperativa;
- evitare commit misti con refactor non correlati;
- non includere output di build, file IDE personali o configurazioni locali;
- non modificare la cronologia Git esistente senza richiesta esplicita.

Formato suggerito:

```text
feat(search): add TMDB media search
fix(database): preserve episode progress during refresh
test(import): cover invalid backup schema
refactor(details): isolate TMDB mapping
```

---

## 18. Documentazione e ADR

Aggiornare la documentazione quando cambiano:

- modello dati;
- formato export;
- policy cache;
- provider;
- requisiti di privacy;
- navigazione principale;
- comportamento notifiche;
- dipendenze architetturali.

Usare ADR brevi in `docs/adr/` per decisioni strutturali, ad esempio:

- strategia API key;
- schema identità locale/esterna;
- formato backup;
- introduzione di Jikan;
- separazione in moduli Gradle.

### Documentazione canonica e report temporanei

- La documentazione durevole di prodotto e architettura appartiene al repository ed è versionata:
  `docs/architecture.md`, `docs/roadmap.md`, privacy, formati di backup, schemi e ADR.
- I report generati da audit o review Codex sono artefatti temporanei, non fonti di verità del prodotto,
  e non devono essere salvati sotto `docs/` né committati per impostazione predefinita.
- Se un report va conservato temporaneamente, usare `.audit/`, che è esclusa da Git.
- Un rilievo diventa guida durevole solo quando viene incorporato deliberatamente nella documentazione
  canonica pertinente.

---

## 19. Licenze e open source

- Verificare la compatibilità della licenza di ogni dipendenza e asset.
- Non includere poster, loghi o dataset scaricati come contenuto statico senza diritto.
- Mantenere attribuzioni richieste dai provider.
- Aggiungere `LICENSE`, `NOTICE` se necessario, `CONTRIBUTING.md`, codice di condotta e policy per segnalazioni di sicurezza prima della pubblicazione ampia.
- Evitare il nome e il branding di servizi esistenti in modo da suggerire affiliazione ufficiale.

---

## 20. Documenti di riferimento

Ordine di lettura consigliato:

1. `AGENTS.md`;
2. `docs/roadmap.md`;
3. `docs/architecture.md`;
4. ADR pertinenti;
5. README e documentazione della feature interessata.

In caso di conflitto, `AGENTS.md` definisce i vincoli operativi; `docs/roadmap.md` definisce l'ordine di consegna e il lavoro rinviato; `docs/architecture.md` descrive lo stato corrente; gli ADR definiscono le decisioni architetturali approvate più recenti.

---

## 21. Audit e manutenzione con Ponytail

- Usare Ponytail per gli audit di architettura e qualità.
- Usarlo per individuare codice eccessivo, morto, ridondante o obsoleto.
- Eseguire un passaggio Ponytail di manutenzione prima di dichiarare completa ogni milestone.
- Verificare e classificare ogni rilievo rispetto a scope, piano e lavoro utente: non applicare raccomandazioni alla cieca.

### Chiusura v1.2.5 e rinvio concordato a v1.2.6

Per decisione dell'utente del 18 ottobre 2026, v1.2.5 si chiude con verifiche finali, suite Android
completa e allineamento documentale. Questi tre passaggi sono completati. Il nuovo audit globale,
con eventuali fix di over-engineering/dead code e studio delle ottimizzazioni future, appartiene a
v1.2.6: non è un gate residuo di v1.2.5 e non è stato eseguito nella chiusura.

Non riaprire le ottimizzazioni concluse senza una regressione dimostrata o una nuova richiesta.
L'animazione attuale del filtro Film/Serie è mantenuta per scelta dell'utente; i frame residui oltre
16,7 ms e le matrici prestazionali non eseguite sono limiti dichiarati, non blocchi di chiusura.
Le nuove ottimizzazioni richiedono comunque la scelta benefici/costi della sezione 22. Chiusura dello
scope non significa commit, firma, tag, push o pubblicazione, che richiedono una richiesta esplicita.

---

## 22. Direzione delle ottimizzazioni concordata con l'utente

Preferenza generale espressa il 18 ottobre 2026: privilegiare la riduzione dello spazio occupato dai
dati locali persistenti, accettando costi CPU/GPU/RAM aggiuntivi quando il risparmio è concreto e il
comportamento resta adeguato. Per spazio locale si intende occupazione su disco, distinta dalla RAM.
Restano prioritarie integrità, portabilità, correttezza e usabilità definite nelle sezioni precedenti;
non assumere risorse hardware illimitate sui dispositivi supportati.

- Misurare il risparmio in byte/MiB e percentuale su dati e condizioni comparabili. Distinguere
  database e relativi file, cache immagini, file temporanei e backup; un miglioramento dei tempi SQL
  o delle allocazioni RAM non dimostra una riduzione dello spazio su disco.
- Proporre ogni nuova direzione prestazionale con beneficio misurato o atteso, costi su CPU/GPU/RAM,
  latenza, comportamento offline e complessità, verifiche necessarie e criterio di arresto. Dichiarare
  esplicitamente ciò che è ancora un'ipotesi. L'utente sceglie le direzioni prima dell'implementazione;
  entro uno scope già approvato procedere autonomamente senza chiedere conferma per ogni modifica.
- Questa preferenza non autorizza automaticamente compressione, eliminazione di cache, modifiche a
  schema/backup o perdita di storico, progressi e metadati necessari all'uso offline. Ogni intervento
  conserva le garanzie già definite per dati, migrazioni e restore.
- Non inseguire simultaneamente minimi di spazio, CPU, GPU e RAM. Non sacrificare fluidità o stabilità
  per un risparmio marginale; non annullare un risparmio approvato per una micro-ottimizzazione opposta.
  Presentare all'utente i compromessi nuovi che superano quelli concordati.
- Fermarsi al criterio concordato, oppure scartare la candidata se il confronto non dimostra un
  beneficio utile. Le direzioni non scelte restano proposte o rinviate: non diventano automaticamente
  nuovi requisiti di chiusura di v1.2.5. Riaprire attività verificate solo per una regressione dimostrata
  o una nuova richiesta dell'utente; mantenere separati difetti, verifiche mancanti e miglioramenti facoltativi.

### Interventi sullo spazio approvati il 18 ottobre 2026

- Esportare JSON senza indentazione, conservando contenuto, schema backup v3, limite di 50 MiB e
  restore v1/v2/v3. Il costo accettato è la minore leggibilità manuale del file, senza introdurre un
  formato compresso.
- Eliminare all'avvio le sole copie ZIP private abbandonate dell'import TV Time, proteggendo le copie
  degli import attivi e conservando gli archivi originali. Pulizia fuori dal thread principale e
  best effort, anche in caso di errori di cancellazione.
- Chiudere questi interventi dopo test di regressione/restore, confronto dei byte sulla stessa
  libreria sintetica e verifica di interruzione/riavvio. Limite cache poster, numero di condivisioni
  recenti e compattazione dello schema database restano proposte non approvate.
