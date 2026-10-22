# La tua storia — Piano implementativo in milestone

**Stato finale, 22 ottobre 2026:** History corrente e scope v1.3.0 verificati e chiusi
dopo accettazione/TalkBack del proprietario, integrazione, suite Android/R8, audit globale
e allineamento documentale. Il [record canonico](../../../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md#v130-closure-record--2026-10-22)
conserva limiti e rinvii. I checkpoint precedenti sotto restano storici.
Nessun commit, push, firma o pubblicazione nel passaggio finale.

## Correzione approvata il 21 ottobre 2026 — timeline orizzontale continua

Il proprietario ha approvato la sostituzione della navigazione mensile descritta sotto: una linea
orizzontale continua parte dalla prima attività datata conservata, Film o Episodio, e procede verso
destra fino alla più recente. Ogni punto datato è collegato a poster e titolo; gli episodi restano
raggruppati per serie/giorno ed espandibili. La spaziatura è regolare e non rappresenta una scala
temporale; nello stesso giorno l'ordine è stabile, senza inventare l'ordine reale delle visioni.
Restano i filtri Film/Serie (chiave interna `EPISODES`); la successiva richiesta del 21 ottobre
nasconde Tutti e apre su Film. Un ulteriore incremento approvato compatta poster/titolo, avvicina
le date ai punti e corregge testi di chiusura e vuoti per filtro. I gruppi restano espandibili
nella scheda, senza un pannello dedicato. Con font oltre 1.3 si omette il poster decorativo.
I blocchi si alternano sopra/sotto un asse orizzontale centrale, con contenuto scorrevole nel proprio
lato. Una vecchia selezione Tutti riparte da Film senza cursore, gruppi o ancora della vista mista;
le selezioni singole conservano il ripristino esistente. Query, date e paginazione restano invariate.
La verifica di questo incremento è riportata nelle
[note v1.3.0](../../release-notes-1.3.0.md#two-filter-alternating-layout--2026-10-21):
533 JVM, 14 Room e 28 esecuzioni UI/adattive passano, insieme a Spotless, lint e build debug/test.
Il 22 ottobre 2026 il proprietario conferma anche il TalkBack manuale sulla versione compatta
alternata e approva l'interazione corrente, nello scope documentato sotto.
Scompaiono selettori mese/anno e pagine principali esplicite:
il caricamento ai bordi è automatico e limitato, con posizione conservata al ritorno dai Dettagli.

Questa approvazione sostituisce i default precedenti di apertura sul mese corrente, ordine
decrescente e pagine principali sostitutive visibili. Le prove H1–H6 sotto documentano la precedente
UI mensile; non certificano la nuova interazione. La correzione è implementata e verificata con
prove automatiche e visuali sotto descritte; accettazione del proprietario e nuova prova manuale
TalkBack sono confermate il 22 ottobre 2026.
L'esecuzione è autorizzata con Astra Medium orchestratore e GPT-6.1 Sol High implementatore,
senza commit o push e senza ulteriore gate progettuale. Il lavoro dirty Dettagli/Home preesistente
va conservato. La milestone v1.3.0 resta aperta; il gate manuale Dettagli M5 rinviato è verificato
dal proprietario il 22 ottobre nello scope riportato nel relativo piano.

### Passi e confini dell'incremento

I percorsi sorgenti seguenti sono relativi a `app/src/main/java/com/cydoniancitizen/bingee/`.

1. **Lettura crescente limitata:** `core/model/ViewingHistory.kt`,
   `data/library/HistoryMappers.kt`, `data/library/local/HistoryDao.kt` e
   `data/library/DefaultLibraryRepository.kt`. Conservare `observeHistory(HistorySnapshotQuery)`
   e la coerenza transazionale; adattare cursori e snapshot alla finestra crescente. Le finestre
   mensili possono restare interne per riusare i confini DST, ma i periodi vuoti devono essere
   saltati cercando la prossima attività, senza un ciclo per ogni mese vuoto. Prima regressioni
   mapper/DAO su ordine, parità, confini anno/fuso e lunghi vuoti, poi implementazione minima.
2. **Finestra e ancora:** `feature/history/HistoryViewModel.kt` e `HistoryUiState.kt`.
   Caricare ai bordi, limitare il numero di blocchi trattenuti e conservare chiave/offset visibili
   durante caricamento ed espulsione. Provare avanti/indietro, ritorno dai Dettagli, cambi dati,
   cancellazione, errore/retry, fuso e restore; non combinare generazioni incompatibili.
3. **Asse e contenuto:** `feature/history/HistoryScreen.kt`, risorse EN/IT e fixture debug/test.
   Riutilizzare Material 3, poster e navigazione; rendere la sequenza con `LazyRow`, punti connessi
   e schede. Conservare la lettura limitata dei figli, gli stati offline/loading/empty/error,
   target accessibili e testo adattivo. Le prove UI devono attraversare il caricamento automatico
   e verificare che il punto visibile non salti; non basta cambiare l'orientamento del contenitore.
4. **Verifica e documentazione:** test mirati JVM/Room/Compose e integrazione restore, controlli
   disponibili di formattazione/lint, suite JVM e build debug/AndroidTest; review Ponytail del diff
   aggiunto e prove native isolate. Aggiornare la documentazione canonica con risultati effettivi,
   distinguendo verifiche automatiche, osservazioni visuali e TalkBack manuale.

- [x] Regressione JVM del mese osservata fallire; latenza e clipping dimostrati prima dei fix.
- [x] Lettura crescente e attraversamento sparso senza salti o duplicati.
- [x] Finestra limitata, ancora stabile e riconciliazione delle generazioni.
- [x] UI orizzontale, gruppi, adattamento e controlli automatici verificati nello scope dichiarato.
- [x] Gate, review e documentazione aggiornati con evidenze nuove.
- [x] Accettazione del proprietario della nuova interazione e prova manuale TalkBack specifica.

**Prova manuale del proprietario, 22 ottobre 2026:** su HONOR CMA-LX1 / Android 13,
360 dp/font 1.0 e UI inglese, con TalkBack collegato ed esplorazione tattile attiva, il proprietario
conferma «Tutto corretto» per i soli filtri Movies/Series, lettura cronologica di date/titoli,
raggiungibilità delle schede sopra/sotto e scorrimento orizzontale con due dita. Conferma poi
«Tutto corretto» per espansione/esplorazione/chiusura dei gruppi Serie, stato espanso/compresso
e ritorno dai Dettagli alla posizione precedente. Alla domanda sull'interazione compatta alternata
risponde «Sì, approvata». L'app sintetica esistente è aggiornata dal codice corrente, senza INTERNET;
nessuna modifica di produzione per questa prova. Le cinque impostazioni iniziali di accessibilità
sono verificate ripristinate e l'inventario pacchetti è invariato. Evidenze in
`.audit/accessibility-2026-10-22/`, escluse da Git. Questo completa il gate manuale della History
corrente, senza certificare ogni dispositivo o configurazione TalkBack né chiudere v1.3.0.

Date, speciali, attività fuori raccolta/abbandonate e semantica dei progressi restano quelle di H1.
Room 9, Backup v3/restore v1/v2/v3, dipendenze e cache non cambiano. La finestra limita il payload,
non promette costo SQL costante. Evidenze temporanee in `.audit/history-horizontal/`, escluse da
Git. Gradle e ADB hanno un solo owner; installazione nativa esclusivamente nel package isolato
`com.cydoniancitizen.bingee.historyhorizontal130`, senza interferire con le altre installazioni.

### Verifica della lettura continua — 21 ottobre 2026

La prima candidata conservava query mensili interne e univa finestre limitate. Pur superando i test
di correttezza, sullo stesso HONOR CMA-LX1/Android 13 ha richiesto 4018,414 / 3850,473 / 3779,689 ms
con 10.000 episodi e 33888,373 ms nel primo campione con 100.000 episodi, distribuiti su 190 mesi
occupati di una serie. È stata scartata: il payload limitato non giustifica questa latenza introdotta.
La prova lenta è stata interrotta intenzionalmente; non è un crash del prodotto né un test superato.

La correzione usa una query aggregata all-history e un lookup SQL binario degli offset UTC prodotti
da Java ZoneRules. Il confronto delle transizioni conserva gli istanti; il troncamento delle frazioni
avviene solo prima dell'aritmetica di calendario SQLite per evitare arrotondamenti di nanosecondi.
Offset storici in secondi, date manuali e gruppi restano esatti. Il percorso mensile, con confini
giornalieri calcolati in Java, rimane un fallback esplicito fuori dal guard calendario 1–9998 o oltre 399 transizioni, senza
scartare dati né superare il budget conservativo di parametri SQLite.

Su stesso dispositivo, dataset, build debug e Room in memoria, la transazione completa dopo la
correzione misura:

| Episodi | Tre campioni dopo la correzione (ms) | Mediana (ms) | Payload per campione |
| --- | --- | --- | --- |
| 10.000 | 181,983 / 143,836 / 151,731 | 151,731 | 90 parent, 20 figli, pagina successiva disponibile |
| 100.000 | 879,872 / 840,360 / 856,740 | 856,740 | 90 parent, 20 figli, pagina successiva disponibile |

Il confronto iniziale 100.000 dispone di un solo campione; non gli si attribuisce una mediana
comparabile. Queste sono misure di transazione di lettura, non frame/UI, CPU, heap/PSS, batteria o
disco. Il costo resta proporzionale ai dati; non si estendono le misure al fallback né ad altri
dispositivi. La correzione elimina la regressione misurata; non apre ulteriori ottimizzazioni.

Il primo controllo del percorso veloce è passato su Android con 13 test DAO, inclusi nuovi casi diretti tramite
repository/query all-history: giorni DST di 23/25 ore e confronto con policy Java, nanosecondi prima
di mezzanotte, offset storico in secondi di Kathmandu, salto di data di Apia all'istante della
transizione e fallback fuori dal guard calendario. Le prove di date manuali/futuro, filtri, gruppi,
cursori avanti/indietro e snapshot coerente restano nella stessa suite. Il controllo permanente del
numero di query è stato poi aggiunto alla suite: il controllo finale DAO passa 14 test e include
120 mesi occupati con un budget di 1–3 letture dei progressi, senza soglie temporali fragili.
I gate conclusivi sono registrati separatamente.

### Checkpoint finale della correzione

- Host finale dopo gli ultimi cambiamenti: 530 test JVM eseguiti, zero failure/error/skip;
  Spotless e build debug/AndroidTest superati. Lint superato con 0 errori e 54 warning.
  `ConfigurationScreenWidthHeight` in History è un warning noto introdotto dal dimensionamento
  delle schede sullo schermo, non un warning preesistente. Le quattro risorse dei controlli rimossi
  sono state eliminate in EN/IT; non si attribuiscono tutti gli altri warning alla baseline.
- Sul HONOR CMA-LX1 Android 13, suite finale EN: 25 test (14 DAO, 7 UI, navigazione, due restore
  History e un confine temporale repository). Dopo il solo fix della forma del pulsante episodio
  e pulizia risorse, altri 8 test UI/navigazione IT passano. Nessuna suite Android globale nuova
  o build release/R8 è implicata da questa correzione.
- Quattro configurazioni adattive controllate sul telefono: 320 dp/IT/scuro/font 1.5,
  360 dp/IT/chiaro/font 1.3, 411 dp/EN/scuro/font 1.0 e 600 dp/EN/chiaro/font 1.5.
  Le configurazioni 320 e 600 sono state ripetute dopo il fix del clipping delle etichette episodio
  multilinea. Non sono prove su un tablet fisico né una matrice cartesiana completa.
- Seed Room sintetico e nuovo processo senza permesso INTERNET: primo punto 3 gennaio 2016,
  gruppo del 6 aprile 2018 con speciale/episodio regolare, ultimo punto 20 ottobre 2026 e ritorno
  dai Dettagli verificati. Le capture confermano estremi dell'asse e testo multilinea leggibile.
  Placeholder poster offline intenzionali; nessun dato personale usato.
- Review circoscritta Impeccable/Ponytail: corretti prefetch dopo caricamento e nuovi dati al bordo,
  estremi dell'asse, regressione SQL, clipping e risorse orfane. Nessun blocco residuo nei percorsi
  ispezionati; nessun audit globale aggiuntivo o dichiarazione di perfezione dell'intera app.

La finestra mantiene al massimo 90 parent e 20 figli per gruppo aperto trattenuto (limite teorico
1800 figli se tutti i 90 gruppi sono aperti); le misure sopra hanno un solo gruppo aperto. Il runner
isolato è stato rimosso; resta la sola app `historyhorizontal130` con dati sintetici, aperta sulla
timeline per la valutazione del proprietario. Inventario delle altre app, cinque impostazioni di
accessibilità, risoluzione/densità e font scale del telefono sono conservati. Nessun commit, push o
pubblicazione. Al checkpoint del 21 ottobre v1.3.0 e Dettagli M5 manuale restavano aperti;
il precedente TalkBack non certificava questa nuova UI. La prova manuale del 22 ottobre è
documentata sopra. Evidenze grezze e probe restano esclusi da Git sotto `.audit/history-horizontal/`.

---

> Per GPT 6.1 Sol: usare `executing-plans` per eseguire H1–H6 in ordine. Implementazione H1–H6 autorizzata dal proprietario il 20 ottobre 2026 nella v1.3.0; la chiusura resta subordinata alle verifiche. Seguire AGENTS.md; niente commit automatici o delegazione implicita.

**Obiettivo:** consultare offline film ed episodi segnati come visti, con date coerenti con le statistiche e caricamento limitato anche dopo import molto grandi.

**Architettura:** modello di lettura dedicato e query Room sui progressi esistenti, esposti dal repository Library esistente; ViewModel e schermata History autonomi. Una piccola policy temporale condivisa allinea l'istogramma mensile. Nessuna nuova tabella, dipendenza o copia persistente della cronologia.

**Stack:** Kotlin, Room 9, coroutines/Flow, Compose/Material 3 e Navigation Compose già presenti.

**Specifica:** requisiti approvati nella conversazione del 20 ottobre 2026, trascritti nella sezione seguente; [AGENTS.md](../../../AGENTS.md), [roadmap canonica](../../../BINGEE_DEVELOPMENT_ROADMAP_AND_CHECKLIST.md) e [architettura](../../architecture.md) restano i riferimenti di progetto.

**Versione:** v1.3.0 / versionCode 11, per assegnazione esplicita del proprietario il 20 ottobre 2026. H1–H6 fanno parte dello scope e dei gate v1.3.0 insieme al lavoro Dettagli già in corso. v1.3.1–v1.3.4 e i relativi versionCode restano invariati.

## Contratto di prodotto

### Cosa racconta la timeline

- È una vista dei progressi conservati, non un registro permanente di eventi.
- Una sola voce per film visto e per episodio visto. Togliere lo stato visto rimuove la voce; marcarlo nuovamente non ricostruisce le visioni precedenti.
- Includere titoli fuori raccolta, serie abbandonate ed episodi speciali con progresso conservato.
- Completamento stagione/serie non genera una visione aggiuntiva.
- I metadati visualizzati sono quelli locali correnti, non una fotografia storica del titolo.
- Non inventare date, provenienza delle registrazioni o sessioni di visione. Timestamp uguali non provano un import, una spunta massiva o un binge.
- Informativa breve EN/IT: le date riflettono le registrazioni disponibili; spunte massive e date mancanti non ricostruiscono il giorno reale di visione; rimuovere lo stato visto rimuove la voce.

### Regole temporali approvate

| Contenuto | Giorno effettivo |
| --- | --- |
| Film con progresso e data manuale | `watchedDate`, senza conversione di fuso |
| Film con progresso senza data manuale | `watchedAt.atZone(zoneId).toLocalDate()` |
| Episodio con progresso | `watchedAt.atZone(zoneId).toLocalDate()` |
| Completamento serie | Non è una voce della timeline |

La stessa policy determina mese e anno del film nell'istogramma. Modificare/rimuovere la data manuale sposta la voce e il contributo alle statistiche, senza cambiare timestamp, durata o quantità di progressi persistiti.

Gli speciali sono inclusi nella timeline e restano esclusi dall'istogramma attuale: spiegare questa differenza di perimetro. Il numero di voci della timeline non va confrontato come se fosse equivalente ai minuti del grafico.

### Scelte progettuali proposte in questo piano

Sono default espliciti del progettista, non nuove preferenze attribuite all'utente. Presentarli all'avvio dell'esecuzione; eventuali correzioni dell'utente prevalgono.

1. Usare il fuso corrente del dispositivo, come le statistiche attuali. Al cambio di fuso ricalcolare entrambi; le date manuali non si spostano. Il fuso originario della visione non è conservato e non si può ricostruire.
2. Escludere da entrambe le viste i giorni effettivi successivi a oggi, conservando i dati. Dichiararlo nell'informativa. Vale il giorno effettivo: un film con timestamp futuro ma data manuale passata segue quest'ultima. Non aggiungere un nuovo validatore distruttivo o correggere automaticamente import.
3. Aprire sul mese corrente, filtro Tutti. Se vuoto ma esistono progressi precedenti, offrire «Vai all'attività più recente». Anni selezionabili dal primo giorno valido per il filtro fino all'anno corrente; nessun giorno futuro.
4. Ordine dei giorni decrescente; all'interno del giorno, ordine stabile per tipo di riga e ID locale. Non suggerire un'ora esatta per film con sola data manuale o un ordine reale di visione tra timestamp uguali.
5. Per contenere realmente la memoria, pagine sostitutive con Precedente/Successiva: 30 righe principali per pagina; ogni gruppo espanso mostra 20 episodi per pagina interna. Nessun accumulo illimitato tramite «Carica altri». Questi numeri sono parametri iniziali, da verificare in H5, non benchmark.
6. Un gruppo è identificato da giorno effettivo e ID locale della serie; include regolari e speciali, identificati distintamente. Episodi interni ordinati per stagione, numero, ID locale. Il gruppo principale compare una sola volta anche se i suoi figli richiedono più pagine.

## Confini

- Ingresso da Il tuo bingee; nessuna nuova tab principale.
- Filtri Tutti / Film / Episodi, selezione mese/anno, intestazioni giornaliere, gruppi espandibili.
- Tocco apre Dettagli del titolo. Non estendere la route per selezionare un episodio storico e non invocare «Vai al prossimo episodio».
- Preservare stato della schermata al ritorno; applicare le regole di riconciliazione H3 se i dati cambiano.
- Consultazione senza rete; immagini già disponibili o placeholder, senza fetch metadati per comporre le righe.
- Nessun rewatch, data manuale episodio, grafico aggiuntivo, recap annuale o modifica degli altri conteggi statistici.
- Room 9, backup v3 e restore v1/v2/v3 invariati. Nessuna modifica a indici, cache, SDK/plugin o librerie.
- Nuova necessità strutturale: fermare la sola parte dipendente e proporre alternativa minima prima di superare lo scope.
- Preservare il lavoro v1.3.0 in corso, comprese modifiche non committate. Test su installazione isolata: il task connected può disinstallare l'app.
- Niente commit, push, firma, tag, PR o pubblicazione senza richiesta esplicita.

## Rischi prioritari e prove assegnate

| Rischio | Risultato richiesto | Milestone |
| --- | --- | --- |
| Data manuale e timestamp in mesi diversi | Stesso mese per timeline e istogramma | H1/H2 |
| Fuso cambia senza cambiare la data odierna | Ricalcolo di entrambi; nessun cursore del vecchio fuso | H1/H3 |
| 100.000 episodi nello stesso giorno | Payload e righe renderizzate limitati; gruppo senza duplicati | H2/H5 |
| Date/progressi cambiano tra pagine | Nessuna append di risultati incompatibili, ancora riconciliata | H3/H4 |
| Restore con nuovi ID locali | Invalidare identità transitorie, rileggere i dati senza associazioni errate | H3/H5 |

## Sequenza e gate comuni

**H1 → H2 → H3 → H4 → H5 → H6.** H1 cambia la semantica di lettura delle date delle statistiche: appartiene allo scope History esplicitamente assegnato alla v1.3.0.

Per ogni milestone: leggere stato Git e file rilevanti; aggiungere regressioni prima della logica non banale; eseguire test mirati; implementare il minimo; verificare test, lint/build pertinenti e diff. Seguire i gate A1–A7/B1–B5 applicabili della roadmap, incluso audit globale read-only con Ponytail prima di dichiarare completata una modifica numerata. Non ridurre questi gate attraverso il nuovo piano.

Report, fixture grandi, screenshot e log in `.audit/history-timeline/`, esclusi da Git. Nella documentazione canonica solo decisioni, stato e risultati sintetici. Nessuna casella si considera completata perché esiste questo documento.

## H1 — Una sola regola delle date

**Consegna:** policy temporale testata e istogramma allineato; nessuna nuova schermata ancora.

**File:** nuovo `app/src/main/java/com/cydoniancitizen/bingee/domain/policy/ViewingDatePolicy.kt`; modificare `domain/model/WatchedStatistics.kt` e, limitatamente all'invalidazione del contesto temporale, `feature/profile/ProfileViewModel.kt` e `domain/calendar/CalendarDateSource.kt`. Tutti questi percorsi sono sotto `app/src/main/java/com/cydoniancitizen/bingee/`.

**Contratto proposto:**

- `effectiveMovieDate(watchedAt: Instant, watchedDate: LocalDate?, zoneId: ZoneId): LocalDate`
- `effectiveEpisodeDate(watchedAt: Instant, zoneId: ZoneId): LocalDate`
- `dayWindow(day: LocalDate, zoneId: ZoneId)` restituisce intervallo di Instant `[inizio giorno, inizio giorno successivo)`.
- Il contesto di osservazione deve includere sia `today` sia `zoneId`. Riutilizzare/estendere la sorgente calendario esistente senza rompere i suoi consumer, evitando una seconda infrastruttura globale.

- [x] Test: film con timestamp ottobre/data manuale settembre compare a settembre; rimuovere data manuale lo riporta a ottobre; timestamp e totale minuti invariati.
- [x] Test: data manuale invariata tra Europe/Rome e America/New_York; episodio vicino a mezzanotte ricade nel giorno del fuso selezionato.
- [x] Test: ora legale, fine mese/anno, anno bisestile, futuro effettivo escluso, film con data passata e timestamp futuro.
- [x] Implementare policy e adattare l'accumulatore mensile ad accettare il giorno effettivo. Non ricostruire una seconda lista completa della cronologia.
- [x] Invalidare la cache statistica su cambiamento di date e fuso; verificare il cambio fuso a parità di `today`. Il solo `Flow<LocalDate>` con deduplicazione non basta a rappresentare tale cambiamento.
- [x] Conservare esclusione speciali, minuti con durata ignota e altre metriche. Aggiornare solo descrizioni statistiche che ora diventerebbero false.

**Test:** nuovo `ViewingDatePolicyTest.kt`; estendere `WatchedStatisticsTest.kt`, `ProfileViewModelTest.kt` e test della sorgente calendario.

**Accettazione:** regola deterministica e unica, nessuna scrittura ai progressi; ritorno all'istogramma dopo modifica data mostra il mese corretto; nessuna regressione della precedente gestione limitata delle allocazioni statistiche.

## H2 — Lettura Room dedicata, raggruppata e limitata

**Consegna:** repository osservabile che restituisce pagine e conteggi, senza materializzare tutta la cronologia.

**File:** nuovi `core/model/ViewingHistory.kt`, `data/library/local/HistoryDao.kt`, `data/library/HistoryMappers.kt`; modifiche mirate a `domain/repository/LibraryRepository.kt`, `data/library/DefaultLibraryRepository.kt`, database/DI per esporre il DAO. Seguire i percorsi effettivi del database e del modulo Hilt presenti al momento dell'esecuzione. Aggiungere un DAO non richiede un nuovo schema.

**Interfaccia di lettura implementata nel repository esistente:**

- `observeHistory(query: HistorySnapshotQuery): Flow<AppResult<HistorySnapshot>>`

La review ha dimostrato che tre Flow indipendenti possono combinare conteggi, pagina e figli di
letture diverse. La decisione implementativa è una sola osservazione delle invalidazioni Room con
letture correlate nella stessa transazione. La richiesta contiene pagina, gruppi aperti e generazione
restore transitoria; lo snapshot contiene overview, pagina e risultati distinti dei gruppi. Nessun
nuovo schema o dato tecnico nel backup. Il restore segnala inizio/fine anche in caso di rollback;
History sospende le letture e invalida cursori, ancore ed espansioni legati agli ID locali.

I tipi sono definiti in `ViewingHistory.kt`: filtro `ALL/MOVIES/EPISODES`, contesto temporale, mese selezionato, cursori, righe film/gruppo, figli episodio, conteggio totale del gruppo, disponibilità pagina precedente/successiva. Overview contiene solo estremi validi e dati scalari necessari alla navigazione, mai tutte le date/attività.

**Identità e ordinamento:**

- Chiave film: tipo + ID locale media; chiave episodio: tipo + ID locale episodio; gruppo: giorno + ID locale serie. Conservare riferimento esterno qualificato e tipo per navigare; non deduplicare per titolo.
- Cursore principale: giorno, tipo riga, ID locale, associato a mese/filtro/fuso e generazione di caricamento. Ordinamento coerente e totale; nessun offset fisso come identità.
- Cursore figli: stagione, numero episodio, ID locale, associato al gruppo e alla generazione. La stessa pagina statica produce sempre le stesse righe anche con timestamp uguali.

- [x] Scrivere test Room per film, episodi, speciali, fuori raccolta, abbandono, record di completamento e riferimenti multipli: ogni progresso deve apparire una volta sola.
- [x] Definire query aggregate paginabili sui gruppi, con limite 30 + una riga sentinella; figli con limite 20 + sentinella. Conteggi in SQL, senza leggere tutti i figli.
- [x] Calcolare gli intervalli dei giorni in Kotlin con la policy H1 e passarli come parametri alle query. Per un mese il contesto è limitato a massimo 31 giorni. Non usare un offset UTC fisso o il `localtime` di SQLite come sostituto del `ZoneId` Java.
- [x] Verificare equivalenza dei predicati SQL con H1: i timestamp attuali sono `Instant.toString()` con frazioni variabili. Confronto/ordinamento lessicografico della stringa completa non è una prova di correttezza. Coprire valori esatti e frazionari ai due confini, senza riscrivere dati né perdere precisione persistita.
- [x] Gestire la data manuale film in modo che il filtro non sia applicato erroneamente al suo timestamp. La pagina e il conteggio devono usare gli stessi predicati.
- [x] Usare proiezioni piccole; includere solo metadati necessari, identità navigabile e appartenenza quando utile. Non chiamare `observePersonalViewing()` per costruire History.
- [x] Leggere i dati correlati necessari alla stessa pagina in modo coerente con le transazioni Room; osservare le tabelle pertinenti e propagare errori recuperabili, senza retry incontrollato.
- [x] Provare pagine consecutive e inverse su dati statici: unione completa delle righe attese, nessun duplicato/salto, gruppo unico anche con migliaia di figli.
- [x] Verificare piani query su Android. Se emergono costi eccessivi, prima ridurre proiezioni/query; non aggiungere automaticamente indici o migrazioni.

**Test:** nuovi `HistoryDaoTest.kt` in androidTest e `HistoryMappersTest.kt` in test; nuovo test di integrazione che confronta giorni/mese di History con `WatchedStatistics` sugli stessi dati regolari.

**Accettazione:** nessuna modifica schema/backup; conteggi corretti, predicati temporali equivalenti, payload limitato anche con un giorno enorme. Le query possono scandire molte righe: misurare questo costo in H5, senza confonderlo con la dimensione del risultato.

## H3 — ViewModel, pagine e riconciliazione

**Consegna:** stato testabile della schermata e lifecycle delle osservazioni.

**File:** nuovi `feature/history/HistoryViewModel.kt` e `feature/history/HistoryUiState.kt`; fake di LibraryRepository nei test/debug aggiornati al nuovo contratto senza risposte vuote che simulino un'implementazione reale.

**Ownership:** ViewModel della sola destinazione History; `ProfileViewModel` non possiede liste, pagine o collector della timeline. Stato immutabile e metodi espliciti per filtro/mese, pagina principale, apertura gruppo, pagina figli e retry.

- [x] Testare loading iniziale, vuoto, dati, errore e retry; errore di un gruppo non svuota l'intera schermata.
- [x] Usare cancellazione delle richieste precedenti al cambio filtro/mese; risultati tardivi non sostituiscono il nuovo contesto.
- [x] Sostituire le pagine anziché accodarle. Conservare al massimo la pagina principale e una pagina figli per ogni gruppo aperto in essa; liberare righe e richieste dei gruppi fuori pagina o richiusi. L'osservazione Room è unica per lo snapshot.
- [x] Con 30 gruppi e 20 figli per gruppo, il limite di payload trattenuto è 30 righe principali + 600 figli, oltre alle sentinelle transitorie. I limiti riguardano il modello letto, non solo ciò che Compose disegna.
- [x] Conservare in SavedStateHandle solo mese, filtro, cursori/ancora e stato di espansione della pagina corrente; mai liste, bitmap o l'intera pila di pagine visitate.
- [x] Al ritorno dai Dettagli, rileggere e mantenere l'ancora per identità se ancora valida. Se la voce sparisce/sposta giorno, restare nel mese/filtro scelto e usare il successivo vicino valido; se il mese diventa vuoto mostrare vuoto.
- [x] Invalidazione durante consultazione: rileggere la pagina coerentemente e sostituirla, senza combinare blocchi di generazioni diverse. Per modifiche che precedono il cursore non promettere una fotografia immutabile della storia.
- [x] Ripristino backup: invalidare cursori, espansioni e ancore basate su ID locali, che possono essere riutilizzati. Riusare il segnale di restore esistente se disponibile; altrimenti proporre il minimo segnale transitorio condiviso con restore, senza nuovi eventi persistenti o bus generico. Mantenere mese/filtro e ripartire da una pagina valida.
- [x] Cambio giorno/fuso: nuova generazione e nuovi confini, cursori precedenti invalidati; preservare selezione mese quando valida.
- [x] Uscita definitiva dalla destinazione: cancellazione collector e rilascio righe. Nessun nuovo scope globale.

**Test:** nuovo `HistoryViewModelTest.kt`, con richieste ritardate, click rapidi, rimozione ultimo elemento, spostamento tra mesi, cambio fuso senza cambio giorno, restore con riuso ID e verifica rilascio consumer.

**Accettazione:** memoria del modello limitata, errori distinti da vuoto, nessuna emissione obsoleta o associazione a ID riciclati, posizione preservata quando i dati lo permettono.

## H4 — Schermata e navigazione

**Consegna:** percorso completo Il tuo bingee → La tua storia → Dettagli → ritorno.

**File:** nuovi `feature/history/HistoryScreen.kt` e componenti locali solo se coesi; modificare `feature/profile/ProfileScreen.kt`, `core/navigation/BingeeNavHost.kt` e la definizione attuale delle route. Risorse `app/src/main/res/values/strings.xml` e `values-it/strings.xml`; preview nel source set debug.

- [x] Aggiungere un ingresso in Il tuo bingee senza riorganizzare le altre sezioni.
- [x] Implementare toolbar, selettori Tutti/Film/Episodi e mese/anno, intestazioni giorno, righe film e gruppi serie espandibili.
- [x] Pagine Precedente/Successiva chiaramente etichettate; controlli figli distinti da quelli principali. Non mostrare navigazione disabilitata come se fosse un'azione utile.
- [x] Per ogni episodio: numero stagione/episodio e titolo secondo antispoiler; identificare speciali senza chiamarli episodi regolari. Usare fallback per artwork/metadati mancanti.
- [x] Aprire la route Dettagli esistente con tipo e riferimento TMDB. Voce priva di riferimento navigabile resta leggibile senza produrre una route falsa.
- [x] Associare la posizione della LazyColumn ad ancora stabile e offset; il ritorno non deve ripartire dall'inizio se la voce esiste ancora.
- [x] Mostrare l'informativa sui limiti della cronologia e sul perimetro diverso degli speciali nelle statistiche, accessibile senza rendere ogni riga verbosa.
- [x] Usare temi/tipografia/componenti esistenti. Semantica di heading, gruppo espanso/collassato e paginazione; target almeno 48 dp, font grandi, Light/Dark, EN/IT.
- [x] Verificare che la schermata non richieda rete per leggere dati, filtri o gruppi e che il fallimento immagini non blocchi l'elenco.

**Test:** nuovi `HistoryScreenTest.kt` strumentali; estendere `ProfileScreenTest.kt` per l'ingresso; test navigazione reale e ritorno con filtri, espansioni e ancora. Preview di vuoto, film con data manuale, gruppo enorme, speciali, errore e testo lungo.

**Accettazione:** flusso completo utilizzabile offline e con TalkBack; pagine/gruppi comprensibili, nessun toggle visto o rewatch aggiunto alla timeline.

## H5 — Dati grandi, restore e coerenza end-to-end

**Consegna:** evidenze Android e regressioni sui flussi a rischio.

- [x] Preparare fixture sintetiche ripetibili: piccola mista; 10.000 episodi distribuiti; 100.000 episodi; caso con tutti i 100.000 nello stesso giorno/serie; caso con molti gruppi nello stesso giorno e film con date manuali.
- [x] Misurare prima pagina, cambio mese/filtro, apertura/pagina gruppo, ritorno dai Dettagli e invalidazione dopo una spunta. Registrare dispositivo/API, numero campioni, mediana/p95 con campioni sufficienti e limiti della misura.
- [x] Verificare mediante conteggi/heap/ownership che il dataset non viene duplicato in Profile o trattenuto dopo uscita. Confrontare stessa pagina visibile a 10.000 e 100.000 righe; distinguere memoria delle righe History da cache condivise già presenti.
- [x] Testare risultato completo attraverso tutte le pagine su fixture controllata, inclusi timestamp uguali. Sul caso enorme verificare almeno confini, conteggi e pagine campionate riproducibili.
- [x] Correggere data film da ottobre a settembre: timeline e grafico aggiornati; rimuovere data: ritorno al mese del timestamp; togliere visto: sparisce da entrambe le viste pertinenti.
- [x] Confermare rimozione dalla raccolta/abbandono senza sparizione dei progressi; speciali presenti solo nella timeline secondo contratto; nessun doppio conteggio dei completamenti.
- [x] Testare restore v1/v2/v3 secondo contratti correnti, date manuali dove supportate, rollback di backup invalido e restore mentre History è nel back stack. Nessuna nuova proprietà serializzata.
- [x] Testare nuovo processo offline, cambio fuso/ora legale, ricreazione UI, dati parziali e errori di lettura.
- [x] Soglie iniziali proposte di accettazione: nessun ANR/crash, nessun lavoro SQL o raggruppamento pesante su Main, payload entro limiti H3, nessun incremento di memoria trattenuta proporzionale all'intera cronologia. Per le latenze definire un budget sul dispositivo di riferimento prima di giudicare le misure; non inventare PASS numerici retroattivi.
- [x] Se la soluzione senza indici non soddisfa il budget, documentare query responsabile, beneficio atteso e costi. Una migrazione resta una decisione separata; non abbassare il gate o aggiungere cache persistenti come scorciatoia.

**Accettazione:** risultati funzionali e limiti di caricamento dimostrati; tempi e memoria misurati con metodo dichiarato. I limiti massimi dei formati legacy restano quelli documentati: un test a 100.000 episodi non autorizza restringere il restore.

## H6 — Verifica finale e documentazione

**Consegna:** milestone chiudibile nello scope approvato, distinta dalla pubblicazione.

- [x] Rieseguire test mirati e suite complete previste dalla roadmap: JVM, Android, Room/migrazioni/backup pertinenti, Spotless, lint, build debug/test/release.
- [x] Matrice UI almeno 320 dp/IT/font 1.5, 360 dp/IT/font 1.3, 411 dp/EN/font 1.0, 600 dp pertinente; Light/Dark, titoli lunghi, gruppi aperti, errore e vuoto. Non dichiarare una matrice completa se solo campionata.
- [x] Verificare manualmente TalkBack sulle azioni nuove e continuità dei flussi Home, Raccolta, Dettagli e Statistiche. Questo non chiude l'hardening globale v1.3.4.
- [x] Audit globale finale con Ponytail e verifiche richieste dalla roadmap. Classificare rilievi introdotti, preesistenti/rinviati e complessità giustificata; niente refactor fuori scope.
- [x] Aggiornare architettura, roadmap, nota sulle date statistiche, limiti History e note della versione quando assegnata. Versione applicativa/versionCode cambiano solo dopo scelta della collocazione.
- [x] Verificare diff/link/Git e assenza di dati reali, segreti, fixture grandi o strumenti diagnostici in produzione.
- [x] Riportare PASS/FAIL/NOT RUN, test riutilizzati rispetto a nuove esecuzioni, ambiente e limiti. Nessuna chiusura se un gate richiesto resta non verificato.

## Comandi per l'esecutore

Esempi dalla root su Windows; usare `./gradlew` sugli altri sistemi. Confermare package e nomi delle nuove classi al momento dell'implementazione. Non eseguire connected sull'installazione personale.

```powershell
# H1
.\gradlew.bat :app:testDebugUnitTest --tests "*ViewingDatePolicyTest" --tests "*WatchedStatisticsTest" --tests "*ProfileViewModelTest" --tests "*CalendarDateSourceTest"

# H2/H3
.\gradlew.bat :app:testDebugUnitTest --tests "*HistoryMappersTest" --tests "*HistoryViewModelTest"
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.cydoniancitizen.bingee.data.library.local.HistoryDaoTest"

# H4
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.cydoniancitizen.bingee.feature.history.HistoryScreenTest,com.cydoniancitizen.bingee.feature.profile.ProfileScreenTest"

# H6, senza filtri residui
.\gradlew.bat spotlessCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease
.\gradlew.bat :app:connectedDebugAndroidTest
git diff --check
git status --short
```

La build release non autorizza firma/pubblicazione. Seguire le condizioni di firma della roadmap; non creare credenziali o usare chiavi personali per aggirare un blocco. Nuove esecuzioni richieste devono essere effettive: distinguere `UP-TO-DATE`/`FROM-CACHE`.

## Handoff e stato

### Evidenze e limiti del checkpoint del 20 ottobre 2026

H1–H5 sono implementate. Il riferimento Android è HONOR CMA-LX1 / Android 13 API 33, con
installazione sintetica separata da quella personale. Le fixture comprendono 10k/100k episodi,
distribuzione su 20 giorni, 100k nello stesso gruppo e 300 serie nello stesso giorno con 100 film
di cui le date manuali ricadono in due mesi. Nessuna API reale è necessaria ai test.

Con build debug e database in memoria, 3 warmup e 20 campioni, lo snapshot completo più costoso
misura mediana 756,62 ms e p95 763,98 ms, con 30 gruppi richiesti aperti. Il budget provvisorio
fissato prima del confronto era p95 1000 ms per la lettura principale e 250 ms per una pagina figli;
questi scenari lo rispettano. Non sono tempi release, disco freddo, frame o garanzie su altri telefoni.
Il confronto iniziale ha motivato la sostituzione della scansione per ogni giorno con selezione
binaria; non introduce indici o cache. I piani SQL sono verificati sullo schema Room 9 vuoto di un
emulatore Android 13: sul framework HONOR anche EXPLAIN SELECT 1 fallisce. Le misure di latenza
restano quelle del telefono, distinte da questi piani statici.

Il payload è verificato tramite conteggi e ownership: massimo 30 parent e 20 figli per gruppo,
con sentinelle transitorie; i consumer si rilasciano dopo il periodo di grazia. Non si attribuisce
un risparmio RAM/PSS dell'intera app, né si riapre la cache delle statistiche precedente. La scansione
SQL resta proporzionale ai dati: limiti del risultato e costo della query sono proprietà diverse.

Su 23 film sintetici e 20 campioni dopo 3 warmup, ritorno dai Dettagli: mediana 330,56 ms / p95
392,47 ms; scrittura del progresso fino all'aggiornamento della riga: 107,75 / 114,76 ms. Il polling
Compose è incluso; non è una misura dei frame né una prova di navigazione su 100k titoli.

Navigazione reale, ritorno, filtro e viewport dopo ricreazione sono verificati. Il restore usa una
transazione reale con un owner History conservato e senza subscriber, rappresentando la sua
permanenza nel back stack; non è un test completo della selezione file SAF tramite UI. La prova
offline usa due invocazioni separate e un processo nuovo senza permesso INTERNET nell'app di test:
film, gruppi, speciali e ritorno dai Dettagli funzionano sui dati già su disco.

La matrice adattiva passa in otto configurazioni: IT chiaro/scuro 320/font1.5 e 360/font1.3;
EN chiaro/scuro 411/font1.0 e 600/font1.5. Sono configurazioni simulate sul telefono, non una
certificazione tablet. Titoli lunghi, gruppo aperto, entrambe le paginazioni e informativa sono
verificati; loading/vuoto/errore/retry hanno test separati, non il prodotto cartesiano con la matrice.

La review Ponytail ha identificato due rilievi introdotti, entrambi corretti: espansioni scomparse
conservate nel saved state e combinazione di Flow di generazioni diverse. Il relativo audit globale
ha campionato backup, progressi, import, credenziali e notifiche; non dichiara di aver letto ogni file.
I gate automatici H6 passano: 528 test JVM sull’host, 388 test nella suite completa sul telefono più
un test nativo aggiuntivo sui tre formati di backup; Spotless, lint (0 errori/52 warning), build
debug/test/release e R8. Il controllo finale del sorgente dopo il test aggiunto passa. Un crash
intermedio di lint su sorgenti Hilt release in rigenerazione è risolto eseguendo lint dopo la build.
La prima esecuzione Android aveva il Centro di controllo sopra l’app: diagnosi nativa confermata,
pannello richiuso e test invariato più successiva suite completa superati. Il proprietario riferisce esito
manuale TalkBack positivo per filtri, mese/anno, espansione gruppo, informativa e ritorno dai
Dettagli: tutti i controlli provati sono chiari e raggiungibili. La continuità Home/Raccolta/Dettagli/
Statistiche è coperta dalla suite nativa; paginazione e retry hanno prove automatiche. Non si
estende il risultato manuale a tutte le azioni e combinazioni, né all’hardening globale v1.3.4. Evidenze e strumenti sono sotto
`.audit/history-timeline/`, esclusi da Git; nessuna chiusura complessiva di v1.3.0 è implicata.

- [x] H1 — Date condivise e statistiche allineate.
- [x] H2 — Query e repository paginati.
- [x] H3 — Stato, lifecycle e riconciliazione.
- [x] H4 — UI e navigazione; prova manuale TalkBack campionata registrata in H6.
- [x] H5 — Grandi cronologie, restore e misure Android, nei limiti dichiarati sotto.
- [x] H6 — Verifica, audit e documentazione nello scope e nei limiti dichiarati.

Al termine di ogni incremento: comportamento consegnato, file modificati, test eseguiti, gate/audit coperti, limiti e passo successivo. Procedere nelle attività già autorizzate; evidenziare nuovi compromessi strutturali prima di implementarli. Esecuzione autorizzata per H1–H6; nessuna chiusura automatica di v1.3.0 e nessun commit, push o pubblicazione.
