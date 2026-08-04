# Supabase Backend Agent Prompt: Tangle Crew RuneLite LFG

You are implementing the backend for the existing RuneLite client plugin in `ZH-Systems/TangleDinkPlugin`.

The RuneLite client-side contract is already implemented. Do not change the plugin contract unless you discover and document a blocking technical issue.

Do not modify the RuneLite repository. Do not create plugin code. Build the Supabase backend only.

## Goal

Implement a Supabase-backed Looking For Group system that supports:

- listing enabled categories
- listing active groups
- creating a group
- joining a group
- leaving a group
- closing a group
- syncing state between RuneLite and Discord

The backend is authoritative for group state.

The RuneLite client treats Discord and RuneLite as projections of that source of truth.

## Client contract

The RuneLite client calls these endpoints:

- `GET /functions/v1/lfg-config`
- `GET /functions/v1/lfg-groups`
- `POST /functions/v1/lfg-groups`
- `POST /functions/v1/lfg-group-action`

### Required request headers

The client sends these headers:

- `Authorization: Bearer <restricted plugin token>`
- `Content-Type: application/json`
- `X-TcCrew-Player: <normalized player identity>`
- `X-Idempotency-Key: <unique write request id>` for writes
- `X-Plugin-Version: <plugin version>`

The `X-TcCrew-Player` value is produced client-side from the local RuneScape player name and RuneLite profile type:

- username is trimmed
- non-breaking spaces are normalized to spaces
- profile type is appended as `username|PROFILETYPE` when present

Treat that header as a client identity hint, not as authorization.

## Request models

### `GET /functions/v1/lfg-config`

Returns enabled categories for the current session.

Response model:

```json
{
  "categories": [
    {
      "id": "uuid",
      "key": "raid",
      "displayName": "Raid",
      "description": "Raid groups",
      "enabled": true,
      "displayOrder": 10
    }
  ],
  "message": "optional status message"
}
```

### `GET /functions/v1/lfg-groups`

Returns active groups visible to the caller.

Response model:

```json
{
  "groups": [
    {
      "id": "uuid",
      "version": 4,
      "category": {
        "id": "uuid",
        "key": "raid",
        "displayName": "Raid",
        "description": "Raid groups",
        "enabled": true,
        "displayOrder": 10
      },
      "activity": "Theatre of Blood",
      "description": "Learner friendly",
      "startTime": "2026-08-04T20:00:00Z",
      "maximumPlayers": 5,
      "status": "OPEN",
      "source": "RUNELITE",
      "creator": {
        "playerId": "player-id",
        "rsn": "Example Player",
        "discordUserId": null,
        "source": "RUNELITE",
        "joinedAt": "2026-08-04T19:30:00Z"
      },
      "members": [
        {
          "playerId": "player-id",
          "rsn": "Example Player",
          "discordUserId": null,
          "source": "RUNELITE",
          "joinedAt": "2026-08-04T19:30:00Z"
        }
      ],
      "permissions": {
        "canJoin": false,
        "canLeave": true,
        "canClose": true
      },
      "discordMessageId": "message-id",
      "createdAt": "2026-08-04T19:30:00Z",
      "updatedAt": "2026-08-04T19:32:00Z",
      "expiresAt": "2026-08-04T23:00:00Z"
    }
  ],
  "message": "optional status message"
}
```

### `POST /functions/v1/lfg-groups`

Creates a group.

Request model:

```json
{
  "categoryKey": "raid",
  "activity": "Theatre of Blood",
  "description": "Learner friendly",
  "startTime": "2026-08-04T20:00:00Z",
  "maximumPlayers": 5
}
```

Notes:

- `maximumPlayers` is nullable
- `null` means unlimited / mass group
- zero is not a valid unlimited sentinel
- `startTime` may be omitted or null when the client chooses immediate/now

### `POST /functions/v1/lfg-group-action`

Performs a join, leave, or close action.

Request model:

```json
{
  "action": "join",
  "groupId": "group-uuid",
  "idempotencyKey": "request-id"
}
```

Supported actions:

- `join`
- `leave`
- `close`

## Response models

### Action response

The client expects:

```json
{
  "success": true,
  "message": "Joined group",
  "group": { },
  "error": null
}
```

or on failure:

```json
{
  "success": false,
  "message": "Unable to join group",
  "group": null,
  "error": {
    "code": "GROUP_FULL",
    "message": "Group is full",
    "details": "optional extra context"
  }
}
```

### Error model

Use this shape for structured errors:

```json
{
  "code": "string",
  "message": "string",
  "details": "string|null"
}
```

Keep `message` safe for local RuneLite chat. Do not include secrets.

