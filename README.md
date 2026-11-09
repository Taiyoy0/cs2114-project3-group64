# TicketLine

TicketLine is a console prototype for support staff to submit, view, and process support tickets. A custom linked priority queue handles critical tickets before high, medium, and low tickets. Equal-severity tickets retain submission order. Pending tickets are saved on exit and restored when the application restarts.

## How to Run

### Console prototype in Eclipse

1. In Eclipse, choose **File > Import > General > Existing Projects into Workspace**. Select the project folder, or select the ZIP using **Select archive file**, then click **Finish**.
2. Use Java 8 or newer. The `lib` folder includes JUnit 4 and Hamcrest, so no separate JUnit library setup is required.
3. Expand **src > ticketline**, right-click **TicketController.java**, and choose **Run As > Java Application**.

If Eclipse says the project already exists, it has detected another project with the same internal name. The replacement archive `TicketLine-ready.zip` uses the project name **TicketLine-Integrated** to avoid the earlier name conflict.

The menu supports submitting tickets, viewing pending tickets, viewing the next ticket, processing the next ticket, and saving/exiting. Enter category names as displayed, rather than numbers. Categories are Hardware, Software, Network, Account, and Other; severities are low, medium, high, and critical. Categories and severity are case insensitive. IDs must be positive whole numbers.

The application uses `tickets.txt` in its working directory. To check persistence manually, submit a ticket, choose Exit, restart from the same directory, and view pending tickets. Completed tickets are kept for the current session only.

### Run the model without the view

Run `tests/ticketline/ModelDemo.java` as a Java application. It creates three tickets, prints their priority order, processes one, and saves/reloads the remaining tickets using a temporary file.

Run `tests/ticketline/ModelChecks.java` as a Java application to execute the 45 standalone model checks. Neither file requires ConsoleView, TicketController, keyboard input, or the real `tickets.txt` file.

### Run the JUnit tests

Run `ConsoleViewTest.java` and `ConsoleIntegrationTest.java` in `tests/ticketline` as JUnit tests. The first checks Samuel's display methods; the second exercises the real view/controller/model together using supplied input and captured output. Integration tests use temporary save locations and do not overwrite a user's save file.

### Command line

With a JDK, compile and run the application:

```sh
javac -d bin src/ticketline/*.java
java -cp bin ticketline.TicketController
```

For the standalone model drivers:

```sh
javac -d bin src/ticketline/Ticket.java src/ticketline/Queue.java src/ticketline/HelpDeskModel.java tests/ticketline/ModelDemo.java tests/ticketline/ModelChecks.java
java -cp bin ticketline.ModelDemo
java -cp bin ticketline.ModelChecks
```

JUnit tests require JUnit 4 and Hamcrest on the classpath. Both JAR files are included in `lib` and referenced directly by the Eclipse project; no JUnit container setup is needed. No external course-support projects are required by this source.

## Verification Results

The integrated source was compiled for Java 8 compatibility using the Eclipse Java compiler and executed on Java 21.

- **45 standalone model checks passed.** These cover priority and equal-priority FIFO, duplicate pending/completed IDs, invalid fields, empty queues, processing, snapshot membership, and save/load behavior, including malformed files and descriptions with commas, quotes, or newlines.
- **20 JUnit tests passed:** 10 view tests and 10 integration tests.
- The integrated MVP path was tested: submit tickets, display pending tickets, inspect next, process a ticket, save/exit, and load into a new application session.
- Additional interaction tests cover invalid menu/ID input, invalid category/severity, blank descriptions, retry/cancel, duplicate IDs, empty operations, malformed save data, input exhaustion, and save failures.
- ModelDemo was executed separately to demonstrate the model without any view.

## Reflection

Integration revealed mismatched method names and package locations, so we aligned the controller calls and placed the view and its tests in the ticketline package. We added missing prompts to support severity selection and error recovery. Injectable input/output streams allowed automated console tests without keyboard input. Validation fixes prevented duplicate IDs and invalid ticket data. Quoted CSV preserved descriptions containing commas, quotes, and newlines, while temporary loading prevented malformed files from partially changing model state

### What changed from our specification?

