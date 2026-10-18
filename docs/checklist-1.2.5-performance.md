# v1.2.5 — Checklist prestazioni

Piano operativo derivato dall'audit indipendente del 10 ottobre 2026 sulla revisione
`522fc7ca3e1d54db7ef3f766c8c485424a920b1d` di v1.2.4.
Questo documento integra la [roadmap canonica](../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md)
per v1.2.5 / versionCode 9, insieme al cleanup e alla sincronizzazione della documentazione già previsti.
Il report completo e gli esperimenti restano artefatti temporanei esterni al progetto.

I rilievi derivano dal codice e da esperimenti SQLite sul PC; non sono misure delle prestazioni Android.
L'ordine indica la priorità di intervento, non una gravità P0/P1 dimostrata.
Creare questa checklist non completa nessuna implementazione o verifica di release.

## Stato di chiusura — 18 ottobre 2026

Lo scope concordato di **v1.2.5/code 9 è chiuso** dopo verifiche finali, suite Android completa e
allineamento documentale. Per scelta dell'utente, il nuovo audit globale e lo studio delle ulteriori
ottimizzazioni passano a **v1.2.6**. Nessuna ottimizzazione conclusa viene riaperta da questa checklist.
La chiusura non equivale a pubblicazione, firma o commit.

| Area | Esito dello scope v1.2.5 | Limiti e decisioni conservati |
|---|---|---|
| Baseline | Fixture e confronti circoscritti registrati | La matrice completa di ogni metrica non è stata eseguita |
| Widget | Ownership, launcher HONOR, callback, resize, poster sintetici, EN/IT e due difetti corretti | Altri launcher, poster TMDB reali, font maggiorati e TalkBack completo non certificati |
| Osservazioni UI | Stop/ripartenza del Profilo verificati sul telefono, stato e aggiornamenti conservati | Sospensione di ulteriori owner Home/Details resta studio futuro; nessun risparmio batteria dichiarato |
| Room | Condivisione e filtro anticipato delle serie complete verificati; costo SQL ridotto nel caso misurato | Compromesso misurato negli altri carichi conservato; cache fredda e selezione unica prossimo/ultimo non implementate |
| Compose | Liste lazy e regressioni funzionali completate; miglioramento delle espansioni misurato | Non completata ogni combinazione eventi/immagini/allocazioni |
| Collection | Filtro/sort fuori Main, cancellazione e risultati correnti verificati | Matrice completa input→risultato sui tre carichi rinviata |
| Refresh episodi | Rimozioni dirette delle chiavi e integrità dei progressi verificate | Benchmark CPU/transazioni su tutti i carichi rinviato |
| Statistics | Ritenzione, caricamento, navigazione e primi cambi filtro corretti e verificati | Animazione attuale mantenuta per scelta dell'utente; sforamenti residui oltre 16,7 ms accettati, nessuna garanzia universale di frame/batteria |
| Backup | Pipeline circoscritta, JSON compatto e restore/rollback grande verificati | Non misurato il picco continuo heap/GC su tutta la matrice; nessuna riscrittura automatica |
| Spazio locale | JSON senza indentazione e pulizia ZIP abbandonati approvati, implementati e verificati | Cache poster più piccola, compattazione schema e altre riduzioni restano proposte non approvate |

Le caselle non spuntate nelle sezioni storiche sottostanti identificano prove o opportunità non
eseguite, non gate residui di v1.2.5. Non trasformarle in risultati positivi né in nuovi task automatici.
Le note datate descrivono lo stato di quel passaggio; i precedenti riferimenti ad audit/gate aperti
sono superati da questa decisione. Si riapre un intervento concluso solo per regressione dimostrata
o richiesta esplicita; nuove direzioni richiedono benefici/costi e scelta dell'utente (AGENTS.md §22).

## 0. Preparazione e baseline

- [x] Ricontrollare stato Git, codice e chiamanti: riconfermare i rilievi sulla revisione da modificare.
- [x] Impostare v1.2.5 / versionCode 9 all'avvio dell'implementazione della milestone.
- [x] Preparare dati sintetici riproducibili; nessun dato utente, credenziale o API reale nei test.
- [x] Registrare dispositivo fisico, Android, refresh rate, revisione/APK, build release ottimizzata,
      stato ART, temperatura e cache/rete per i confronti temporali.
- [ ] Misurare prima gli scenari interessati usando strumenti esistenti: query/observer per azione,
      CPU/Main, frame, allocazioni/heap e aggiornamenti widget secondo il punto.
- [x] Tenere trace, log e risultati temporanei in `.audit/`, esclusa da Git.
- [x] Separare misure Android, conteggi deterministici ed esperimenti sul PC; se manca hardware,
      registrare il limite senza dichiarare benefici di fluidità o batteria.

Applicare un cambiamento coerente per volta. Le osservazioni della UI, le sessioni Glance e il lavoro
background hanno ownership diverse: non usare una politica unica per tutti.

Il 10 ottobre 2026 è stata eseguita una baseline parziale su HONOR CMA-LX1 / Android 13 a 60 Hz,
con release ottimizzata in un package sintetico separato: 100/1.000/10.000 titoli,
1.000/25.000/100.000 episodi visti, 30 avvii e nove cicli di espansione/collasso.
Ricerca finale e conteggi Statistics corretti sui tre carichi; l'espansione cresce sensibilmente
di costo tra 24, 100 e 300 episodi, confermando il rilievo del punto 4.
Fixture, condizioni, log e trace restano temporanei in `.audit/v1.2.5-synthetic/`.
La baseline completa e i fix restano aperti: non sono ancora misurati tutti gli scenari,
le immagini sono assenti e non è dimostrata una regressione introdotta dal cleanup.

Il batch software dell'11 ottobre implementa gli interventi principali dei punti 1–7 e limita le coroutine del refresh
manuale dopo un conteggio sintetico di 10.000 job in attesa. Le spunte delle azioni implementate
non certificano l'intera sezione: confronti fisici, matrice immagini/widget e misure temporali restano
distinti. Le regressioni mirate verificano risultati, progressi, ownership e virtualizzazione.

Il 12 ottobre il confronto release R8 su HONOR CMA-LX1 / Android 13 ha ripetuto 60 espansioni
di stagioni da 24/100/300 episodi, 20 avvii e 20 ricerche su 10.000 titoli, in ordine prima/dopo/dopo/prima.
Con 300 episodi, la mediana del P99 gfxinfo passa da 400 a 31,5 ms e la PSS a stagione espansa
da circa 240 a 91 MiB. Avvio e scroll della fixture senza immagini non mostrano un beneficio
di fluidità chiaro. I due snapshot Statistics dopo hanno circa 10 MiB di PSS in più: allocazioni,
oggetti trattenuti e GC nel riavvio dei producer restano da indagare. Queste prove parziali non
completano la matrice immagini/widget, l'heap backup o il release gate; gli artefatti restano in `.audit/`.

## 1. Widget: evitare lavoro senza istanze e aggiornamenti irrilevanti

Riferimenti: [BingeeApplication](../app/src/main/java/com/cydoniancitizen/bingee/BingeeApplication.kt),
[BingeeWidgets](../app/src/main/java/com/cydoniancitizen/bingee/feature/widget/BingeeWidgets.kt),
[WidgetData](../app/src/main/java/com/cydoniancitizen/bingee/feature/widget/WidgetData.kt).

- [x] Rilevare le istanze installate tramite le API Glance e sospendere le osservazioni dell'updater
      quando non esiste alcun widget.
- [x] Riattivare correttamente l'updater se un widget viene aggiunto a processo già vivo;
      gestire anche la rimozione dell'ultima istanza.
