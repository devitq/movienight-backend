using System;
using System.Collections.Generic;
using System.Text.Json.Serialization;

namespace Jellyfin.Plugin.MovieNight.Services;

/// <summary>
/// Event payload sent to MovieNight.
/// </summary>
/// <param name="EventId">Idempotency key.</param>
/// <param name="EventType">Event type.</param>
/// <param name="OccurredAt">Event timestamp.</param>
/// <param name="JellyfinUserId">Jellyfin user id.</param>
/// <param name="ItemId">Jellyfin item id.</param>
/// <param name="PayloadVersion">Payload version.</param>
/// <param name="Payload">Extra event data.</param>
public sealed record MovieNightEventPayload(
    [property: JsonPropertyName("event_id")]
    string EventId,
    [property: JsonPropertyName("event_type")]
    string EventType,
    [property: JsonPropertyName("occurred_at")]
    DateTimeOffset OccurredAt,
    [property: JsonPropertyName("jellyfin_user_id")]
    string JellyfinUserId,
    [property: JsonPropertyName("item_id")]
    string ItemId,
    [property: JsonPropertyName("payload_version")]
    int PayloadVersion,
    [property: JsonPropertyName("payload")]
    IReadOnlyDictionary<string, object?> Payload);
