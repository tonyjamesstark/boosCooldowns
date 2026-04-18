# boosCooldowns

Bukkit/Spigot/Paper plugin for per-command cooldowns, warmups, limits, prices
and custom command policies. Persists data to SQLite or MySQL/MariaDB so
cooldowns can be shared across a BungeeCord/Velocity network.

- **Per-command configuration** — cooldown, warmup, usage limit, price (Vault),
  point cost (PlayerPoints), XP cost, item cost.
- **Server-wide cooldowns** — one player triggers, everyone on all connected
  servers shares the cooldown window.
- **Progressive cooldowns** — multiplier that grows the cooldown on every
  consecutive use, resets after a quiet window.
- **Usage limits** with scheduled global resets (`/bcd scheduleglobalreset`).
- **Per-world & per-region restrictions** — command rules apply only in
  specific worlds or WorldGuard regions (optional).
- **Disable commands** without unregistering them — per group, with a
  `booscooldowns.nodisable` bypass permission.
- **Group inheritance** — `_inherits: default` in a group pulls down every
  command rule from its parent, keeping VIP configs short.
- **Per-command messages** — override cooldown / warmup / disabled messages
  on a per-command basis.
- **Action bar + Boss bar** for warmup countdowns and cooldown notices.
- **Warmup sounds & particles** per command.
- **Confirmation dialogs** — clickable Yes/No for commands with a cost.
- **Refund on warmup cancel** — opt-in return of charged money/items/XP when
  a warmup is aborted.
- **Localization (i18n)** — drop a `messages_<locale>.yml` and messages are
  served in the player's locale.
- **PlaceholderAPI** — exposes `%boos_remaining_<cmd>%`, `%boos_uses_<cmd>%`
  etc. and expands third-party placeholders inside plugin messages.
- **Command aliases** with argument substitution (`$1`, `$*`, `$player`,
  `$world`).
- **Sign shortcuts** — `[boosCooldowns]` signs that execute commands as the
  player or as console.
- **Event-based resets** — clear a player's cooldown when they kill a
  specific entity or earn an advancement, with an optional chat
  notification, sound cue or server-wide broadcast.
- **Namespace-aware** — the `/plugin:command` form is rewritten to the bare
  command so cooldowns still apply, no need to keep the old hard syntax
  blocker turned on.
- **Audit log** — every command decision optionally persisted to
  `command_audit` for forensic review via `/bcd audit`.
- **Admin tooling** — `/bcd info`, `/bcd grant`, `/bcd tempset`, `/bcd
  validate`, `/bcd audit`.
- **Shared database** — set both servers to the same MySQL/MariaDB and
  cooldowns/limits/server-cooldowns are shared automatically.
- **bStats metrics** for global insight.

## Requirements

| Requirement | Version |
| --- | --- |
| Java | 21 |
| Paper / Spigot / Bukkit | 1.20+ (API) — tested on Paper 1.21.8 |
| Vault | Optional — only needed for money prices |
| PlayerPoints | Optional — only needed for point prices |
| PlaceholderAPI | Optional — enables `%boos_*%` placeholders and expansion of third-party placeholders inside plugin messages |
| WorldGuard | Optional — enables the `regions:` restriction key |

SQLite and MySQL JDBC drivers are loaded at runtime through Paper's
`libraries` mechanism — no separate installation required.

## Installation

1. Download `boosCooldowns.jar` from the Releases page.
2. Drop it into your `plugins/` directory.
3. Start the server once. A default `config.yml` is generated in
   `plugins/boosCooldowns/`.
4. Edit `config.yml` and `/booscooldowns reload` (or restart).

If you're upgrading from an older version, the plugin will detect `users.yml`
on first start and migrate its contents into the database automatically. The
old file is renamed to `users.yml.migrated-<timestamp>`.

## Configuration

### Database

```yaml
database:
  type: sqlite              # sqlite (default) or mysql / mariadb
  # host: localhost
  # port: 3306
  # database: boosCooldowns
  # username: user
  # password: secret
  # jdbc-url: jdbc:mysql://localhost:3306/boosCooldowns?useSSL=false
  pool-size: 4
  connection-timeout-ms: 10000
  cache-ttl-seconds: 30     # in-memory cache TTL before re-querying DB
```