- [x] Confrontare il contenuto effettivamente mostrato prima di `updateAll`, anziché le liste complete.
- [x] Conservare gli observer delle sessioni Glance attive e il callback MarkNextEpisode.
- [x] Verificare zero/uno/due tipi di widget, aggiunta/rimozione senza riavvio, cambio tema/giorno,
      cambio visibile e modifica a titoli/eventi non visibili.

**Completato quando:** senza widget non partono observer dati dell'updater; modifiche irrilevanti
non generano aggiornamenti visuali; contenuti rilevanti e callback restano corretti, anche in release minificata.

Il primo controllo mirato del 18 ottobre distingue la logica dell'updater dalla matrice sul launcher.
I test JVM coprono zero/uno/due tipi, aggiunta/rimozione/sostituzione a processo vivo, assenza di
collector duplicati, cambi tema/giorno e contenuti visibili o irrilevanti. Non viene aggiunto codice
widget senza un difetto riprodotto. In quel passaggio, posizionamento, rendering/immagini, resize e callback sulla
release minificata restavano da completare sul launcher reale; segue la successiva verifica fisica.

La successiva verifica sul launcher HONOR reale usa un APK R8 non debuggable isolato, dati e poster
sintetici, larghezza 360dp e font scale 1.0. Passano zero/uno/due tipi, rimozione/aggiunta nello stesso processo, un solo owner updater,
stop delle osservazioni dopo l'ultima rimozione, nessun aggiornamento visuale per metadati nascosti e
aggiornamento di entrambi i tipi per un titolo visibile. Entrambi i pulsanti segnano davvero l'episodio;
i tap aprono il dettaglio locale corretto di film e serie. Passano stati vuoti, temi Light/Dark, testi
visibili EN/IT, decodifica e sostituzione poster locali e fallback quando il file manca. Il resize
reale da 4×2 a 4×3 mostra una seconda uscita. Il cambio giorno, iniettato solo nella sorgente diagnostica
senza cambiare l'orologio del telefono, aggiorna soltanto il tipo con le uscite.

Restano due rilievi P3: in italiano la riga «Prossimo: S1 E2» viene troncata prima dei numeri, anche nel
widget espanso; durante un cambio lingua a caldo EN→IT controllato dalla sonda la descrizione accessibile del pulsante può
restare inglese mentre gli altri testi diventano italiani. Sono rilievi verificati, non correzioni
incluse in questo passaggio. Immagini HTTP/cache TMDB, altri launcher e una prova completa TalkBack
restano distinti dai casi eseguiti. Dopo la rigenerazione Room descritta sotto sono ripetute con
successo entrambe le scritture dai pulsanti sull'APK aggiornato. Pacchetto di prova rimosso; inventario
iniziale e tutti i dieci binding widget preesistenti conservati. Evidenze in `.audit/widget-launcher-db/`.

La successiva correzione del 18 ottobre risolve entrambi i rilievi: la riga del prossimo episodio può
andare a capo; snapshot e updater osservano anche la lingua portabile. Entrambi i widget usano un
contesto risorse nella lingua osservata e ricreano il contenuto al suo cambio, comprese le descrizioni
accessibili dei pulsanti. Sullo stesso HONOR a 360dp/font 1.0, APK R8 non debuggable, i numeri sono
visibili nel widget grande Light/Dark; le descrizioni dei due tipi seguono EN/IT a caldo nello stesso
processo. Passa anche il solo cambio della preferenza, senza `updateAll` manuale o cambio locale di
sistema. I pulsanti portano il progresso da 1 a 2, 3 e 4 episodi; dopo l'ultimo entrambi mostrano lo
stato vuoto italiano. La regressione JVM sul cambio lingua fallisce prima e passa dopo. Evidenze
in `.audit/widget-fixes/`. Restano fuori altri launcher, font maggiorati e TalkBack completo.

## 2. ViewModel: sospendere letture e calcoli senza consumatori UI

Riferimenti: [ProfileViewModel](../app/src/main/java/com/cydoniancitizen/bingee/feature/profile/ProfileViewModel.kt),
[ProfileScreen](../app/src/main/java/com/cydoniancitizen/bingee/feature/profile/ProfileScreen.kt),
[BingeeNavHost](../app/src/main/java/com/cydoniancitizen/bingee/core/navigation/BingeeNavHost.kt).

- [x] Contare gli observer nel percorso Your Bingee → Collection → Details → background → ritorno.
- [x] Legare le letture/calcoli del Profilo alla presenza di consumatori, con un breve grace period
      e conservazione dello stato già disponibile; riusare job e dispatcher esistenti.
- [x] Preservare la preview di Your Bingee e la condivisione dell'owner con Statistics,
      inclusi i percorsi aperti dai launcher shortcut.
- [x] Lasciare terminare le scritture richieste dall'utente anche quando la UI smette di raccogliere lo stato.
- [x] Verificare stop/ripartenza, assenza di sottoscrizioni duplicate, dati freschi al ritorno,
      contenuto cached, errori, retry e cancellazione con fake repository.
- [x] Estendere la stessa revisione a Home/Details solo se contatori e misure dimostrano lavoro evitabile.

**Completato quando:** dopo il grace period un producer UI senza consumatori non mantiene letture
costose attive; il ritorno non perde stato, aggiornamenti o operazioni utente.

Il 18 ottobre una regressione con due consumatori UI verifica un solo producer per sorgente:
rimuovere il primo consumatore non interrompe il secondo, anche dopo sei secondi. Dopo la rimozione
dell'ultimo, le letture restano attive per 4.999 ms e si fermano al termine dei cinque secondi.
Questa prima prova usa tempo virtuale e fake repository; il successivo controllo fisico è descritto sotto.

Il successivo controllo sullo stesso HONOR / Android 13, APK R8 non debuggable isolato e quattro titoli
sintetici, esegue la navigazione reale Your bingee → Collection → dettaglio Film → background → ritorno,
un ritorno rapido di navigazione, un background breve e tre cicli prolungati. Contatori passivi, letti
senza raccogliere `uiState`, verificano al massimo un producer di ciascun tipo per ProfileViewModel.
Dashboard e Collection hanno owner diversi: durante la tolleranza possono sovrapporsi due letture
Personal Viewing, mentre la query progressi condivisa resta sempre una. A regime rimane il solo owner
visibile. I ritorni rapidi riusano il producer; nei tre cicli lunghi i quattro producer del Profilo
si fermano 5.047–5.073 ms dopo l'ultimo consumatore. La query progressi ha una propria tolleranza di
cinque secondi e, senza altri owner, si ferma dopo 10.091–10.107 ms complessivi.

Una scrittura Room mentre il Profilo è fermo cambia un titolo e porta i film visti da uno a due:
al ritorno Collection, Statistics e dashboard leggono i dati nuovi nello stesso processo, con stato
cached disponibile e senza ripristinare il loading. Statistics riusa l'owner del dashboard.
Home e il dettaglio Film conservano alcune osservazioni finché i rispettivi ViewModel restano nel
back stack, anche a UI ferma; il dettaglio le rilascia quando viene rimosso. Il conteggio registra
questo comportamento preesistente, ma non misura il costo CPU/batteria: l'eventuale sospensione è
un'opportunità P2 da valutare separatamente, non una modifica implicita di questa verifica.
Il controllo fisico del ciclo di vita del Profilo è completato; nessun sorgente di produzione è
modificato e gli APK normali restano identici. Riutilizzato il gate valido 495 JVM/63 Android sul
codice invariato. Sonda rimossa, inventario app e dieci widget iniziali conservati. Evidenze in
`.audit/observer-lifecycle/`. Gli altri punti prestazionali e il gate globale restano distinti.

## 3. Room: ridurre aggregazioni e osservazioni ripetute