Our Revised Scope moved severity-based priority into the MVP. The implementation follows that decision using Ticket's comparison logic and our custom linked Queue, while preserving FIFO within each severity. Tickets now follow FIFO within each severity level, rather than across all tickets.

The spec mixed enum fields and String-based method signatures. We retained the existing String fields but validate category, severity, and status. The accepted category list was defined during integration because the spec did not enumerate it. IDs are stored as Strings, matching the Data & State section, but must represent positive whole numbers; leading zeroes and surrounding spaces are normalized so `001` and `1` are treated as the same ID.

The controller now calls the model's actual `peekNextTicket()`, `savePendingTickets()`, and `loadPendingTickets()` methods and uses `Ticket.getId()`. Ticket submission includes severity as its fourth argument. Samuel's view and view test were moved into the `ticketline` package. The view's unused controller reference was removed, a no-argument constructor and injectable input/output streams were supplied, and severity and yes/no prompts were added. The controller uses Samuel's input and display methods instead of replacing his view. Two output messages were aligned with his tests, and processed tickets use a generic details heading rather than a next-ticket heading.

Validation stays with the responsible layer: Ticket checks field/update values, HelpDeskModel checks duplicate IDs, Queue checks null entries and empty operations, and TicketController checks menu/numeric input. String equality and whitespace handling were corrected. The view collects raw input and displays results; it does not validate ticket rules or access model objects.

Persistence now uses quoted CSV for descriptions containing commas, quotes, or newlines. Loading accepts the earlier unquoted five-field format, validates a temporary queue, and replaces pending state only after the entire file is valid. Missing files leave state unchanged, and invalid loads report errors without partially updating the queue.

We added ModelDemo and ModelChecks to demonstrate model independence and check its behavior. Samuel's JUnit view tests were retained, and ConsoleIntegrationTest was added for the combined application.

### Did the model really stand alone?

Yes. Ticket, Queue, and HelpDeskModel compile and run with the standalone drivers without ConsoleView or TicketController. The model does not print interface messages, read keyboard input, or reference the view. It returns tickets/results or throws exceptions. The controller translates those into display values and messages.

### What happened to our unknowns?

The prototype demonstrates how to order tickets by severity, preserve equal-severity order, move a pending ticket into completed state, validate duplicate IDs, save/reload pending tickets, and connect the MVC classes through matching methods. Those implementation questions have working code and checks. Remaining questions include how to track an active ticket, calculate reliable wait estimates, and preserve unfinished work across restarts. We will address these in Deliverable 4.

### What does not work yet?

Editing through the menu, average resolution time, and estimated wait times are not implemented (stretch goals). Completed tickets are not persisted, as the MVP requires only pending-ticket persistence. Consequently, ID uniqueness against completed tickets is session-only. 

## Next Steps

### Remaining MVP work

All MVP features are complete: submitting tickets, viewing pending tickets, inspecting and processing the next ticket, and saving and restoring pending tickets. No MVP work remains. Deliverable 4 will focus on the additional feature below and its tests.

### Deliverable 4 change feature

We will add **resolution-time tracking and estimated wait times**. Staff will start a ticket and mark it completed separately. The model will record the processing duration, calculate the average duration of completed tickets, and estimate pending tickets' waits using their priority order. Before any tickets are completed, the app will display “Not enough data.” These statistics will initially cover the current session.

This feature adds model state, calculations, and user actions accessible through the controller and view. It was not part of our MVP. The following method names are proposed; we will agree on their signatures before implementation and compare these predictions with the actual changes afterward.

### Predicted code changes

| Class | Specific methods expected to change or be added |
|---|---|
| Ticket | Add startProcessing(), completeProcessing(), and getResolutionDuration() to record timestamps and return processing duration. Use a controllable clock for reliable timing tests. |
| HelpDeskModel | Add startNextTicket(), completeCurrentTicket(), getCurrentTicket(), getAverageResolutionTime(), and getEstimatedWaitTime(String id). Replace the immediate-completion workflow in processNextTicket() with separate start and complete operations. |
| TicketController | Update start() menu routing and processNextTicket() handling. Add completeCurrentTicket() and showTimingSummary() handlers. Update viewPendingTickets() to supply wait estimates for display. |
| ConsoleView | Update displayMenu() and displayPendingTickets(...). Add displayCurrentTicket(...) and displayTimingSummary(...) to show the active ticket and timing results. |
| HelpDeskModel persistence | Update savePendingTickets() and loadPendingTickets() so an unfinished current ticket is preserved and restored without loss or duplication. On restart, return that ticket to pending, preserving its order among equal-priority tickets, and discard its interrupted timing sample. Continue accepting existing save files. |