For **shared cooldowns across multiple servers**, point every server at the
same MySQL/MariaDB instance. With the default 30 s cache TTL, writes are
visible on other servers within the TTL window. No message broker (Redis etc.)
is required.

### Multi-server configuration

Player state (cooldowns, limits, user preferences, audit log) lives in the
shared DB and is synchronized automatically. **Command rules** — `/kit`
cooldown, prices, aliases, options — still live in each server's
`config.yml`, so you need to keep those files in sync manually (git, rsync,
symlink, or your deploy pipeline).

To help you catch drift, the plugin hashes each node's `config.yml` on
enable and on `/bcd reload` and writes it into a `config_version` table.
Every `options.options.config_sync_interval_seconds` (default 60) each node
scans peer hashes and, if they disagree, logs:

```
[boosCooldowns] Config drift detected across nodes:
  survival-1  hash=ab3c…  reloaded=2026-04-19T10:12:34Z (this server)
  survival-2  hash=98df…  reloaded=2026-04-19T09:48:11Z
[boosCooldowns] Edit config.yml on every node and run /bcd reload, or run /bcd diff to inspect the mismatch.
```

Admins can inspect the state any time with `/bcd diff`. Assign each server
a human-readable label via `options.options.node_id` (e.g. `survival-1`) —
otherwise the plugin defaults to `hostname:port`.

`/bcd set` and `/bcd tempset` detect a multi-node setup and warn that the
change only lands on the current server.

### Per-command rules

```yaml
commands:
  groups:
    default:
      /kit:
        cooldown: "1 hour"
        warmup: 5                         # seconds
        limit: 3
        limit_reset_delay: "6 hours"
        price: 100.0                      # Vault money
        xpcost: 5
        playerpoints: 10
        itemcost:
          item: DIAMOND
          count: 1
          name: "Kit Token"               # optional lore/enchants
        server_cooldown: "30 minutes"     # shared across all players

        # Progressive cooldown — 2nd use doubles, 3rd quadruples…
        cooldown_multiplier: 2.0
        cooldown_reset_after: "6 hours"   # streak resets after this much idle time

        # Per-world / per-region restrictions (rule only applies if the player
        # is inside one of these worlds/regions — otherwise command passes through)
        worlds: [survival, nether]
        regions: [spawn, arena]           # requires WorldGuard

        # Effects (played on the player)
        warmup_sound: BLOCK_BEACON_AMBIENT
        complete_sound: ENTITY_EXPERIENCE_ORB_PICKUP
        warmup_particle: ENCHANT

        # Access control
        disabled: false
        disabled_message: "&cKit is temporarily offline."
        cooldown_message: "&7Kit is recharging, &e&seconds&&7 left."
        warmup_message: "&7Preparing kit... &e&seconds&&7."
        permission: "myplugin.kit"
        denied_message: "&cNot allowed."
        shared_cooldown: [/kit_daily]
        shared_limit: [/kit_daily]

    vip:
      _inherits: default                  # vip inherits every /kit option above…
      /kit:
        cooldown: "10 minutes"            # …except this one gets a shorter cooldown

  aliases:
    /ja: /me             # exact match
    /tell *: /msg $1 $*  # wildcard with argument substitution

# Reset a player's cooldown when a specific in-world event happens.
event_triggers:
  - when: KILL
    entity: ENDER_DRAGON
    reset_cooldown: /boss_reward
    message: "&6You slew the dragon! &e/boss_reward&6 is ready."
    sound: ENTITY_PLAYER_LEVELUP
    broadcast: true           # announce to every online player (default: false)
  - when: ADVANCEMENT
    advancement: story/mine_diamond
    reset_cooldown: /miner_reward
    message: "&a/miner_reward cooldown has been reset."
```

Message placeholders: `&command&` (the command key), `&player&`,
`&entity&` (KILL triggers), `&advancement&` (ADVANCEMENT triggers).