Riferimenti: [LibraryDao](../app/src/main/java/com/cydoniancitizen/bingee/data/library/local/LibraryDao.kt),
[DefaultLibraryRepository](../app/src/main/java/com/cydoniancitizen/bingee/data/library/DefaultLibraryRepository.kt),
[ContinueWatchingPolicy](../app/src/main/java/com/cydoniancitizen/bingee/domain/policy/ContinueWatchingPolicy.kt).

- [ ] Dopo i punti 1–2, misurare quali proiezioni rimangono duplicate per invalidazione.
- [x] Riutilizzare `regular_episode_activity` per runtime totale e runtime mancanti in Personal Viewing,
      evitando le due aggregazioni correlate aggiuntive; validare la candidata sul vero DAO Room.
- [x] Condividere l'osservazione Continue Watching per giorno se resta raccolta da più consumatori,
      conservando ripartenza, errori e cancellazione corretti.
- [ ] Valutare la selezione unica dell'identità del prossimo/ultimo episodio e l'esclusione di lavoro
      su titoli non eleggibili; adottare solo una query equivalente e più economica.
- [x] Applicare `distinctUntilChanged` prima dei mapping costosi dove evita riaggregazioni reali:
      documentare che non impedisce il rerun SQL dovuto all'invalidation Room.
- [x] Conservare la condivisione dei progressi già presente; non aggiungere indici o cache generiche
      senza evidenza dal query plan e dalle misure.
- [ ] Verificare più stagioni/riferimenti, speciali, copertura parziale, runtime null, date future/assenti,
      serie complete/abbandonate, identità TMDB qualificata per tipo, progressi e cambio giorno.
- [x] Confrontare query count e durata su librerie piccole/grandi, prevalentemente film o serie complete.

**Completato quando:** risultati DAO/domain equivalenti, nessun errore nascosto dagli observer condivisi
e riduzione verificabile di letture/trasformazioni; ogni beneficio temporale dichiarato è misurato su Android.

Il 18 ottobre il DAO reale su HONOR CMA-LX1 / Android 13 verifica che tre consumatori condividono
una query iniziale e una sola query aggiuntiva al cambio giorno. Un secondo caso con 100 film e
una serie riproduce 101 righe nella proiezione Continue Watching, benché la policy escluda già i film.
Il solo filtro SQL `media_type = 'SERIES'` riduce le righe restituite a una, conservando la prossima
puntata e tutti i 100 film nella raccolta. Il caso include un film e una serie con lo stesso ID TMDB.
La regressione fallisce prima e passa dopo; non cambia schema, policy di eleggibilità o identità.
Sono conteggi deterministici, non una misura di durata, frame o batteria. La selezione unica di
prossimo/ultimo episodio e i confronti temporali su librerie grandi restano aperti. Evidenze temporanee
in `.audit/branding-and-core-validation/`.

La successiva misura fisica del 18 ottobre usa HONOR CMA-LX1 / Android 13, SQLite 3.32.2,
release R8 non debuggable e ART `speed`. Nove fixture Room 9 coprono 100/1.000/10.000 titoli con
90% film, 90% serie complete o 50% serie in corso; il massimo è 270.000 episodi. Le tre fixture grandi
sono ripetute in ordine ruotato. Ogni prova riscalda entrambe le query e alterna blocchi ABBA/BAAB:
288 letture SQL complessive, 24 campioni per variante su ogni composizione grande, più 60 campioni
separati della materializzazione Room attuale. Si confronta la query Continue Watching con e senza
il solo predicato SERIES; le letture raw usano fonte/data costanti e attraversano tutto il cursor.

Mediane aggregate a 10.000 titoli, in millisecondi:

| Composizione | SQL prima | SQL attuale | Room attuale, inclusa materializzazione |
|---|---:|---:|---:|
| 90% film, 30.000 episodi | 407,74 | 190,75 | 199,05 |
| 50% serie in corso, 150.000 episodi | 1.701,18 | 965,10 | 1.004,11 |
| 90% serie complete, 270.000 episodi | 3.753,08 | 3.159,10 | 3.219,11 |

Le due prove grandi riproducono la direzione del beneficio. Per ogni fixture coincidono i digest
delle righe serie prima/dopo, il numero di righe raw/Room e l'eleggibilità di dominio. Tre consumatori
condividono una query iniziale e una per invalidazione dei metadati. Library Progress e Personal Viewing
hanno anche cinque campioni raw attuali per prova, senza un confronto dichiarato con la vecchia
aggregazione. Stato termico 0, batteria 27,1–28,7 °C; display adattivo osservato a 60/90 Hz, non forzato.
Questi sono tempi di lettura a cache calda, non latenza completa della UI, frame, batteria o cache OS fredda.

La proiezione delle serie complete resta costosa: calcola e materializza 9.000 righe poi escluse dalla
policy, ottenendo zero elementi Continue Watching. Selezione unica prossimo/ultimo ed esclusione
anticipata dei titoli non eleggibili restano quindi lavoro aperto; questa verifica non riscrive SQL.
I piani nativi EXPLAIN non sono disponibili nelle prove (`SQLITE_ROW / another row available`);
l'errore diagnostico è separato dalle letture e registrato, senza rivendicare una verifica dei piani.

Prima delle misure accettate viene rilevato output Room release obsoleto nonostante build riuscite.
I tentativi sono scartati: dopo rigenerazione KSP senza cache incrementale/build, le tre query generate
coincidono esattamente con i sorgenti attuali e i controlli nativi passano. Anche gli APK normali vengono
rigenerati e verificati, privi di sonde; schema, sorgenti di produzione e dipendenze restano invariati.
Gate standard riuscito; JVM/lint riusano risultati validi sullo stesso codice (494 test, zero errori lint,
37 warning). Dati, condizioni, singoli campioni, limiti e cleanup restano in `.audit/widget-launcher-db/`.
Il gate complessivo v1.2.5 e i confronti non coperti restano aperti.

### Correzione successiva: escludere prima le serie senza prossimo episodio — 18 ottobre

Un `EXISTS` nella query Continue Watching richiede un episodio regolare non visto, con data nulla o
non futura, prima delle proiezioni dettagliate. Le serie complete o in pari non vengono più
materializzate inutilmente. Non cambia la policy di dominio, né lo stato WATCHING di una serie con
cache incompleta: raccolta, statistiche e copertura continuano a usare le loro proiezioni complete.
Nessuna modifica a schema, migrazioni, dipendenze o formato backup.

Il confronto usa la query precedente, già filtrata su SERIES, e quella corretta nello stesso APK
R8/ART `speed` su HONOR. Stesse nove fixture e ripetizione dei tre casi grandi, blocchi ABBA/BAAB,
288 letture SQL e 60 letture Room separate. Le righe trattenute coincidono in tutti i campi;
le righe eliminate dalla query precedente non hanno un prossimo episodio regolare disponibile.
Tre consumatori conservano una query iniziale e una per invalidazione. Mediane aggregate a cache calda:

| Libreria da 10.000 titoli | SQL precedente, ms | SQL corretto, ms | Room corretto, ms |
|---|---:|---:|---:|
| 90% film | 190,21 | 196,71 | 206,03 |
| 50% serie in corso | 965,75 | 998,66 | 1.040,89 |
| 90% serie complete | 3.174,20 | 224,56 | 225,50 |

Il caso con 9.000 serie complete e 270.000 episodi restituisce ora zero righe, con costo SQL ridotto
del 92,9%; entrambe le ripetizioni confermano il risultato. Il controllo aggiuntivo costa circa il
3,4% nei due casi grandi ancora in corso: questo è il compromesso misurato, non un miglioramento
universale. Stato termico sempre 0, batteria 28,5–29,9 °C; display adattivo 60/90 Hz invariato.
Restano letture a cache calda, senza rivendicare tempi di schermata, frame o batteria. La selezione
unica prossimo/ultimo episodio e i costi delle altre proiezioni non sono modificati.

