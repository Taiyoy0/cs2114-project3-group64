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

These results apply to this integrated source copy, tested outside the Eclipse interface. Importing and running the replacement project in Ari's Eclipse workspace still needs confirmation. The checks do not cover every public method exhaustively. The Eclipse Java compiler reports one existing warning that Queue's `backNode` field is maintained but never read; it does not prevent execution.

## Reflection

### What changed from our specification?

Our Revised Scope moved severity-based priority into the MVP. The implementation follows that decision using Ticket's comparison logic and our custom linked Queue, while preserving FIFO within each severity. Tickets now follow FIFO within each severity level, rather than across all tickets.

The spec mixed enum fields and String-based method signatures. We retained the existing String fields but validate category, severity, and status. The accepted category list was defined during integration because the spec did not enumerate it. IDs are stored as Strings, matching the Data & State section, but must represent positive whole numbers; leading zeroes and surrounding spaces are normalized so `001` and `1` are treated as the same ID.

The controller now calls the model's actual `peekNextTicket()`, `savePendingTickets()`, and `loadPendingTickets()` methods and uses `Ticket.getId()`. Ticket submission includes severity as its fourth argument. Samuel's view and view test were moved into the `ticketline` package. The view's unused controller reference was removed, a no-argument constructor and injectable input/output streams were supplied, and severity and yes/no prompts were added. The controller uses Samuel's input and display methods instead of replacing his view. Two output messages were aligned with his tests, and processed tickets use a generic details heading rather than a next-ticket heading.

Validation stays with the responsible layer: Ticket checks field/update values, HelpDeskModel checks duplicate IDs, Queue checks null entries and empty operations, and TicketController checks menu/numeric input. String equality and whitespace handling were corrected. The view collects raw input and displays results; it does not validate ticket rules or access model objects.

Persistence now uses quoted CSV for descriptions containing commas, quotes, or newlines. Loading accepts the earlier unquoted five-field format, validates a temporary queue, and replaces pending state only after the entire file is valid. Missing files leave state unchanged, and invalid loads report errors without partially updating the queue.

We added ModelDemo and ModelChecks to demonstrate model independence and check its behavior. Samuel's JUnit view tests were retained, and ConsoleIntegrationTest was added for the combined application. This is meaningful prototype coverage but not yet the full per-public-model-method JUnit suite proposed in the spec.

### Did the model really stand alone?

Yes. Ticket, Queue, and HelpDeskModel compile and run with the standalone drivers without ConsoleView or TicketController. The model does not print interface messages, read keyboard input, or reference the view. It returns tickets/results or throws exceptions. The controller translates those into display values and messages. Only the driver prints model demonstration results; no interface code was moved into the model.

### What happened to our unknowns?

The prototype demonstrates how to order tickets by severity, preserve equal-severity order, move a pending ticket into completed state, validate duplicate IDs, save/reload pending tickets, and connect the MVC classes through matching methods. Those implementation questions have working code and checks. Questions remaining for the next feature are how to find an editable pending ticket by ID, reject completed/missing IDs, and verify that editing preserves queue order and survives restart. We still need to compare these results with the original scope document's unknowns list; that list is not included in the supplied spec.

### What does not work yet?

No failure was observed in the tested MVP paths. Editing through the menu, average resolution time, and estimated wait times are not implemented.

Completed tickets are not persisted, as the MVP requires only pending-ticket persistence. Consequently, ID uniqueness against completed tickets is session-only. Returned lists protect collection membership but still contain mutable Ticket references; they are not deep immutable copies. Saves are not atomic, so an interrupted write could leave a partial file. Full automated coverage of every public model method remains unfinished.

## Next Steps

### Remaining MVP work

The core MVP features are implemented and pass the checks above. Our remaining steps, in order, are:

1. Confirm that the replacement ZIP imports and the menu runs in Eclipse.
2. Manually check submission, pending/next-ticket display, processing, and save/restart in that Eclipse project.
3. Review the integration as a group and confirm category choices, attribution, and the original scope's unknowns.
4. Record any remaining differences between the spec and the implemented methods and tests.