Cooldowns, warmups and reset delays accept friendly durations (`"30 seconds"`,
`"5 minutes"`, `"2 hours"`, `"1 day"`) or a plain integer (interpreted as
seconds).

### Options

```yaml
options:
  options:
    disabled_for_ops: true              # plugin has no effect on ops
    warmups_enabled: true
    cooldowns_enabled: true
    limits_enabled: true
    prices_enabled: true
    cancel_warmup_on_move: false
    cancel_warmup_on_damage: false
    cancel_warmup_on_sneak: false
    cancel_warmup_on_sprint: false
    cancel_warmup_on_gamemode_change: false
    block_interact_during_warmup: false
    clear_cooldowns_on_death: false
    clear_uses_on_death: false
    start_cooldowns_on_death: false
    command_logging: false
    command_signs: false
    syntax_blocker_enabled: true        # reject /pluginname:command outright
    # when the blocker is off, rewrite /pluginname:cmd → /cmd so rules still apply
    apply_rules_to_prefixed_syntax: true
    command_confirmation: true          # ask Yes/No for priced commands
    refund_on_warmup_cancel: false      # return charged price when warmup aborted
    audit_enabled: false                # persist every command decision to DB
    audit_retention_days: 30
    default_locale: en                  # fallback for messages_<lang>.yml
    debug: false
  action_bar:
    warmup: false
    cooldown_on_attempt: false
    update_interval_ticks: 10
  boss_bar:
    warmup: false
    color: BLUE                         # BLUE, GREEN, PINK, PURPLE, RED, WHITE, YELLOW
    style: PROGRESS                     # PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20
```

### Namespace-prefixed commands

Minecraft lets players bypass single-command rules by typing
`/minecraft:tp` instead of `/tp`. There are two ways to deal with that:

- **`syntax_blocker_enabled: true`** (the default) — the plugin rejects any
  namespace-prefixed command outright with `invalid_command_syntax`. Admins
  using this hate the "blocked" message whenever they type `/paper:version`.
- **`syntax_blocker_enabled: false` + `apply_rules_to_prefixed_syntax: true`**
  — the plugin transparently rewrites `/plugin:cmd` to `/cmd` before any
  rule lookup. Cooldowns, warmups, prices and limits all apply as if the
  player typed the bare form. Players keep the shortcut, admins keep
  control.

The permission `booscooldowns.syntaxblockerexception` still lets specific
users bypass the hard blocker when it is enabled.

### Localization

Drop a `messages_<locale>.yml` into `plugins/boosCooldowns/` (or bundle one
as a JAR resource). The plugin consults the file matching the player's
client locale; unknown keys fall back to `default_locale`, then to the main
`config.yml`. Example `messages_cs.yml` ships with the JAR.

## Commands

All subcommands live under `/booscooldowns` (alias `/bcd`). Type
`/bcd help` for a permission-filtered, auto-generated listing, or
`/bcd help <section>` to expand a group (e.g. `/bcd help clear`).

**Player commands** (default: allowed):

| Command | Purpose |
| --- | --- |
| `/bcd status [player]` | Full overview — cooldowns, limits, active warmup. No arg = self, arg requires `booscooldowns.status.others` |
| `/bcd check <command>` | Is this command on cooldown for me? |
| `/bcd confirmations` | Toggle the confirmation dialog on/off for yourself |

**Administration** (default: op):

| Command | Purpose |
| --- | --- |
| `/bcd reload` | Reload `config.yml` and re-register listeners |
| `/bcd clear cooldowns <player> [command]` | Clear active cooldowns |
| `/bcd clear uses <player> [command]` | Reset usage counters |
| `/bcd clear warmups <player>` | Cancel all active warmups |
| `/bcd clear all <player>` | Wipe cooldowns + uses + warmups in one call |
| `/bcd rule set <option> <command> <value> [group]` | Persist a rule override to `config.yml` |
| `/bcd rule tempset <option> <command> <value> <duration>` | Temporary override — auto-restores after the duration |
| `/bcd rule get <command> [group]` | Print the effective rule set for a command (after group inheritance) |
| `/bcd grant cooldown <player> <command> <duration>` | Manually seed a cooldown |
| `/bcd grant uses <player> <command> <count>` | Overwrite a player's remaining uses |
| `/bcd reset now <command>` | Wipe the usage counter for a command across all players immediately |
| `/bcd reset schedule <command> <time>` | Schedule a global reset (`+2h`, `yyyy-MM-dd HH:mm:ss`) |
| `/bcd reset list` | Show pending scheduled resets |

