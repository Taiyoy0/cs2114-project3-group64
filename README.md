# TicketLine — Prototype 2

TicketLine helps support staff submit, prioritize, start, and complete support tickets. Critical tickets come before high, medium, and low tickets; equal-severity tickets retain submission order. The added feature tracks processing durations and estimates how long pending tickets will wait before processing starts.

## How to Run

### Eclipse

1. Import the project with **File > Import > General > Existing Projects into Workspace**. Use Java 8 or newer.
2. The project includes `src`, `tests`, and the JUnit/Hamcrest libraries in `lib`.
3. Right-click `src/ticketline/TicketController.java` and choose **Run As > Java Application**.

Use these menu choices:

| Option | Action |
|---|---|
| 1 | Submit a ticket using a unique positive numeric ID, category, description, and severity. |
| 2 | View pending tickets in priority order and their estimated waits. |
| 3 | View the next pending ticket without removing it. |
| 4 | Start processing the next ticket. It becomes the one active ticket. |
| 5 | Save unfinished tickets and exit. |
| 6 | Complete the active ticket and record its processing duration. |
| 7 | View the average processing duration of completed tickets. |

Categories are Hardware, Software, Network, Account, and Other. Severity is low, medium, high, or critical. Category and severity are case insensitive. IDs such as `001` and `1` identify the same ticket.

**To save, type `5` (Exit) in the menu and press Enter.** Closing the Console tab does not trigger saving; Eclipse's red Stop button terminates the app without triggering saving. Restart from the same working directory to restore `tickets.txt`.

In the completed design, pending and unfinished active tickets are restored as pending. An interrupted active ticket keeps its place before later tickets of equal severity and starts a new timing sample when processed again. Completed-ticket history and timing statistics cover only the current session.

### Compile and run from a terminal

With a JDK installed:

```sh
javac -cp "lib/*" -d bin src/ticketline/*.java tests/ticketline/*.java
java -cp bin ticketline.TicketController
```

### Run tests

In Eclipse, right-click each `*Test.java` file under `tests/ticketline` and choose **Run As > JUnit Test**. Run the model and timing tests independently of ConsoleView. The separate console integration tests check the connected layers.

To run all six JUnit test classes from a terminal:

```sh
java -cp "bin:lib/*" org.junit.runner.JUnitCore ticketline.TicketTest ticketline.HelpDeskModelTest ticketline.QueueTest ticketline.ResolutionTimeCalculatorTest ticketline.ConsoleViewTest ticketline.ConsoleIntegrationTest
```

On Windows, use `"bin;lib/*"` instead of `"bin:lib/*"`.

`ModelDemo` and `ModelChecks` remain Java applications rather than JUnit tests. They exercise the original immediate-processing API directly. They do not by themselves demonstrate the new start/complete lifecycle.

## Timing Rules

The average is the total processing duration divided by the number of successfully completed timing samples, rounded down to whole seconds. Zero-second samples are valid. With no samples, the model returns `-1`, and the controller displays **Not enough data.**

For a pending ticket:

**Estimated wait = active ticket's estimated remaining time + average duration × number of pending tickets ahead.**

The active ticket's remaining estimate is `max(0, average − elapsed processing time)`. With no active ticket it is zero. Queue position uses the existing severity order and equal-severity FIFO. Estimates are recalculated when displayed, are not guarantees, and assume one staff member and no future arrivals. A new high-priority arrival can increase another ticket's estimate. Missing, completed, or active ticket IDs are rejected because this calculation applies only to pending tickets.

## Verification

| Check | Result |
|---|---|
| TicketTest | 8 JUnit tests passed. |
| HelpDeskModelTest | 25 JUnit tests passed. |
| ConsoleViewTest | 15 JUnit tests passed. |
| ConsoleIntegrationTest | 15 JUnit tests passed. |
| ResolutionTimeCalculatorTest | 15 JUnit tests passed. |
| QueueTest | 11 JUnit tests passed, including interleaved enqueue/dequeue ordering. |
| ModelChecks | 45 standalone model checks passed. |
| Total | 89 JUnit tests passed and 45 standalone model checks passed. |


## Reflection

### How did the design absorb the change?

The feature touched Ticket, HelpDeskModel, TicketController, and ConsoleView, as predicted in Deliverable 3. Ticket gained timing information. HelpDeskModel gained active-ticket lifecycle operations and coordinated timing and persistence. TicketController connected menu actions to those operations and formatted durations. ConsoleView gained start/complete actions and timing displays.

We also added ResolutionTimeCalculator in the model layer. This was an addition to the Deliverable 3 class prediction: separating the calculations let Ari implement and test the math while Vivaan developed ticket timing and lifecycle behavior. It records completed samples and calculates waits from a supplied pending list and elapsed duration. It does not control tickets or store a copy of the queue.

Queue.java and its internal Node did not need feature changes, matching our prediction. Existing enqueue/dequeue/peek behavior already supplied the correct processing order. We expanded QueueTest to check order when enqueues and dequeues are interleaved. Ticket ID/category/description validation also remained unchanged.

### Did the change cross the model/view line?

The model owns the queue, active ticket, completed tickets, and timing statistics. The view keeps no copy of that state. The controller fetches current model values for each display and passes plain strings and transient display rows to the view. The view learned how to display a wait estimate and timing summary, but it did not learn queue traversal, timing formulas, file parsing, or ticket lifecycle rules. Timing therefore flowed through the existing MVC boundary without moving business rules into the view. The calculator has no console, view, or controller dependency.

### What changed from the Deliverable3 plan, and why?