### Deliverable 4 change feature

Ari plans to implement **editing a pending ticket's description by ID**. A user will choose an edit option, enter a pending ticket ID, supply a nonblank replacement description, and receive confirmation. The model will reject missing or completed IDs. Editing must preserve all other fields and processing order, and the edited description must survive save/restart.

| Class | Predicted impact |
|---|---|
| HelpDeskModel | Add a pending-ticket lookup/edit operation and its validation. |
| TicketController | Add menu routing and an edit handler. |
| ConsoleView | Add the edit menu option, prompts, and results. |
| Ticket | No change expected: updateDescription already exists and validates text. |
| Queue and internal Node | No change expected: the model can find a ticket through toList; description changes do not affect priority. |

This adds model-level lookup and edit rules and a complete user interaction, rather than only a button for an already-complete workflow. The TA will judge whether it qualifies as non-trivial. We will compare the actual class changes with these predictions after implementation.

### Stretch goals

Severity priority is already in the revised MVP and will not be counted again as the change feature. The proposed additional feature is pending-ticket editing. Average resolution time and estimated wait times are not planned for Deliverable 4 unless the group changes that decision. Editing is an additional feature, rather than one of the two original stretch goals.

### New tests needed

- Edit an existing pending ticket and confirm that only its description changes.
- Reject missing/completed IDs, invalid IDs, and null/empty/whitespace descriptions without state changes.
- Preserve priority and equal-priority order after editing.
- Save/reload edited descriptions, including commas and quotation marks.
- Test edit menu routing, confirmation, errors, retries/cancellation, and input exhaustion.
- Expand per-method JUnit coverage for the model, including compareTo, size, toList, hasPendingTickets, updates, and persistence boundaries.

### Who does what? (proposed assignments)

| Member | Remaining responsibility |
|---|---|
| Ari Gallo | Integration review, end-to-end checks, README coordination, and editing across MVC layers. |
| Vivaan Dutt | Model/queue support and expanded model tests. |
| Taiyo Lloyd | Controller support and review of menu routing/error handling. |
| Samuel Abiy | View support and edit prompts/display updates. |

These future testing/support assignments follow the spec's ownership and should be confirmed by the group.

## Attribution

| Class or test file | Contributors and contribution |
|---|---|
| Ticket.java | Vivaan Dutt wrote the original class. Ari's GenAI-assisted integration changes strengthen field, ID, severity, and status validation. |
| Queue.java, including internal Node | Vivaan Dutt wrote the linked priority queue. The algorithm was retained; model checks verify ordering and boundary behavior. |
| HelpDeskModel.java | Vivaan Dutt wrote the original model. Ari's GenAI-assisted changes correct duplicate-ID checks and persistence/loading. |
| TicketController.java | Taiyo Lloyd wrote the original controller. Ari's GenAI-assisted revisions align model/view calls, severity input, input exhaustion, and an injectable save path for isolated tests. |
| ConsoleView.java | Samuel Abiy wrote the original view. Ari's GenAI-assisted integration revisions add the package, constructors/stream injection, missing prompts, EOF handling, and consistent display messages. |
| ConsoleViewTest.java | Samuel Abiy wrote the 10 original JUnit display tests. Ari's GenAI-assisted changes update the package, construction, and captured output; all 10 passed. |
| ModelChecks.java | Generated with GenAI for Ari's integration work; 45 model checks executed successfully. |
| ModelDemo.java | Generated with GenAI for Ari's integration work and executed independently of the view. |
| ConsoleIntegrationTest.java | Generated with GenAI for Ari's integration work; 10 combined console/controller/model tests executed successfully. |

### GenAI note

ChatGPT inspected the repository and supplied spec, prepared integration and validation/persistence fixes, adapted Samuel's actual view to the controller, generated the model drivers and integration tests, executed the checks, and drafted this README for Ari. Samuel's original view and view tests, Taiyo's controller, and Vivaan's model/queue classes were used as the starting code. This note describes the assistance used in Ari's integration work. Any GenAI use by other members still needs their confirmation. Automated test results do not imply that teammates have already reviewed these changes.