## Canonical enums

### Group status

Use exactly:

```text
OPEN
FULL
STARTED
CLOSED
CANCELLED
EXPIRED
```

### Source

Use exactly:

```text
RUNELITE
DISCORD
ADMIN
```

## Category contract

The backend is the canonical category source.

The RuneLite client:

- fetches categories from `lfg-config`
- sorts by `displayOrder`
- applies a client-side allowlist from `LFG Settings -> Visible Categories`
- ignores unknown category keys
- matches keys case-insensitively
- shows only enabled categories

Initial category families may include:

- Boss
- Raid
- Skilling
- Minigame
- Other

Do not hardcode those as the only supported values.

Persist category rows with:

- `id`
- `key`
- `displayName`
- `description`
- `enabled`
- `displayOrder`

## Group contract

Persist groups with:

- `id`
- `version`
- `category`
- `activity`
- `description`
- `startTime`
- `maximumPlayers`
- `status`
- `source`
- `creator`
- `members`
- `permissions`
- `discordMessageId`
- `createdAt`
- `updatedAt`
- `expiresAt`

The `version` field is used for stale-response rejection and optimistic concurrency.

The `permissions` object must contain:

- `canJoin`
- `canLeave`
- `canClose`

The client uses server-provided permission flags; do not infer permissions from local heuristics alone.

## Database and concurrency requirements

Implement Supabase storage so that:

- category rows are canonical and ordered
- group writes are transactional
- concurrent joins/leaves/closes do not corrupt membership
- group version increments on each meaningful update
- membership changes are idempotent when the same request is retried
- stale writes are rejected or safely no-op'd
- expired groups transition cleanly to `EXPIRED`

Use database transactions or equivalent single-source logic to avoid double-joins, double-leaves, or race conditions during close/update flows.

## Idempotency

Writes from the RuneLite client include `X-Idempotency-Key`.

Use it for:

- create
- join
- leave
- close

The same request key must not create duplicate side effects.

## Discord integration requirements

Implement the backend Discord side separately from the RuneLite plugin.

Requirements:

- Discord-created groups must appear in RuneLite
- RuneLite-created groups must appear in Discord
- joins from either side must converge
- leaves from either side must converge
- closes from either side must converge

Do not reconstruct group state by parsing Discord message text.

Use the database as the source of truth and update Discord message state from backend events.

### Discord slash/interactions

Implement Discord interactions with:

- signature verification
- safe allowed-mentions restrictions
- create/join/leave/close actions
- message update flows for state changes
- per-group message tracking using `discordMessageId`

### Account linking

Design a backend-side account linking flow between Discord users and RuneScape player identities.

Do not use Discord display names as proof of identity.

## Delivery queue and retry requirements

Use a delivery queue for outbound Discord updates and any other asynchronous side effects.

Requirements:

- retry transient failures
- back off safely
- avoid duplicate Discord posts
- preserve idempotency across retries
- record failed deliveries for inspection

## Expiration and cleanup

Groups must expire according to backend policy.

Requirements:

- expired groups eventually become `EXPIRED`
- closed groups are not joinable
- cancelled groups are not joinable
- cleanup jobs must not delete active groups prematurely
- stale Discord messages should be updated or marked inactive

## Realtime

If you use Supabase Realtime:

- document the exact channels
- ensure the client can still function from periodic refresh alone
- do not make the RuneLite plugin depend on Realtime for correctness unless documented

## Security requirements

- no service-role key in the client
- no Discord bot token in the client
- no unrestricted backend credentials in the client
- no secret values in logs
- no secret values in validation errors
- no allowed-mentions leaks in Discord messages

## Backend tests

Add tests for:

- category list response shape
- group list response shape
- create/join/leave/close success paths
- idempotency key reuse
- optimistic concurrency/version checks
- duplicate join/leave protection
- expired group handling
- Discord interaction signature verification
- allowed-mentions restrictions
- delivery retry logic
- account-linking flow

## Deployment steps

Document:

- Supabase function deployment
- database migrations
- secret/environment setup
- Discord bot deployment
- any Realtime setup
- any webhook configuration needed for master-channel posting

## Manual acceptance tests

Verify end-to-end:

1. RuneLite loads categories and groups from Supabase.
2. A RuneLite-created group appears in Discord.
3. A Discord-created group appears in RuneLite.
4. Join/leave/close from either side converges after refresh.
5. Full groups are reported correctly.
6. Closed and cancelled groups are not joinable.
7. Expired groups transition correctly.

## Important constraint

Do not modify the RuneLite plugin contract unless you discover a blocking issue. If that happens, report it with the exact API shape that must change and why.
