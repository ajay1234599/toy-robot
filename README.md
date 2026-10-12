# Toy Robot

A simulation of a small truck robot driving around on a square tabletop. You tell the robot where to stand and where to go, and it tells you where it ended up. Whatever you tell it, **the robot will never drive off the edge of the table.**

This README has two parts:

- **[Part 1: For everyone](#part-1-for-everyone).** What this is and how to try it, with no programming knowledge needed.
- **[Part 2: For developers](#part-2-for-developers).** Technical reference: endpoints, errors, logging, tests and code structure.

---

# Part 1: For everyone

## What is this?

Picture a tabletop divided into a grid of **5 by 5 squares**, like a small chessboard. A toy robot sits on one square and faces one of four directions: **NORTH**, **EAST**, **SOUTH** or **WEST**.

You control the robot by sending it short commands, such as "move forward" or "turn left". After each instruction, it can report where it is and which way it's facing.

There's no picture or animation on screen. You send the robot instructions, and it answers in text, like `3,3,NORTH`.

## The table

Every square has an address made of two numbers: **X** (how far across, left to right) and **Y** (how far up, bottom to top). Both go from 0 to 4.

```
                          NORTH ↑

              ┌─────┬─────┬─────┬─────┬─────┐
        Y=4   │ 0,4 │ 1,4 │ 2,4 │ 3,4 │ 4,4 │
              ├─────┼─────┼─────┼─────┼─────┤
        Y=3   │ 0,3 │ 1,3 │ 2,3 │ 3,3 │ 4,3 │
              ├─────┼─────┼─────┼─────┼─────┤
WEST ←  Y=2   │ 0,2 │ 1,2 │ 2,2 │ 3,2 │ 4,2 │  → EAST
              ├─────┼─────┼─────┼─────┼─────┤
        Y=1   │ 0,1 │ 1,1 │ 2,1 │ 3,1 │ 4,1 │
              ├─────┼─────┼─────┼─────┼─────┤
        Y=0   │ 0,0 │ 1,0 │ 2,0 │ 3,0 │ 4,0 │
              └─────┴─────┴─────┴─────┴─────┘
                X=0   X=1   X=2   X=3   X=4

                          SOUTH ↓
```

- The bottom-left square, **0,0**, is the corner furthest **SOUTH** and **WEST**.
- Going **NORTH** (up) makes Y bigger. Going **EAST** (right) makes X bigger.

## The five commands

| Command | What it does | Example |
|---|---|---|
| **PLACE X,Y,F** | Puts the robot on square X,Y, facing direction F. This is always the first step. | `PLACE 0,0,NORTH` puts the robot in the bottom-left corner, facing up. |
| **MOVE** | Drives the robot forward one square, in the direction it's facing. | Facing NORTH at 0,0, `MOVE` takes it to 0,1. |
| **LEFT** | Turns the robot 90° to its left. It stays on the same square. | Facing NORTH, `LEFT` makes it face WEST. |
| **RIGHT** | Turns the robot 90° to its right. It stays on the same square. | Facing NORTH, `RIGHT` makes it face EAST. |
| **REPORT** | The robot tells you where it is and which way it's facing. | `0,1,NORTH` |

Commands can be in capitals or lowercase: `move` works the same as `MOVE`.

## The safety rules

These rules keep the robot safe:

1. **It never falls off the table.** If a MOVE would take the robot over the edge, it stays where it is. The next instructions still work as normal.
2. **It can't be placed off the table.** A placement like `PLACE 7,2,NORTH` is outside the grid, so it's ignored. If the robot was already on the table, it stays where it was.
3. **It must be placed before it can do anything.** Until there's a valid PLACE, the robot ignores MOVE, LEFT and RIGHT, and REPORT answers **`ROBOT MISSING`**.

An ignored instruction isn't an error: it's the robot following the rules. The answer always tells you which instructions were carried out and which were skipped, and why.

## A worked example

Instructions: `PLACE 1,2,EAST` → `MOVE` → `MOVE` → `LEFT` → `MOVE` → `REPORT`

| Step | Instruction | Robot is now at | Facing |
|---|---|---|---|
| 1 | PLACE 1,2,EAST | 1,2 | EAST → |
| 2 | MOVE | 2,2 | EAST → |
| 3 | MOVE | 3,2 | EAST → |
| 4 | LEFT | 3,2 (turned on the spot) | NORTH ↑ |
| 5 | MOVE | 3,3 | NORTH ↑ |
| 6 | REPORT | answers **`3,3,NORTH`** | |

More examples:

| Instructions | Answer | Why |
|---|---|---|
| `PLACE 0,0,NORTH` `MOVE` `REPORT` | `0,1,NORTH` | It moved one square up. |
| `PLACE 0,0,NORTH` `LEFT` `REPORT` | `0,0,WEST` | It turned but didn't move. |
| `MOVE` `REPORT` | `ROBOT MISSING` | It was never placed on the table. |
| `PLACE 0,4,NORTH` `MOVE` `REPORT` | `0,4,NORTH` | It's on the top edge, so the MOVE was skipped. |
| `PLACE 9,9,NORTH` `REPORT` | `ROBOT MISSING` | 9,9 isn't on the table, so it was never placed. |

## How to try it yourself (no coding needed)

The app runs on your own computer, and you control the robot from your web browser.

### Step 1: Install two free tools (one time only)

The app needs **Java** (the language it's written in) and **Maven** (the tool that builds and starts it).

- **Mac:** install [Homebrew](https://brew.sh), then open the **Terminal** app and run:
  ```bash
  brew install openjdk@21 maven
  ```
- **Windows:** install Java 21 from [Adoptium](https://adoptium.net) and Maven from [maven.apache.org](https://maven.apache.org/install.html).

### Step 2: Start the app

Open **Terminal** (on Windows, **Command Prompt**), go to the project folder, and start the app:

```bash
cd ~/Desktop/toy-robot
```
```bash
mvn spring-boot:run
```

The first start takes a minute or two while Maven downloads what it needs. The app is ready when you see a line ending in **`Started ToyRobotApplication`**. Leave this window open: closing it, or pressing **Ctrl+C**, stops the app.

### Step 3: Open the control page in your browser

Go to **http://localhost:8000/swagger-ui.html**

This page lists everything you can ask the app to do, and lets you send requests by clicking buttons.

### Step 4: Create a robot

1. Find **POST /robots: Create a robot** and click it.
2. Click **Try it out**, then **Execute**.
3. In the answer below, find the line `"id": "…"`, a long code like `3f2b6c1e-8a4d-…`. **Copy that id**: it's your robot's name, and you need it for the next steps.

### Step 5: Give it instructions

1. Find **POST /robots/{id}/commands: Run commands**, click it, then click **Try it out**.
2. Paste your robot's id into the **id** box.
3. In the request box, type your instructions in quotes, for example:
   ```json
   { "commands": "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT" }
   ```
4. Click **Execute**.

### Step 6: Read the answer

The app replies with something like this. The comments after `←` are explanations, not part of the reply:

```
"outputs": ["3,3,NORTH"]                                 ← what REPORT said
"results": [
  { "command": "PLACE 1,2,EAST", "status": "APPLIED" }   ← done
  { "command": "MOVE",           "status": "APPLIED" }   ← done
  ...
]
```

- **APPLIED** means the robot carried out the instruction.
- **IGNORED** means the robot safely skipped it. The **reason** says why:

| Reason | Meaning |
|---|---|
| `WOULD_FALL` | Moving would have taken the robot off the edge. |
| `OFF_TABLE` | The PLACE position isn't on the table. |
| `NOT_PLACED` | The robot hasn't been placed on the table yet. |

The robot remembers where it is between requests. You can send one instruction at a time, and it continues from where it was.

### If something goes wrong

If an instruction is mistyped, for example `JUMP` or `PLACE 1,2,UP`, the app **doesn't run any of the instructions in that request**, so the robot isn't left halfway through. You get a short message explaining what was wrong, like:

```json
{ "error": { "code": "INVALID_COMMAND", "message": "Unknown command \"JUMP\"" } }
```

Fix the typo and send it again.

| Message code | What it means | What to do |
|---|---|---|
| `INVALID_COMMAND` | An instruction isn't one of the five, or is mistyped. | Check the spelling. Commands are PLACE, MOVE, LEFT, RIGHT, REPORT. |
| `INVALID_PLACEMENT` | The position or direction is missing or mistyped. | Use whole numbers for X and Y, and NORTH, EAST, SOUTH or WEST. |
| `OFF_TABLE` | That square isn't on the 5 × 5 table. | Use X and Y between 0 and 4. |
| `ROBOT_NOT_FOUND` | The app doesn't know a robot with that id. | Check you pasted the whole id. Robots are forgotten when the app restarts. |
| `TOO_MANY_COMMANDS` | More than 10,000 instructions in one go. | Split them into smaller groups. |

## Other ways to use it

- **Postman:** import [`Toy-Robot-API.postman_collection.json`](Toy-Robot-API.postman_collection.json) to get every request ready to click, including the examples above with automatic checks.
- **Spec file:** [`openapi.yaml`](openapi.yaml) describes the whole app in a standard format that most API tools understand. It's useful for sharing with other teams.

## Words you might see

| Word | Plain meaning |
|---|---|
| **API** | A way for programs to talk to each other. Here, it's how you talk to the robot. |
| **Endpoint** | One specific thing you can ask the app to do, such as "create a robot". |
| **Request / response** | What you send to the app, and what it sends back. |
| **JSON** | A simple text format for data, using `{ }` and `"quotes"`. Both the instructions and the answers use it. |
| **Server** | The app, running and waiting for requests. |
| **localhost:8000** | "This computer, door number 8000": the address where the app listens. |
| **Swagger UI** | The control page in your browser that lists every endpoint and lets you try them. |
| **Status code** | A number in each answer: 200s mean success, 400s mean the request had a problem. |
| **Id** | The unique code naming each robot. |

## Common questions

**Can I have more than one robot?** Yes. Each one you create gets its own id and its own table, and they don't affect each other.

**Are my robots saved?** No. They're kept in memory and disappear when the app stops. That's fine for a simulation.

**Is anything shown on screen?** No, there's no graphics. The robot answers in text.

**Do commands have to be in capitals?** No, `move` and `MOVE` work the same.

**Can I break it?** We tried hard: 955 automated checks run against the app, including thousands of random and deliberately broken requests. The robot has never left the table.

---

# Part 2: For developers

## Tech stack and commands

Java 21+, Spring Boot 3.5, Maven. There's no database: state is held in memory.

```bash
mvn spring-boot:run                                   # start on http://localhost:8000 (override with PORT=...)
mvn test                                              # run all 955 tests
mvn package && java -jar target/toy-robot-1.0.0.jar   # build and run the jar
```

## Endpoints

| Method | Path | Purpose | Success | Errors |
|---|---|---|---|---|
| `POST` | `/robots` | Create an unplaced robot; `Location` header points at it | `201` | |
| `GET` | `/robots` | List all robots | `200` | |
| `GET` | `/robots/{id}` | Robot state and links | `200` | `404` |
| `DELETE` | `/robots/{id}` | Remove a robot | `204` | `404` |
| `PUT` | `/robots/{id}/placement` | **PLACE**, body `{"x":0,"y":0,"facing":"NORTH"}`; idempotent | `200` | `400`, `404`, `415`, `422` |
| `POST` | `/robots/{id}/commands` | Run commands in order | `200` | `400`, `404`, `413`, `415` |
| `GET` | `/robots/{id}/report` | **REPORT**, e.g. `{"output":"0,1,NORTH", ...}` | `200` | `404` |
| `GET` | `/actuator/health` | Health check, `{"status":"UP"}`; probes at `/actuator/health/liveness` and `/actuator/health/readiness` | `200` | |
| `GET` | `/actuator/info` | Application info | `200` | |

### `POST /robots/{id}/commands`

Accepts any of these bodies:

```json
{ "commands": "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT" }
{ "commands": ["PLACE 1,2,EAST", "MOVE", "MOVE", "LEFT", "MOVE", "REPORT"] }
{ "command": "MOVE" }
```

It also accepts a `text/plain` body, with commands separated by spaces or newlines. Commands are case-insensitive, and spaces around the commas in `PLACE` are allowed.

Each result is `APPLIED` or `IGNORED`, with a `reason` of `WOULD_FALL`, `OFF_TABLE` or `NOT_PLACED`. Ignored commands still return `200`, because ignoring them is the correct behaviour under the rules. The batch is parsed before anything runs, so a syntax error returns `400` and changes nothing. Coordinates of any size are valid syntax: `PLACE 99999999999,0,NORTH` is ignored as `OFF_TABLE`, not rejected.

```json
{
  "outputs": ["3,3,NORTH"],
  "results": [
    { "command": "PLACE 1,2,EAST", "status": "APPLIED" },
    { "command": "MOVE", "status": "APPLIED" },
    { "command": "REPORT", "status": "APPLIED", "output": "3,3,NORTH" }
  ],
  "robot": { "id": "…", "placed": true, "position": { "x": 3, "y": 3, "facing": "NORTH" }, "report": "3,3,NORTH" }
}
```

### Errors

Every error has the shape `{ "error": { "code": "...", "message": "..." } }`.

| Status | Codes | When |
|---|---|---|
| `400` | `INVALID_COMMAND`, `INVALID_PLACEMENT`, `INVALID_JSON` | Bad syntax, wrong types, malformed or trailing JSON |
| `404` | `ROBOT_NOT_FOUND`, `NOT_FOUND` | Unknown robot id or route |
| `405` | `METHOD_NOT_ALLOWED` | Wrong HTTP method; the `Allow` header lists the valid ones |
| `406` | (empty body) | The client's `Accept` header excludes JSON |
| `413` | `TOO_MANY_COMMANDS`, `PAYLOAD_TOO_LARGE` | Over 10,000 commands, or a body over 1 MB (including chunked bodies) |
| `415` | `UNSUPPORTED_MEDIA_TYPE` | Not `application/json`, or `text/plain` for commands |
| `422` | `OFF_TABLE` | `PUT /placement` outside the table; the robot is unchanged |

### curl examples

```bash
ID=$(curl -s -X POST localhost:8000/robots | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')

curl -s -X POST localhost:8000/robots/$ID/commands \
  -H 'Content-Type: application/json' \
  -d '{"commands":"PLACE 0,0,NORTH MOVE REPORT"}'          # outputs: ["0,1,NORTH"]

curl -s -X PUT localhost:8000/robots/$ID/placement \
  -H 'Content-Type: application/json' \
  -d '{"x":1,"y":2,"facing":"EAST"}'

curl -s -X POST localhost:8000/robots/$ID/commands \
  -H 'Content-Type: text/plain' --data-binary $'MOVE\nMOVE\nLEFT\nMOVE'

curl -s localhost:8000/robots/$ID/report                  # {"output":"3,3,NORTH",...}
```

## API documentation

The OpenAPI spec is generated from annotations on `RobotController`. Every endpoint has an operation id, a summary, its parameters, example request bodies, and an example response for each status code.

- **Spec file:** [`openapi.yaml`](openapi.yaml), OpenAPI 3.1, including the Actuator endpoints
- **Live spec:** http://localhost:8000/v3/api-docs (JSON) and http://localhost:8000/v3/api-docs.yaml (YAML)
- **Swagger UI:** http://localhost:8000/swagger-ui.html

`OpenApiSpecFileTest` fails if `openapi.yaml` stops matching the code or isn't valid OpenAPI. After changing an endpoint, regenerate it with:

```bash
mvn test -Dtest=OpenApiSpecFileTest -Dopenapi.update=true
```

## Logging

Every request gets an id. If the client sends a safe `X-Request-Id` (up to 64 letters, digits, `.`, `_` or `-`), it's reused; otherwise the server generates one, which stops clients forging log lines. The id is returned in the `X-Request-Id` response header and appears on every log line the request produces, in every layer:

| Layer | Class | Logs |
|---|---|---|
| Web | `RequestLoggingFilter` | `-->` request start; `<--` status and duration (INFO) |
| Web | `RequestSizeLimitFilter` | Oversized bodies rejected (WARN) |
| Controller | `RobotController` | Robot created, placed, deleted; result of each command batch (INFO); reads (DEBUG) |
| Domain | `Robot` | State before and after each batch (DEBUG) |
| Simulation | `Simulator` | Every command, applied (old → new state) or ignored and why (DEBUG) |
| Parsing | `CommandParser` | Commands parsed; rejected tokens (DEBUG) |
| Repository | `RobotRepository` | Store, find, list, remove (DEBUG) |
| Errors | `ApiExceptionHandler` | Every 4xx with its code (WARN); unexpected errors with stack trace (ERROR) |

INFO is the default, because one request can carry 10,000 commands. To trace every command:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--logging.level.com.example.toyrobot=DEBUG
```

## Tests

`mvn test` runs 955 tests:

| Suite | What it covers |
|---|---|
| `BrutalSimulationTest` | Every cell × every direction for PLACE, MOVE and rotation; 384 off-table placements; huge coordinates; 20,000 random sessions checked against an independent reference implementation; 31 malformed inputs; locale independence |
| `BrutalHttpTest` | Against real Tomcat: requirement scenarios, malformed bodies, content types, size limits, every wrong method, concurrency races (lost updates, batch atomicity, edge stops), and 2,000 fuzzed requests that must never return a 5xx |
| `ApiDocsTest`, `OpenApiSpecFileTest` | Every endpoint documented with examples for each status; `openapi.yaml` in sync and valid |
| `LoggingTest` | One request is logged by every layer under one request id; unsafe ids are replaced |
| `RobotApiTest`, `SimulatorTest`, `CommandParserTest`, `DirectionTest` | Core behaviour |

## Code structure

```
src/main/java/com/example/toyrobot/
├── constant/   AppConstants (paths, fields, commands, error codes, messages, log messages, limits)
│               ApiDocs (OpenAPI text and example payloads)
├── domain/     Direction, Position, Table: immutable values, no framework code
├── command/    Command (sealed interface), CommandParser, Simulator, CommandResult
└── api/        RobotController, Robot, RobotRepository (in-memory), request/response records,
                ApiExceptionHandler, RequestLoggingFilter, RequestSizeLimitFilter, OpenApiConfig
```

- **Rules:** pure functions on immutable values. `Simulator.execute` checks the target square before accepting a `PLACE` or `MOVE`, so the robot can never reach an invalid position. Bounds are checked on 64-bit coordinates before narrowing, so huge values can't wrap round onto the table.
- **Commands:** `Command` is a sealed interface, so the compiler checks that the `switch` in `Simulator` handles every command.
- **Concurrency:** `Robot.execute` is `synchronized`, so each batch runs as one unit, and the response is built from that batch's resulting state.
- **REST design:** `PUT /placement` is idempotent. `MOVE`, `LEFT` and `RIGHT` depend on the current state, so they go through `POST /commands`.
- **Constants:** no string literals outside `constant/`. Tests keep their literals on purpose, so an accidental change to a constant shows up as a change to the API's behaviour.
- **Storage:** in memory, lost on restart. A persistent `RobotRepository` could replace it without changing the controller.