We kept the same feature: resolution-time tracking and estimated waits. The model retains the public facade methods getAverageResolutionTime() and getEstimatedWaitTime(String id), while delegating arithmetic to ResolutionTimeCalculator. Controller handlers use the existing handleProcessNext(), handleCompleteCurrent(), and handleShowTimingSummary() naming rather than the preliminary names in D3.

The controller uses `long` whole seconds and a `-1` unavailable result. Keeping that contract consistent avoids mixing milliseconds with displayed seconds. Tests use supplied durations and a controllable lifecycle clock rather than waiting in real time. Whole-second averages round down, which keeps formatting and assertions predictable.

Persistence returns unfinished active tickets to pending on restart rather than counting interrupted work as a completed timing sample. Timing statistics remain session-only, as planned. This avoids treating time while the application was closed as staff processing time.

Integration required startNextTicket() and completeCurrentTicket() to return the affected Ticket so the controller could display it. We separated the active ticket from getPendingTickets() and hasPendingTickets() so wait estimates apply only to queued work. Saving and loading were aligned to restore interrupted tickets as Open, preserve their order among equal-priority tickets, and discard unfinished timing samples. We also restored the existing input-validation rules omitted during the timestamp changes and removed obsolete root-level view copies. These fixes connected the team’s classes without changing Queue.java.

### What does not work yet?

For the completed prototype, editing pending ticket descriptions and durable completed-ticket history remain outside scope. Statistics reset on restart. Estimates are approximate and do not model multiple staff, future arrivals, or per-category processing times. Saving requires menu option 5; there is no automatic save.

### What design change would make the next feature cheaper?

A small immutable ticket-display snapshot returned by HelpDeskModel would make future display features cheaper. Currently the controller constructs positional string rows. Named fields for ID, status, severity, and estimated wait would reduce mistakes when adding another displayed value and would avoid exposing mutable Ticket objects. The view could still render the supplied snapshot without owning or updating application state. This is a future improvement, not an implemented class.

## Next Steps

### Remaining work for the final build, in order

1. **Vivaan:** Review the completed core model tests and address any newly discovered lifecycle or persistence issues.
2. **Ari and Taiyo:** Review the passing controller integration tests and confirm the workflow in the team’s Eclipse environments.
3. **Samuel and Taiyo:** Verify menu wording, current-state displays, retry behavior, and timing formatting on the integrated source.
4. **All members:** Rehearse the timed demo, produce one clean final source/test/README submission, start discussing slide preparation/creation.


### Stretch goals committed versus dropped

We have committed to resolution-time tracking and estimated waits, and made the severity priority a part of the revised MVP. Editing pending ticket descriptions is deferred potentially for Deliverable5. Multi-staff scheduling and persistent timing history are not planned for this build.

### Three-minute demo plan


| Time | Action and evidence |
|---|---|
| 0:00–1:00 | Run TicketController; submit a low and two critical tickets. Show critical-first ordering, FIFO between critical tickets, and Not enough data before completion. |
| 1:05–2:00 | Start the first critical ticket with option 4. After showing the current-ticket output and attempting another start, complete it with option 6. Show the summary with option 7 and updated waits with option 2. |
| 2:05–2:30 | Demonstrate a duplicate ID and an invalid menu choice without crashing. |
| 2:30–3:00 | Start another ticket, exit with option 5, restart, and show unfinished work restored correctly. Note that session timing statistics reset. |



## Attribution

| Class or test file | Contributors and contribution |
|---|---|
| Ticket.java | Vivaan Dutt wrote the original class and added processing timestamps and duration tracking for Deliverable4 |
| Queue.java, including Node | Vivaan Dutt wrote the linked priority queue. Its existing priority and FIFO behavior supported the timing feature without changes to the class. |
| HelpDeskModel.java | Vivaan Dutt wrote the original model and added current-ticket tracking, start/complete operations, and unfinished-ticket persistence. |
| TicketController.java | Taiyo Lloyd wrote the original controller and added Deliverable4 handlers for starting tickets, completing the current ticket, displaying timing summaries, and requesting wait estimates. |
| ConsoleView.java | Samuel Abiy wrote the original view and added Deliverable4 menu options, current-ticket displays, estimated waits, and timing summaries. |
| ConsoleViewTest.java | Samuel Abiy wrote the original display tests and updated them for the Deliverable4 menu and timing displays. |
| ResolutionTimeCalculator.java | Ari developed the Deliverable4 average-resolution-time and estimated-wait calculations. |
| TicketTest.java | Ari added direct ticket validation, lifecycle, and clock JUnit tests. |
| HelpDeskModelTest.java, including nested MutableClock | Ari added direct model timing, lifecycle, persistence, and error-case JUnit tests. |
| ResolutionTimeCalculatorTest.java | Ari added tests for timing calculations, unavailable data, invalid inputs, and changing queue order. |
| QueueTest.java | Ari added the original queue tests and expanded them for Deliverable4 to verify ordering after interleaved enqueues and dequeues. |
| ModelChecks.java | Ari added standalone checks for ticket validation, queue behavior, processing, and persistence. |
| ModelDemo.java | Ari added a driver demonstrating model operations independently of the view. |
| ConsoleIntegrationTest.java | Ari added integration tests and updated them for the separate start/complete workflow and timing feature. |


### GenAI note

The team used GenAI, such as ChatGPT & Claude, tools as assistance, not as a substitute for understanding or reviewing the program. ChatGPT was used for debugging guidance, integration discussions in eclipse suggestions for JUnit tests, and save/load code for saving tickets, validation/persistence fixes and timing calculation guidance. Additionally it was helpful when integration efforts weren't work from Deliverable 2 to 3 and Deliverable 3 to 4. Each member’s individual AI use is documented in their separate AI note assignment on Canvas.