# MovieNight Jellyfin Plugin

Thin Jellyfin server plugin for bridging Jellyfin playback/sync signals to the MovieNight backend.

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

Sync requests use JSON with:

- `items`
- `items[].jellyfinItemId`
- `items[].title`
- `items[].description`
- `items[].year`
- `items[].genres`
- `items[].imdbId`
- `items[].userStates`
- `items[].userStates[].jellyfinUserId`
- `items[].userStates[].isViewed`
- `items[].userStates[].lastPlayedAt`
