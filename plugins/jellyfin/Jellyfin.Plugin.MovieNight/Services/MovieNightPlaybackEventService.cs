using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Controller.Library;
using MediaBrowser.Controller.Session;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Jellyfin.Plugin.MovieNight.Services;

/// <summary>
/// Subscribes to Jellyfin playback events and forwards thin payloads.
/// </summary>
public sealed class MovieNightPlaybackEventService : IHostedService
{
    private readonly ISessionManager _sessionManager;
    private readonly MovieNightBackendClient _backendClient;
    private readonly ILogger<MovieNightPlaybackEventService> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightPlaybackEventService"/> class.
    /// </summary>
    /// <param name="sessionManager">Jellyfin session manager.</param>
    /// <param name="backendClient">Backend client.</param>
    /// <param name="logger">Logger.</param>
    public MovieNightPlaybackEventService(
        ISessionManager sessionManager,
        MovieNightBackendClient backendClient,
        ILogger<MovieNightPlaybackEventService> logger)
    {
        _sessionManager = sessionManager;
        _backendClient = backendClient;
        _logger = logger;
    }

    /// <inheritdoc />
    public Task StartAsync(CancellationToken cancellationToken)
    {
        _sessionManager.PlaybackStopped += OnPlaybackStopped;
        return Task.CompletedTask;
    }

    /// <inheritdoc />
    public Task StopAsync(CancellationToken cancellationToken)
    {
        _sessionManager.PlaybackStopped -= OnPlaybackStopped;
        return Task.CompletedTask;
    }

    private void OnPlaybackStopped(object? sender, PlaybackStopEventArgs e)
    {
        if (Plugin.Instance?.Configuration is not { Enabled: true, EnablePlaybackEvents: true })
        {
            return;
        }

        if (!e.PlayedToCompletion)
        {
            return;
        }

        var userId = e.Users?.FirstOrDefault()?.Id.ToString("N");
        var itemId = e.Item?.Id.ToString("N");
        if (string.IsNullOrWhiteSpace(userId) || string.IsNullOrWhiteSpace(itemId))
        {
            return;
        }

        var occurredAt = DateTimeOffset.UtcNow;
        var eventId = string.IsNullOrWhiteSpace(e.PlaySessionId)
            ? $"playback-stopped:{userId}:{itemId}:{occurredAt.ToUnixTimeMilliseconds()}"
            : $"playback-stopped:{userId}:{itemId}:{e.PlaySessionId}";

        var payload = new MovieNightEventPayload(
            EventId: eventId,
            EventType: "playback.stopped",
            OccurredAt: occurredAt,
            JellyfinUserId: userId,
            ItemId: itemId,
            PayloadVersion: 1,
            Payload: new Dictionary<string, object?>
            {
                ["itemName"] = e.Item?.Name,
                ["playSessionId"] = e.PlaySessionId,
                ["positionTicks"] = e.PlaybackPositionTicks,
                ["playedToCompletion"] = e.PlayedToCompletion
            });

        _ = Task.Run(
            async () =>
            {
                try
                {
                    await _backendClient.PushEventAsync(payload, CancellationToken.None).ConfigureAwait(false);
                }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "MovieNight playback event push failed");
                }
            });
    }
}