**Diagnostics** (default: op):

| Command | Purpose |
| --- | --- |
| `/bcd validate` | Lint `config.yml` for typos, bad durations, unknown materials/sounds |
| `/bcd audit [command] [limit]` | Read the `command_audit` log |
| `/bcd diff` | Compare config hash with other nodes sharing the same DB |
| `/bcd help [section]` | Autogenerated help |

**Legacy aliases** — the pre-4.0 flat layout still works for backward
compatibility. Macros, scripts and tab-completers don't break on upgrade:

| Legacy | New |
| --- | --- |
| `/bcd clearcooldowns`, `clearuses`, `clearwarmups` | `/bcd clear cooldowns`, `clear uses`, `clear warmups` |
| `/bcd set`, `tempset` | `/bcd rule set`, `rule tempset` |
| `/bcd scheduleglobalreset`, `listresets`, `startglobalreset` | `/bcd reset schedule`, `reset list`, `reset now` |
| `/bcd info`, `limits`, `checkcooldown` | `/bcd status [player]`, `/bcd status`, `/bcd check` |

**Tab completion** is wired up end-to-end: `/bcd <Tab>` lists visible
subcommands, `/bcd clear <Tab>` expands to `cooldowns|uses|warmups|all`,
`/bcd clear cooldowns <Tab>` suggests online players, `/bcd rule set <Tab>`
suggests valid option keys (`cooldown`, `warmup`, `limit`, …).

## Permissions

| Permission | Effect |
| --- | --- |
| `booscooldowns.exception` | Bypass the plugin entirely |
| `booscooldowns.norestriction` | Bypass warmups/cooldowns/prices/limits (aliases still apply) |
| `booscooldowns.nocooldown[./command]` | Bypass cooldowns (optionally per command) |
| `booscooldowns.nowarmup[./command]` | Bypass warmups |
| `booscooldowns.nolimit[./command]` | Bypass limits |
| `booscooldowns.noprice[./command]` | Bypass Vault money cost |
| `booscooldowns.noxpcost[./command]` | Bypass XP cost |
| `booscooldowns.noitemcost[./command]` | Bypass item cost |
| `booscooldowns.noplayerpoints[./command]` | Bypass PlayerPoints cost |
| `booscooldowns.nodisable[./command]` | Use commands flagged `disabled: true` |
| `booscooldowns.noactionbar` | Suppress action-bar notifications |
| `booscooldowns.nocancel.{move,damage,sneak,sprint,gamemodechange}` | Do not cancel warmup on that event |
| `booscooldowns.dontblock.interact` | Interact with blocks/containers during warmup |
| `booscooldowns.signs.{player,server}.{use,place}` | Permissions for `[boosCooldowns]` signs |
| `booscooldowns.<group-name>` | Place the player in a non-default command group |

Full list with defaults is in [plugin/src/main/resources/plugin.yml](plugin/src/main/resources/plugin.yml).

## PlaceholderAPI

When [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) is on the
server, the plugin registers a `boos` expansion:

| Placeholder | Value |
| --- | --- |
| `%boos_remaining_<cmd>%` | Formatted remaining cooldown (`"2h 15m"`) |
| `%boos_remaining_seconds_<cmd>%` | Raw integer seconds |
| `%boos_on_cooldown_<cmd>%` | `true` / `false` |
| `%boos_server_cooldown_<cmd>%` | Server-wide cooldown remaining |
| `%boos_uses_<cmd>%` | `"remaining/limit"` (or `∞` if no limit) |
| `%boos_uses_remaining_<cmd>%` | Raw integer remaining uses |

Use underscores instead of spaces in the command name (e.g.
`%boos_remaining_daily_claim%` → `/daily claim`). Leading `/` is optional.

