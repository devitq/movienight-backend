# MovieNight Jellyfin Plugin

Thin Jellyfin server plugin for bridging Jellyfin playback/sync signals to the MovieNight backend.

## Instructions

Brief instructions on how to integrate this plugin to Jellyfin.

1. Install this plugin
2. Setup plugin in plugin settings
2. Install [JavaScript Inejector plugin](https://github.com/n00bcodr/Jellyfin-JavaScript-Injector)
3. Add [ui.js](./Jellyfin.Plugin.MovieNight/Configuration/ui.js) file to JavaScript Injector
5. You're all set! (i hope)

## Build

```bash
cd plugins/jellyfin/Jellyfin.Plugin.MovieNight
dotnet publish -c Release
```

Install the published `net9.0` plugin files into the Jellyfin data directory under `plugins/MovieNight/`, then restart Jellyfin. This build targets Jellyfin `10.11.x`.

## Backend Contract Used

Current implemented calls:

- `POST /api/integrations/jellyfin/sync`
- `GET /api/integrations/jellyfin/sync-state`
- `POST /api/integrations/jellyfin/events`

Event requests use JSON with:

- `event_id`
- `event_type`
- `occurred_at`
- `jellyfin_user_id`
- `item_id`
- `payload_version`
- `payload`

The plugin sends `X-MovieNight-Plugin-Token` when configured. Completed Jellyfin playback stop events are sent as `playback.stopped`. Transient event push failures are retried three times with the same `event_id`.

The config page test action posts a small synthetic event to `/api/integrations/jellyfin/events` and treats `200` as success and `401` as token/config failure.
