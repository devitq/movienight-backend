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
- `GET /api/integrations/jellyfin/users/{jellyfin_user_id}/recommendations`
- `POST /api/integrations/jellyfin/users/{jellyfin_user_id}/ratings/items/{jellyfin_item_id}`
- `POST /api/integrations/jellyfin/users/{jellyfin_user_id}/library/items/{jellyfin_item_id}/viewed`
- `GET /api/integrations/jellyfin/users/{jellyfin_user_id}/preferences`
- `POST /api/integrations/jellyfin/users/{jellyfin_user_id}/recommendation-onboarding`

Configure the backend with:

- `JELLYFIN_INTEGRATION_ENABLED=true`
- `JELLYFIN_PLUGIN_TOKEN=<same token configured in the plugin>`
- `JELLYFIN_WEB_URL=<browser URL of Jellyfin, used for recommendation watch links>`

`JELLYFIN_SYNC_ENABLED=true` is still accepted as a legacy alias for `JELLYFIN_INTEGRATION_ENABLED=true`.

Optional backend-pull sync values:

- `JELLYFIN_BASE_URL=<backend-reachable Jellyfin server URL>`
- `JELLYFIN_API_KEY=<Jellyfin API key>`

The Jellyfin API key is only for backend-to-Jellyfin calls. The plugin token is a MovieNight shared secret for plugin-to-backend calls.

Configure the plugin with:

- Backend URL: MovieNight backend URL reachable from the Jellyfin server, for example `http://movienight-backend:8080`
- Plugin token: the exact `JELLYFIN_PLUGIN_TOKEN` value
- Enable MovieNight integration: checked
- Enable periodic backend sync: checked if the plugin should push library state on an interval
- Send playback stop events: checked if completed playback should mark films viewed in MovieNight

Event requests use JSON with:

- `event_id`
- `event_type`
- `occurred_at`
- `jellyfin_user_id`
- `item_id`
- `payload_version`
- `payload`

The plugin sends `X-MovieNight-Plugin-Token` when configured. Completed Jellyfin playback stop events are sent as `playback.stopped`. Transient event push failures are retried three times with the same `event_id`.

Sync requests push Jellyfin users, items, and per-user watched states to the backend. The backend creates MovieNight users for new Jellyfin users using their Jellyfin id as the stable mapping key, upserts films by `jellyfinItemId`, and uses the Jellyfin-facing endpoints above for UI actions so Jellyfin ids do not have to match MovieNight UUIDs. Run "Sync Library" once after installing/configuring the plugin so recommendations, rating, and viewed actions can resolve Jellyfin items.

The config page test action posts a small `plugin.test` event to `/api/integrations/jellyfin/events` and treats `200` as success and `401` as token/config failure.
