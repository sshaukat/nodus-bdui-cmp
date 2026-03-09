# bduiRuntime

`bduiRuntime` is the reusable Compose Multiplatform runtime module for Nodus BDUI sandbox flows.

## Responsibilities

1. accept raw backend-driven JSON schema payloads;
2. parse and validate them into an internal node tree;
3. create a render session with form state and action dispatching;
4. expose a single `BduiScreen` composable entry point for host applications.

## Public API

Main entry points live under `dev.nodus.bdui.runtime.api`:

- `BduiRuntimeEngine`
- `BduiSchemaPayload`
- `BduiRegistryContext`
- `BduiParseResult`
- `BduiRenderSession`
- `BduiActionDispatcher`
- `BduiScreen`

## Current support matrix

Supported nodes:

1. `column`
2. `row`
3. `box`
4. `text`
5. `button`
6. `input`
7. `spacer`

Supported actions:

1. `log`
2. `open_url`
3. `navigate`

Unsupported nodes currently produce diagnostics and fallback rendering.

## Host boundary

The host app is responsible for:

1. fetching schema payloads from backend or fake data source;
2. navigation between selector and render screens;
3. platform side effects such as URI opening or back navigation.

The runtime module is responsible for:

1. schema decode/validate;
2. runtime state for rendered forms;
3. action propagation to the host boundary;
4. node-to-Compose rendering.