You can also embed foreign placeholders inside plugin messages — for
example `cooldown_message: "Wait &e%remaining% &7(balance: &a%vault_eco_balance%&7)"`
will be expanded per-player on every delivery.

## API

Other plugins can drive the service through `BoosCooldownAPI`:

```java
BoosCooldownAPI api = BoosCooldownAPI.getInstance();
CommandData data = api.buildCommandData(player, "/kit");
if (data != null && !api.isOnCooldown(player, data)) {
    api.setCooldown(player, data); // returns CompletableFuture<Void>
}
```

Consumers should declare `loadbefore: [boosCooldowns]` in their `plugin.yml`
if they reference `BoosCooldownAPI` at class-load time.

Three custom Bukkit events are also exposed for listener-style integration:
`CooldownEvent`, `WarmupEvent`, `CommandEvent` (all cancellable).

## Architecture

```
software.boos.boosCooldown/
├── persistence/    # HikariCP + JDBC repositories (cooldowns, limits, prefs)
│   ├── repository/ # interfaces + JDBC implementations (SQLite / MySQL)
│   └── migration/  # YAML → JDBC one-shot migrator
├── service/        # Business logic — CooldownService, WarmupService, LimitService,
│                   # PriceService, AliasService, ConfirmationService, ActionBarService,
│                   # ScheduledResetService
├── economy/        # EconomyProvider + Vault / PlayerPoints / XP / Items adapters
├── listener/       # CommandPreprocessListener (main pipeline), WarmupCancelListener,
│                   # PlayerConnectionListener (cache prefetch), SignListeners
├── command/        # CommandHandler + subcommand package
├── model/          # records: CommandData, CooldownEntry, LimitEntry, ItemCost, …
├── config/         # PluginConfig, MessageConfig, DatabaseConfig
└── event/          # Custom Bukkit events for the public API
```

Every DB write is off the main thread through a single-thread `ExecutorService`.
The main-thread hot-path (`PlayerCommandPreprocessEvent`) only touches an
in-memory cache, which is pre-warmed at `PlayerJoinEvent`.

## Building from source

```
mvn clean package
```

Produces `jar/target/boosCooldowns.jar` with HikariCP and bStats shaded and
relocated. Requires Java 21.

## Testing

```
mvn -pl plugin test
```

113 tests:

- **Utility** — `CommandKey`, `TimeFormatter`, `CachingService`
- **Persistence** — all 5 JDBC repositories + `SchemaMigrator` + YAML migrator,
  each using an isolated SQLite file per test
- **Services** — `CooldownServiceImpl`, `LimitServiceImpl`, `AliasService`,
  `ConfirmationService`
- **Integration (MockBukkit)** — plugin lifecycle, command pipeline (cooldown,
  disable, limit, server cooldown, permissions, `exception` bypass), sub-command
  router (10 subcommands), warmup-cancel listeners (5 cancel causes + bypass
  permission), confirmation flow (dialog, toggle, persistence across reload)

## Benchmarks

A JMH micro-benchmark covers the hot-path cooldown lookup:

```
mvn -pl plugin test-compile
java -cp "plugin/target/test-classes:plugin/target/classes:$(mvn -pl plugin dependency:build-classpath -q -DincludeScope=test -Dmdep.outputFile=/dev/stdout)" \
     software.boos.boosCooldown.benchmark.CooldownPipelineBenchmark
```

Typical results on a modern laptop: **~200 ns** for cache hits,
**~150 µs** for cache misses that hit SQLite.

## CI

GitHub Actions runs on every PR:

- [.github/workflows/ci.yml](.github/workflows/ci.yml) — Java 21 matrix, full
  test suite, surefire reports uploaded on failure, shaded JAR as artifact.
- [.github/workflows/maven-build.yml](.github/workflows/maven-build.yml) —
  tagged releases published automatically on push to `master`.

## License

[MIT](LICENSE) — copyright © 2011–2026 LordBoos (boosik) and contributors.
Contributions welcome via pull requests.

## Credits

Original plugin by **LordBoos** (boosik). Refactored to service/repository
architecture with JDBC persistence, cross-server cooldowns, confirmation
dialogs, action bars and scheduled resets.