Il nuovo test Room fallisce prima e passa dopo: serie in pari escluse, ricomparsa all'uscita del
prossimo episodio o dopo unmark. Restano verificati cache incompleta, speciali, stato della raccolta,
backup e migrazioni: 63 test Android passati sul telefono. Passano 495 JVM, Spotless, lint senza errori
(38 warning) e build debug/AndroidTest/release normali. Le tre query Room generate coincidono con il
sorgente e gli APK normali non contengono la sonda. La revisione Ponytail non rileva problemi azionabili.
Pacchetti diagnostici rimossi, inventario app e dieci binding widget preesistenti preservati.
I due rilievi widget descritti sopra sono risolti; evidenze ignorate in `.audit/widget-fixes/`.
Il gate complessivo v1.2.5, cache fredda, altri launcher e TalkBack completo restano aperti.

## 4. Compose: virtualizzare singoli episodi ed eventi

Riferimenti: [TvSeriesSection](../app/src/main/java/com/cydoniancitizen/bingee/feature/details/TvSeriesSection.kt),
[HomeScreen](../app/src/main/java/com/cydoniancitizen/bingee/feature/home/HomeScreen.kt).

- [ ] Misurare espansione/scroll con stagioni da 24/100/300 episodi e date con 2/20/100 eventi.
- [x] Esporre header ed episodi come elementi distinti dello stesso `LazyListScope` in Details.
- [x] Esporre header di data ed eventi come elementi distinti della lista Home.
- [x] Usare key qualificate e contentType coerenti; conservare raggruppamento visuale, speciali e ordine.
- [x] Mantenere un solo scroller, senza liste annidate con altezza arbitraria.
- [x] Valutare l'animazione dell'espansione nel nuovo layout; preservare stabilità del contenuto,
      controllo di stagione, semantics, target di tocco e font scaling.
- [ ] Verificare toggle, espansione/collasso, scroll e ritorno; confrontare elementi composti,
      richieste immagini, allocazioni e frame con cache immagini vuota e popolata.

**Completato quando:** il lavoro cresce con viewport/prefetch anziché con tutto il gruppo;
nessuna regressione di accessibilità, navigazione o progressi.

## 5. Collection: spostare filtro e ordinamento dal Main

Riferimento: [ProfileViewModel](../app/src/main/java/com/cydoniancitizen/bingee/feature/profile/ProfileViewModel.kt).

- [ ] Misurare digitazione e cambio filtri/sort su 100/1.000/10.000 titoli.
- [x] Spostare filtro, ordinamento e calcoli delle preview nel dispatcher già disponibile,
      usando uno snapshot coerente di dati e parametri.
- [x] Aggiornare immediatamente il testo digitato e pubblicare solo il risultato della richiesta ancora valida.
- [x] Gestire cancellazione cooperativa dei cicli lunghi e invalidazioni Room durante il calcolo.
- [x] Verificare input rapido, sort/filter alternati, aggiornamenti concorrenti e risultato finale corretto.
- [ ] Confrontare tempo sul Main e latenza input→risultato; evitare Paging/FTS o cache aggiuntive
      se lo spostamento minimo è sufficiente.

**Completato quando:** il calcolo costoso non blocca il Main e un risultato vecchio non sovrascrive
la selezione o i dati correnti.

## 6. Refresh episodi: eliminare le scansioni quadratiche delle mappe

Riferimento: [SeriesDao](../app/src/main/java/com/cydoniancitizen/bingee/data/library/local/SeriesDao.kt).

- [x] Sostituire le due scansioni `entries.removeIf` per candidato con rimozioni dirette delle vecchie chiavi.
- [x] Leggere le chiavi prima di sostituire `state.episode` e rimuoverle solo se puntano allo stesso stato.
- [x] Conservare controlli di conflitto/appartenenza, ID locali, progressi e transazione unica.
- [x] Verificare refresh invariato, nuovo ID provider sullo stesso numero, duplicati e conflitti ref/numero.
- [ ] Confrontare conteggi/CPU e durata della transazione su 24/100/300/1.000 episodi.

**Completato quando:** le scansioni delle mappe non crescono più quadraticamente nel refresh descritto,
senza DELETE/REINSERT di entità o perdita di progressi.

## 7. Statistiche: aggregazione mensile e cancellazione cooperativa

Riferimenti: [WatchedStatistics](../app/src/main/java/com/cydoniancitizen/bingee/domain/model/WatchedStatistics.kt),
[ProfileViewModel](../app/src/main/java/com/cydoniancitizen/bingee/feature/profile/ProfileViewModel.kt).

- [x] Misurare l'aggregazione implementata con 1.000/25.000/100.000 attività; distinguere
      calcolo puro, caricamento Room/mappatura e ripartenza del producer UI nella fixture descritta sotto.
- [x] Sostituire i dodici filtri dell'intero storico con un passaggio e accumulatori mensili.
- [x] Conservare timezone, anno selezionato, anni disponibili, date future e indicatori di runtime mancante.
- [x] Rendere cooperativa la cancellazione dei grandi calcoli: `mapLatest` da solo non interrompe
      i cicli sincroni privi di sospensioni o controlli del Job.
- [x] Valutare i ricalcoli di rating/generi al cambio anno: circa 0,3 ms su 100 titoli nei tre carichi;
      nessuna cache aggiunta, perché il costo misurato non giustifica un intervento.
- [x] Verificare equivalenza statistica, storico fuori libreria, bordi anno/timezone e cancellazione durante il lavoro.
- [x] Evitare che gli snapshot UI dei titoli votati trattengano copie complete delle attività degli episodi;
      verificare memoria e correttezza dopo riaperture e cambi filtro.
- [x] Misurare presentazione del contenuto e frame durante ingresso, riapertura, ritorno dal background
      e cambio tipo media/anno, distinguendo dati pronti, dissolvenza e limiti della traccia.
- [x] Mantenere il caricamento attivo nei passaggi di navigazione entro la tolleranza di cinque secondi;
      evitare il ricalcolo dello storico al solo cambio tipo media e la finestra popup del filtro anno.
- [x] Individuare e correggere i blocchi del primo cambio Film/Serie dovuti alla compilazione del ripple,
      preservando feedback di pressione/focus, selezione e controlli Material.
- [x] Verificare la successiva riduzione dei frame Film/Serie oltre 16,666667 ms con radar popolati,
      separando soglia fissa, deadline Android e variabilità tra prove.

Il 17 ottobre l'indagine sull'HONOR CMA-LX1 / Android 13 usa una release R8 diagnostica isolata,
100 serie votate senza immagini e fixture da 1.000/25.000/100.000 attività. Con 100.000 attività,
20 cicli background/foreground oltre i timeout dei producer mantengono un solo ProfileViewModel:
l'heap Java dopo GC passa da 17,31 a 17,35 MiB, senza crescita progressiva nel percorso a filtro invariato.
Gli heap dump contengono però 200.000 WatchedEpisodeActivity: cache del ViewModel e stato UI dei voti
trattengono due storici distinti. Cambi filtro e successive riaperture portano il campione a 400.000
attività, trattenute anche da snapshot/lambda e slot Compose. Tornando da Statistiche e raccolta alla
Home, dopo GC scompaiono sia il ProfileViewModel sia tutte le attività osservate; in background le
righe Room delle attività vengono rilasciate. Le allocazioni cumulative mediane per riapertura sono
circa 4/42/157 MiB sui tre carichi: non sono picchi heap né tempi della sola aggregazione.
Le prove non ricostruiscono l'esatto confronto storico dei +10 MiB PSS e non certificano assenza di
leak in ogni percorso. La sola indagine ha identificato la ritenzione evitabile senza modifiche di
produzione. Fixture, APK/mapping, campioni, heap e analisi restano in `.audit/statistics-memory/`.