We expect Queue.enqueue(), dequeue(), peek(), isEmpty(), size(), and toList(), along with its internal Node, to remain unchanged because priority ordering already works. Ticket ID, category, and description validation should also remain unchanged.

Wait estimates assume one staff member processes tickets at a time and no new tickets arrive. For a pending ticket, estimate the active ticket's remaining time as the average duration minus its elapsed processing time, with a minimum of zero, then add the average duration for each pending ticket ahead of it. Estimates will be recalculated when displayed and may change when higher-priority tickets arrive.

### Stretch goals

We commit to average resolution time and estimated wait times for Deliverable 4. Severity priority is already implemented in the revised MVP and will not be counted again as the new feature. Editing pending ticket descriptions is deferred so we can finish and test the timing non-trivial feature first.

### New tests needed

- startNextTicket(): starting a ticket removes it from pending and makes it current; an empty queue or an already-active ticket is rejected without changing state.
- completeCurrentTicket(): completing a ticket clears the current ticket, adds it to completed, and records its duration; completing without an active ticket is rejected.
- getAverageResolutionTime(): known durations produce the correct average; no completed tickets produces an unavailable result that the view displays as “Not enough data.”
- getEstimatedWaitTime(String id): estimates follow priority order, include the active ticket's estimated remaining time, and never become negative. Missing, invalid, or completed IDs are rejected without changing state.
- Save/load: an unfinished current ticket survives restart without duplication, returns to the correct pending position, and contributes no interrupted duration to the average. Existing save files still load.
- Controller/view: verify start and complete menu actions, timing displays, invalid choices, and errors.
- Regression: existing submission, duplicate-ID validation, priority ordering, and pending-ticket persistence still work.

Timing tests will use a controllable clock so they do not require real waiting. Model tests will run without the view.

### Who does what? (proposed assignments)

| Member | Deliverable 4 responsibility |
|---|---|
| Ari Gallo | Implement getAverageResolutionTime() and getEstimatedWaitTime(String id) and their tests; integrate the feature, run end-to-end tests, and update the README. |
| Vivaan Dutt | Implement ticket timestamps, model start/complete operations, persistence updates, and related model tests. |
| Taiyo Lloyd | Implement controller menu routing, start/complete handlers, timing-result handling, and controller error-handling tests. |
| Samuel Abiy | Update the menu, current-ticket and timing displays, and view tests. |

We will agree on method signatures first. Once the model operations and view methods are ready, Taiyo will connect them through the controller. Ari will then integrate and test the complete workflow and run regression tests before submission.

## Attribution

| Class or test file | Contributors and contribution |
|---|---|
| Ticket.java | Vivaan Dutt wrote the original class. |
| Queue.java, including internal Node | Vivaan Dutt wrote the linked priority queue. |
| HelpDeskModel.java | Vivaan Dutt wrote the original model. |
| TicketController.java | Taiyo Lloyd wrote the original controller. |
| ConsoleView.java | Samuel Abiy wrote the original view. |
| ConsoleViewTest.java | Samuel Abiy wrote the 10 original JUnit display tests. |
| ModelChecks.java | Ari wrote; 45 model checks executed successfully. |
| ModelDemo.java | Reviewed with GenAI for Ari's integration work and executed independently of the view. |
| ConsoleIntegrationTest.java | Reviewed with GenAI for Ari's integration work; 10 combined console/controller/model tests executed successfully. |

### GenAI note
The team used GenAI tools as assistance, not as a substitute for understanding or reviewing the program. ChatGPT was used for debugging guidance, integration discussions in eclipse suggestions for JUnit tests, and save/load code for saving tickets, validation/persistence fixes. Additionally it was helpful when integration efforts weren't work. Each member’s individual AI use is documented in their separate AI note assignment on Canvas.