La successiva correzione rimuove le attività cronologiche dai risultati dei titoli votati, conservando
metadati, rating, conteggi e l'input completo dei calcoli. La regressione fallisce prima e passa dopo;
passano tutti i 487 test JVM e 42 test Android mirati sullo stesso HONOR, con Spotless, lint senza
errori e build debug/AndroidTest/release ottimizzata. Ripetendo le stesse fixture e gli stessi cicli,
gli oggetti attività passano da 2N–4N a N: gli snapshot UI ne trattengono zero, la cache dei calcoli
resta completa e le righe Room si liberano in background. Con 100.000 episodi, l'heap Java dopo GC
passa da 17,35 a 13,16 MiB dopo 20 riaperture e da 25,25 a 12,67 MiB nel controllo con filtri;
tornando alla Home scompaiono ancora tutte le attività e il relativo ViewModel. Il punto di ritenzione
è verificato e chiuso. Le allocazioni temporanee del ricaricamento restano circa 157 MiB sul carico
massimo: la correzione riduce la memoria viva, non dimostra benefici di frame/batteria né completa
le misure temporali dell'aggregazione e gli altri scenari della checklist. Report e confronto
verificabile restano in `.audit/statistics-memory/fix/`; nessun componente diagnostico entra nell'APK normale.

La successiva misura del 17 ottobre usa lo stesso HONOR, release R8 non-debuggable isolata,
ART compilato `speed`, cache disco calda, timezone Europe/Rome e le stesse 100 serie votate senza
immagini. Temperatura batteria 29,1–29,5 °C, stato termico Android 0. Dodici calcoli dopo warmup
alternano anno corrente/precedente; cinque letture per proiezione/repository sono separate dai
tre trial di memoria. Le mediane in millisecondi sono:

| Attività | Calcolo `calculateWatchedStatistics` | Room attività, inclusa materializzazione | Primo risultato repository | Solo mapping repository |
|---|---:|---:|---:|---:|
| 1.000 | 0,83 | 42,53 | 27,21 | 0,43 |
| 25.000 | 11,87 | 446,95 | 480,35 | 4,16 |
| 100.000 | 48,39 | 2.344,49 | 2.080,59 | 16,15 |

Le letture sono trial distinti: cache, GC e query concorrenti impediscono di sommare i tempi o
ricavarne il solo tempo SQL. Il calcolo dei gusti aggiunge circa 0,18–0,22 ms. Il caricamento delle
attività è il costo dominante osservato; include query, ordinamento, cursor, conversione delle date
e creazione delle righe, senza attribuzione separata a ciascuna componente.

Nei passaggi di memoria, le allocazioni cumulative mediane per lettura attività sono
1,47/35,70/142,73 MiB; per il primo risultato repository 2,22/37,52/146,89 MiB e per il solo
calcolo watched 0,14/1,80/6,98 MiB. I massimi Java campionati durante tre letture repository
consecutive sono 11,78/15,52/23,33 MiB, da occupazioni iniziali 5,45/5,47/5,44 MiB.
Il campionamento richiesto ogni 2 ms fornisce un limite inferiore del picco, include la sonda e
non misura PSS o picchi nativi. Nei tre reload repository i contatori ART rilevano rispettivamente
1/19/55 GC e 10/288/997 ms di tempo GC cumulativo, con zero blocking GC osservati: non sono
misure delle pause del Main o dei frame. I circa 157 MiB della precedente prova dell'intero reload
UI restano coerenti con costi temporanei distinti dalla ritenzione già corretta.

Le prove UI separano tre riattivazioni dopo almeno 12 secondi in background e sei selezioni
ciascuna di scope e anno per carico. Il cambio anno richiede mediane di 4,77/23,70/70,15 ms
dal metodo del ViewModel alla pubblicazione; sul carico massimo i due reload senza campionamento
richiedono 2,28 e 2,25 secondi. Nel reload UI con contatori, 100k attività allocano circa 155 MiB
cumulativi; l'occupazione Java osservata alla pubblicazione è 28,87 MiB e al dump successivo 13,48 MiB,
senza misura del massimo continuo UI. I massimi del repository isolato non sono massimi dell'app.
Tempi fino alla pubblicazione dello stato, variabilità,
contatori del reload UI e limiti sono riportati con gli artefatti temporanei in
`.audit/statistics-timing/`. Il calcolo e il mapping osservati restano fuori dal Main; questa misura
non identifica il primo pixel visualizzato e non certifica fluidità. Non è stato aggiunto codice
di produzione: nessun budget temporale è definito dalla checklist, nessun errore funzionale è emerso
e la cache condizionata di rating/generi non è giustificata. È completata questa baseline circoscritta,
senza confronto temporale con l'algoritmo precedente ai dodici passaggi; restano aperti gli altri
scenari prestazionali, gli audit e il release gate v1.2.5.

Il successivo intervento sul caricamento, sempre il 17 ottobre, separa query/cursor, ordinamento,
conversione delle date e costruzione delle righe. Con 100k attività, cinque trial release sullo stesso
HONOR misurano circa 901 ms per query ordinata e lettura grezza, 271 ms senza `ORDER BY`, 936 ms
per il solo `Instant.parse` su input residente e 6 ms per costruire le righe con Instant già disponibili.
Sono esperimenti separati, non componenti sommabili esattamente del timer Room. Il parsing alloca
circa 135,6 MiB cumulativi per 100k timestamp: il primo intervento è limitato a questo costo.

`RoomConverters.stringToInstant` ora legge direttamente i campi numerici dei formati UTC comuni
scritti da `Instant.toString()`, con validazione data/ora tramite `java.time` e precisione al nanosecondo.
Formati alternativi, anni estesi, secondi intercalari, mezzanotte 24:00 e input non validi mantengono
il percorso `Instant.parse`. Non cambiano scritture, schema, ordine della query, dati o backup;
nessuna cache dipendente dalla ripetizione dei timestamp. I test proteggono 2.000 round-trip
deterministici, tutte le precisioni, formati legacy ed errori; una regressione Room verifica
nanosecondi e aggregazione ai bordi del mese.

Il confronto release prima/dopo usa le stesse fixture, ART `speed`, cache calda, cinque trial per
fase e pass memoria separati. Stato termico 0, temperatura batteria circa 29,2–29,5 °C.

| Attività | Primo risultato repository, mediana prima → dopo | Allocazioni cumulative mediane per caricamento prima → dopo |
|---|---:|---:|
| 1.000 | 22,94 → 7,82 ms | 2,23 → 0,39 MiB |
| 25.000 | 500,23 → 125,20 ms | 37,52 → 4,86 MiB |
| 100.000 | 2.092,69 → 1.070,87 ms | 146,88 → 18,45 MiB |

Il criterio sperimentale fissato prima del fix — almeno 30% di riduzione del caricamento 100k —
fallisce sulla baseline e passa dopo: riduzione misurata circa 49%. Il solo parsing scende da
circa 936 a 29 ms; la lettura grezza ordinata resta circa 0,9 s. Il caricamento grande rimane quindi
di circa un secondo: non viene dichiarato istantaneo né ottimizzato integralmente. Nel controllo
UI, i reload del producer 100k passano da circa 2,6 a 1,3 s fino alla pubblicazione dello stato;
non sono tempi del primo pixel. Nei tre trial repository 100k, i GC rilevati passano da 52 a 10,
con tempo ART cumulativo 1.000 → 581 ms; i massimi Java campionati passano da 26,33 a 19,04 MiB.
Questi massimi restano limiti inferiori del picco, non misure del massimo dell'intera app o delle pause UI.

Il punto di approfondimento e riduzione del caricamento è coperto da questo intervento circoscritto;
sonde, APK separati, RED/GREEN prestazionale, campioni e confronto restano ignorati in
`.audit/statistics-load/`. La precedente correzione di ritenzione è preservata. Restano distinti
frame/primo pixel, picco continuo dell'intera UI, cache disco fredda e gli altri scenari della milestone.
Passano tutti i 490 test JVM e 265 test Android sullo stesso HONOR, con zero fallimenti/errori/skipped,
Spotless, lint senza errori e 51 warning preesistenti, build debug/AndroidTest/release standard.
L'APK normale è privo di sonde; i pacchetti temporanei sono rimossi e le installazioni originali conservate.

La successiva verifica UI del 17 ottobre usa 33 tracce sullo stesso HONOR a 60 Hz, release R8/ART
`speed`, le tre fixture precedenti e cache disco calda. Ogni carico ha tre ingressi da nuovo processo,
tre riaperture, sei cambi tipo media, sei cambi anno e tre ritorni dal background. I timestamp di
disegno sono collegati ai frame app e alla presentazione SurfaceFlinger; le durate della finestra
principale sono rilevate separatamente con FrameMetrics. Non si effettuano screenshot o campionamenti
heap nelle finestre misurate. Stato termico 0, batteria 25,1–26,0 °C.

Con 100k attività i risultati compaiono dopo una mediana di 1.998 ms dal tap (intervallo 1.984–2.033 ms,
tre correlazioni dirette al display). Nelle tre prove il producer riparte verso il termine della
transizione: la causa precisa va indagata, senza attribuire l'intera attesa al converter. Con dati già
in memoria il primo frame arriva entro circa 85–89 ms, con dissolvenza conclusa verso 771 ms;
i cambi tipo media/anno arrivano al display in mediane di 109/120 ms. Alcuni tempi del primo frame
negli altri scenari sono limiti superiori sul frame successivo dello stesso stato, non misure ottiche.

La fluidità resta da migliorare: i massimi FrameMetrics sono 115 ms nell'ingresso, 69 ms nel cambio
tipo media e 77 ms nel cambio anno; nessun frame principale misurato supera 700 ms. Le superfici
del menu anno registrano 10 frame scartati su 294 frame/18 aperture, distinti dagli zero scartati
segnalati nella finestra principale. Non si confonde l'attesa di due secondi con un singolo frame
bloccato. La traccia ha un limite esplicito: SurfaceFlinger emette 30 eventi di fine frame con timestamp
non valido e 39 durate FrameTimeline risultano incomplete. I metadati di jank noti sono mantenuti,
le durate incomplete escluse dai percentili e FrameMetrics non perde callback; non si certifica
completezza assoluta di FrameTimeline. I dettagli restano ignorati in `.audit/statistics-frames/`.

Questo passaggio misura il comportamento corrente e non applica correzioni né dimostra un beneficio
prima/dopo dei frame. Il codice e l'APK normale restano identici al punto precedente; passano nuovamente
`spotlessCheck` e `assembleRelease`. I 490 test JVM/265 Android citati sopra appartengono alla precedente
verifica del codice invariato, non a una nuova esecuzione. Restano aperti caricamenti aggiuntivi, scatti
misurati, picco continuo dell'intera UI e gli altri scenari/gate della milestone.

La successiva correzione del 17 ottobre risolve la ripartenza: il ritorno di un consumer prima di
cinque secondi riemetteva `true` dopo la tolleranza e `collectLatest` riavviava i producer ancora attivi.
La deduplicazione dello stato effettivo dopo il timer conserva il lavoro in corso. Il producer delle
statistiche riusa un solo aggregato completato finché storico, giorno, anno e fuso non cambiano;
il cambio Film/Serie aggiorna i generi senza rileggere lo storico mensile. La cache non trattiene
un secondo elenco di attività e si svuota alla fine del producer. Il filtro anno diventa una riga
di chip Material 3 a selezione esclusiva, scorrevole e senza finestra popup; restano tutti gli anni.

Il confronto ripete i 33 scenari con le stesse fixture e definizioni dei timestamp, release R8/ART
`speed`, display 60 Hz, stato termico 0 e batteria 26,5–26,8 °C (baseline 25,1–26,0 °C). Con 100k
attività il primo contenuto passa da 1.998 a 1.013 ms di mediana, intervallo dopo 890–1.074 ms,
con tre correlazioni dirette al display; il producer parte una volta per ingresso, senza riavvii
nelle nove riaperture brevi. Il cambio tipo media passa da 109 a 46 ms di mediana, massimo da
125 a 63 ms; le 18 selezioni non eseguono alcun calcolo dello storico. Il cambio anno a 100k resta
circa 120 ms perché richiede ancora il calcolo mensile. Il primo frame alla riapertura resta circa
90 ms e la dissolvenza termina circa 791 ms (prima 86/771 ms): non si attribuisce un beneficio alla
transizione stessa. Dopo oltre cinque secondi in background il producer riparte normalmente da Room.

FrameMetrics del cambio anno: P95 35,72 → 18,99 ms, massimo 77,30 → 34,37 ms, deadline superate
98/912 → 20/951 frame; la finestra fissa di 250 ms dopo ogni selezione conferma P95 30,32 → 15,93 ms.
Le superfici popup e i loro 10 scarti precedenti non esistono più. Questo non significa assenza di
jank: nel cambio tipo media P95 passa solo da 24,53 a 23,75 ms, il massimo resta circa 69 ms e le
deadline superate sono 32/945 prima e 36/946 dopo. Alcuni frame aspettano il renderer; la traccia non
giustifica altre modifiche al calcolo. In ingresso P95 resta circa 26 ms, massimo 115 → 83 ms;
al ritorno dal background i nove primi frame superano ancora la deadline, massimo 67 ms. Nessuno
dei 2.742 frame principali misurati supera 100 ms. FrameTimeline segnala zero scarti principali ma
resta parziale: 29 timestamp vendor non validi, 47 durate incomplete, zero callback FrameMetrics perse;
38 correlazioni di contenuto sono dirette e 25 limiti superiori sul frame successivo.

Passano 493 test JVM, 20 test nativi Statistics/navigazione e il nuovo selettore anche in italiano
(un'ulteriore esecuzione), inclusi layout 320dp/font 1,5 e anni fuori dall'area visibile. Regressioni
RED/GREEN coprono continuità del caricamento, riuso e invalidazione giorno/fuso. Passano Spotless,
lint (0 errori, 51 warning preesistenti), build debug/AndroidTest/release e review Ponytail mirata.
L'APK standard è privo di sonde; inventario dei pacchetti originali invariato e pacchetti temporanei
assenti. Confronto e limiti completi restano ignorati in `.audit/statistics-navigation-fix/`.
Questo chiude le correzioni mirate, non certifica fluidità assoluta né completa i gate v1.2.5.

Il successivo approfondimento isola gli scatti Film/Serie: RenderThread compila shader `CircleOp`
del ripple per 41–49 ms e il Main aspetta in `postAndWait`. Un confronto sullo stesso APK, con soli
dati privati del pacchetto sintetico azzerati e ordine ripple attivo/disattivo/disattivo/attivo,
riproduce massimi di 66 ms con il ripple e 24–26 ms senza; gli shader lenti scompaiono nei controlli.
La correzione conserva i `SegmentedButton` e sostituisce soltanto il loro ripple con una tinta
animata nelle opacità Material 3 (pressione/focus 0,1, hover 0,08), usando colori del tema e
`animateColorAsState`. Nessuna disattivazione globale, preriscaldamento shader o modifica del dispositivo.

Il confronto finale del 17 ottobre misura quattro prove prima e quattro dopo, sei cambi per prova,
con 1k/25k/100k attività e una seconda ripetizione a 1k. Ogni prova riparte dai soli dati sintetici
privati azzerati, anziché ereditare gli shader già caldi dagli altri carichi: la baseline è distinta
dalle precedenti 33 tracce. HONOR CMA-LX1/Android 13, release R8/ART `speed`, 60 Hz, stato termico 0;
batteria 26,5–27,0 °C prima e 26,9–27,1 °C dopo. Le medesime finestre di 250 ms dopo il callback
mostrano P95 53,32 → 18,88 ms, massimo 86,70 → 26,41 ms e frame oltre 50 ms 22 → 0.
Le deadline FrameMetrics superate passano da 60/347 a 21/343 nella finestra fissa; i primi tre cambi
per prova passano da P95 62,02 a 19,83 ms, quelli successivi da 22,16 a 17,96 ms.
Le latenze callback→display sui 24 cambi passano da mediana 70,98 a 44,40 ms, P95 99,54 → 57,75 ms.
Gli shader `CircleOp` non sono più compilati durante i cambi; resta una compilazione `TextureOp`
di circa 15 ms al primo uso, distinta dai blocchi eliminati. La tinta termina prima del vecchio
ripple: i totali della prova hanno 1.244 → 343 frame, perciò il confronto usa anche finestre uguali.

Il risultato non garantisce ogni frame entro circa 16,7 ms: rimangono 21 deadline superate nella
finestra dopo e zero scarti FrameTimeline sia prima sia dopo. FrameTimeline resta parziale:
17/12 durate incomplete nelle prove accoppiate prima/dopo, zero callback FrameMetrics perse;
i timestamp vendor non validi sono 20 nelle sei tracce di diagnosi e 9 nelle quattro finali.
Le 24 correlazioni contenuto→display dopo sono dirette; due delle 24 prima sono limiti superiori.
Un confronto a riposo trova zero pixel cambiati nei 1.022.400 pixel dell'area contenuto confrontata,
escluse barre di stato/navigazione. Il nuovo feedback è verificato anche con pressione annullata,
focus e Invio, nei due temi; non si rivendica una prova con mouse fisico o TalkBack.

Passano 493 test JVM, tutti i 22 test Statistics/navigazione selezionati e tre casi aggiuntivi in
italiano; Spotless, lint (0 errori, 51 warning preesistenti), debug/AndroidTest/release standard e
review Ponytail mirata passano. Restano invariati cache/storico, schema, backup, versioni e dipendenze.
L'APK normale è privo di sonde; pacchetti temporanei assenti, inventario protetto invariato.
Le evidenze restano ignorate in `.audit/statistics-scope-jank/` e `.audit/statistics-scope-final/`.
Gli altri scenari prestazionali e il gate complessivo v1.2.5 restano aperti.

Il seguito del 18 ottobre distingue la soglia fissa di 16,666667 ms dalla deadline FrameMetrics,
che può concedere circa 33 ms: nella precedente prova dopo erano rispettivamente 31 e 21 frame
su 343. Quattro controlli sullo stesso APK, con marcatore al radice attivo/disattivo/disattivo/attivo,
non indicano che la sonda spieghi gli scatti. I confronti successivi lo disattivano e non rivendicano
una nuova misura esatta callback→nuovo contenuto.

Il confronto accettato usa lo stesso APK R8/ART `speed`, dati sintetici privati azzerati per prova,
ordine prima/dopo controbilanciato e finestre di 250 ms per 24 selezioni per versione. Le fixture
contengono 1k/25k/100k attività episodiche e 20 film visti: il radar ha sei assi in Tutti e tre in
Film/Serie; la seconda prova a 1k controlla la ripetibilità. HONOR CMA-LX1/Android 13, 60 Hz,
stato termico 0, batteria 25,9–26,3 °C. Questa baseline con radar popolato è distinta dalle precedenti.

La spunta Material appare subito, evitando la transizione predefinita che compila `TextureOp`
per 12–17 ms al primo cambio. Il Canvas del radar usa `graphicsLayer()` con alpha 1 e strategia
predefinita: le sue registrazioni di disegno passano da 288 a 24, mantenendo l'aggiornamento dei dati.
Restano native forma, fondo, bordo, selezione, semantiche e movimento delle etichette; la tinta
e i feedback precedenti restano animati. Nessuna modifica a storico, cache, dati, schema o dipendenze.

I frame oltre 16,666667 ms passano da 20/341 (5,87%) a 16/319 (5,02%); P95 17,49 → 16,56 ms,
massimo 27,26 → 25,80 ms e deadline FrameMetrics superate 14 → 10. La riduzione è modesta e varia
tra prove: i primi tre cambi passano da 12 a 7 frame oltre soglia, gli ultimi tre da 8 a 9.
Non si dichiara quindi un miglioramento uniforme dei cambi già caldi né fluidità assoluta.
Zero ricalcoli dello storico, zero callback FrameMetrics perse e quattro → zero compilazioni shader.
FrameTimeline segnala 15 → 8 deadline app superate e zero scarti; resta parziale, con 8/13 durate
incomplete e 4/9 timestamp vendor non validi prima/dopo. Il confronto a riposo trova zero pixel
cambiati su 1.022.400 nell'area verificata, escluse barre di stato e navigazione.

I controlli di sola isolazione del selettore non mostrano un guadagno robusto aggiuntivo; la variante
con lettura del colore in disegno viene esclusa dalla review perché il modifier esterno includerebbe
il target interattivo di 48dp e altererebbe il fondo visivo di 40dp. Queste varianti non entrano nel
prodotto. Le evidenze e i controlli restano ignorati in `.audit/statistics-frame-budget/`.

Passano 493 test JVM, 24 test nativi Statistics/navigazione in inglese e cinque casi aggiuntivi
in italiano, inclusi selezione rapida ed effettivo aggiornamento/ripristino dei pixel del radar nei
due temi. Spotless, lint (zero errori, 51 warning preesistenti), build debug/AndroidTest/release
standard e review Ponytail mirata passano. Il runner AGP incontra un bug della propria opzione
`--serial` prima dei test; le esecuzioni finali usano il filtro `ANDROID_SERIAL` per il solo HONOR,
senza modifiche al plugin né operazioni sull'emulatore. APK normali privi di sonde/fixture,
pacchetti temporanei assenti e inventario protetto invariato. Il gate v1.2.5 resta aperto.

Il successivo controllo dei cambi già caldi usa otto selezioni di riscaldamento fuori misura e
dodici alternanze Film/Serie per prova, con finestre identiche di 250 ms. Sullo stesso HONOR,
in release R8/ART `speed` a 60 Hz, nessuna delle tre varianti giustifica un ulteriore cambiamento:

- Confine di ricomposizione per pulsante, quattro prove controbilanciate: il codice di Tutti passa
  da 145 a zero esecuzioni per dodici cambi, ma i frame oltre soglia sono 15/315 → 18/320 e il
  P95 16,04 → 17,11 ms. Ridurre quel lavoro non dimostra un beneficio sui frame.
- Geometria del radar con `drawWithCache`, una sola coppia esplorativa: 8/159 → 5/158 frame oltre
  soglia, P95 16,37 → 15,34 ms. La geometria viene comunque ricostruita dodici volte in entrambi
  i casi; la coppia non dimostra un guadagno ripetibile e non viene adottata.
- Layer raster del solo radar, quattro prove controbilanciate: le operazioni grafiche sui percorsi
  scendono da circa 630 a 48 per prova, ma i frame oltre soglia sono 20/317 → 21/322 e il
  P95 17,09 → 17,22 ms. La memoria GPU riportata da `gfxinfo` cresce da 2.599.556 a 4.074.116 byte
  (circa 1,4 MiB); questa variante non viene adottata.

Le prove di attribuzione con 1k e 100k attività escludono ricomposizioni dei riepiloghi di visione
durante le dodici alternanze: il contenuto dei pulsanti viene eseguito 148/146 volte, mentre il
radar viene registrato dodici volte. Zero ricalcoli dello storico e zero callback FrameMetrics perse
in tutte le 144 selezioni misurate, incluse le prove esplorative e di attribuzione. I tempi delle
sonde aggiuntive non sono aggregati ai confronti A/B. Restano picchi nel percorso Compose/layout
e nella pipeline grafica; questi dati non dimostrano un limite hardware né una nuova riduzione
affidabile degli sforamenti. Per scelta dell'utente resta l'animazione attuale, con il codice di
produzione precedente invariato. Evidenze, controlli negativi e prototipi esclusi restano sotto
`.audit/statistics-warm-filters/`; il miglioramento dei cambi caldi resta aperto.

**Completato quando:** risultati equivalenti, meno passaggi/allocazioni e cancellazione verificata.
La riduzione da dodici passaggi a uno non implica un'accelerazione di dodici volte dell'intera funzione.

## 8. Backup: misurare il picco heap prima di cambiare la pipeline

Riferimenti: [BackupJsonCodec](../app/src/main/java/com/cydoniancitizen/bingee/data/importexport/BackupJsonCodec.kt),
[BackupDataStore](../app/src/main/java/com/cydoniancitizen/bingee/data/importexport/BackupDataStore.kt),
[BackupFileGateway](../app/src/main/java/com/cydoniancitizen/bingee/data/importexport/BackupFileGateway.kt).

- [ ] Misurare heap, allocazioni, GC, durata e transazioni con JSON sintetici da 1/10/40–50 MiB
      e una cache ampia non appartenente alla libreria.
- [x] Registrare quali rappresentazioni complete sono contemporaneamente vive e se il picco è problematico.
- [x] Se confermato, scegliere il più piccolo intervento: lettura a record con limiti/UTF-8 rigoroso,
      export su file temporaneo prima della pubblicazione oppure conversioni fuori dalla transazione
      dopo aver acquisito uno snapshot coerente. Non imporre una riscrittura completa.
- [ ] Se la misura non giustifica una modifica, registrare la decisione e l'evidenza; distinguere
      una scelta verificata dalla mancanza di hardware o misure.
- [ ] Per ogni modifica verificare backup v1/v2/v3, input malformato, UTF-8 errato, limiti,
      cancellazione, errore di scrittura e rollback.

L'11 ottobre sono stati confrontati parsing/validazione di JSON sintetici da circa 1/10/45 MiB e
snapshot export con 6.000 titoli di cache non posseduti (46,8 MB di testo), su Pixel 9 emulato/API 35,
debug e heap Java massimo 192 MiB. Il parser precedente va in OOM a 45 MiB; con validazione UTF-8
a buffer limitato e parsing da reader lo stesso file passa. Lo snapshot selettivo evita di caricare
la cache esclusa dall'export: il picco Java campionato passa da circa 56 a 9 MiB nella fixture.
Sono campioni singoli di diagnosi, non benchmark fisici né misure del restore completo; la matrice
di allocazioni native, transazioni, ripetizioni e hardware resta da completare.

**Completato quando:** verifica e decisione sono documentate; l'eventuale intervento riduce il problema
misurato preservando export completo, validazione preventiva e restore interamente transazionale.
Non ridurre limiti, troncare dati o introdurre commit parziali come scorciatoia.

## Opportunità condizionate, non requisiti automatici

- [x] Verificare il numero di coroutine in attesa nel refresh manuale di una libreria molto grande;
      introdurre lotti solo se necessario, conservando tutti i titoli, concorrenza remota e risultati parziali.
- [ ] Valutare un Baseline Profile dell'app solo dopo una baseline di startup/percorsi utente;
      non aggiungere moduli benchmark o infrastruttura CI come effetto collaterale del cleanup.
- [ ] Lasciare invariati R8 già abilitato, Strong Skipping predefinito, loader Coil condiviso e WorkManager
      con lotti/retry limitati, salvo un problema concreto distinto.

## Verifica e chiusura v1.2.5

- [x] Registrare scenari ed evidenze disponibili; distinguere implementato, verificato senza modifica,
      limiti accettati e matrici rinviate nella tabella di chiusura.
- [x] Eseguire regressioni proporzionate: widget, Profile/Statistics e shortcut, query Room,
      identità/progressi, statistiche, import/export e worker secondo i file cambiati.
- [x] Eseguire formattazione/lint, test JVM, test Room/strumentali pertinenti e build debug previsti dal repository.
- [x] Verificare in release ottimizzata i percorsi concordati, inclusi callback Glance e detail intent.
- [x] Registrare benefici misurati e relativi compromessi su hardware, senza sommare risparmi sovrapposti
      né estendere i risultati alle matrici non eseguite.
- [x] Conservare le revisioni Ponytail già eseguite; registrare il rinvio esplicito del nuovo audit
      globale a v1.2.6, senza dichiararlo eseguito.
- [x] Completare cleanup e sincronizzazione documentale concordati per v1.2.5;
      aggiornare policy/ADR solo quando cambia una decisione durevole.
- [x] Conservare Room 9 e backup 3 se non cambia lo schema/formato. Se serve una modifica,
      applicare incremento versione, migrazione non distruttiva/schema e test di compatibilità richiesti.
- [x] Controllare link, diff, file generati e assenza di segreti; report/trace restano esclusi da Git.
- [x] Chiudere il gate concordato v1.2.5; prossima manutenzione v1.2.6 prima di v1.3.0.
      Commit, firma e pubblicazione restano azioni esplicite separate.

### Spazio locale e verifiche finali

Il JSON compatto passa da 16.636.440 a 9.745.199 byte sulla stessa libreria sintetica: 6.891.241 byte
in meno (41,4%), con contenuto JSON equivalente. Il riavvio elimina 8.390.045 byte di ZIP privati
abbandonati, conserva l'originale e protegge l'import attivo. Restano schema v3, limite di 50 MiB e
restore v1/v2/v3. È risparmio su disco, non una misura di CPU/GPU/RAM o batteria.

Le verifiche finali in release R8 sullo stesso HONOR / Android 13 coprono navigazione offline EN/IT,
Light/Dark e quattro configurazioni tra 320 e 600dp, font scale 1.0–1.5. Il backup sintetico da
47.518.199 byte (45,32 MiB) ripristina 1.000 titoli, 2.700 stagioni e 27.000 episodi/progressi;
il re-export è identico semanticamente e in byte. Preview senza scritture e rollback su errore
iniettato conservano tutte le tabelle. Tempi singoli: restore 10,225 s, export 14,858 s, rollback
17,294 s. Nessun OOM osservato; non è una misura continua del picco heap né una prova completa SAF.

Il solo rilievo finale, la dicitura backup ancora v1 in EN/IT, è corretto: export v3 e restore
v1/v2/v3. Sul codice finale passano **276/276 test Android senza filtri**, zero errori, fallimenti o
test saltati, più un test separato della schermata backup italiana. Passano Spotless, lint senza
errori (38 warning), build debug/AndroidTest e release ottimizzata. I 496 risultati JVM validi sono
riusati da Gradle come UP-TO-DATE; non sono una nuova esecuzione. Dispositivo e dieci widget
preesistenti preservati. Le prove sono su HONOR Android 13, non su tutti i dispositivi supportati.

Nessun ulteriore problema prodotto emerge da queste verifiche. Evidenze temporanee ignorate in
`.audit/storage-fixes/`, `.audit/final-verification/` e `.audit/closure-android/`.
