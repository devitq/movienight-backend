using System;
using System.Threading;
using System.Threading.Tasks;
using Jellyfin.Plugin.MovieNight.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Jellyfin.Plugin.MovieNight.Controllers;

/// <summary>
/// Admin endpoints for the MovieNight plugin.
/// </summary>
[ApiController]
[Authorize]
[Route("MovieNight")]
public class MovieNightController : ControllerBase
{
    private readonly MovieNightBackendClient _backendClient;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightController"/> class.
    /// </summary>
    /// <param name="backendClient">Backend client.</param>
    public MovieNightController(MovieNightBackendClient backendClient)
    {
        _backendClient = backendClient;
    }

    /// <summary>
    /// Returns plugin status.
    /// </summary>
    /// <returns>Status response.</returns>
    [HttpGet("Status")]
    public ActionResult<MovieNightPluginStatus> GetStatus()
    {
        var configuration = Plugin.Instance?.Configuration;
        return new MovieNightPluginStatus(
            Enabled: configuration?.Enabled ?? false,
            BackendBaseUrl: configuration?.BackendBaseUrl ?? string.Empty,
            PeriodicSyncEnabled: configuration?.EnablePeriodicSync ?? false,
            PlaybackEventsEnabled: configuration?.EnablePlaybackEvents ?? false,
            SyncIntervalMinutes: configuration?.SyncIntervalMinutes ?? 30);
    }

    /// <summary>
    /// Tests backend connectivity.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Connection result.</returns>
    [HttpPost("TestConnection")]
    public async Task<ActionResult<MovieNightConnectionResult>> TestConnection(CancellationToken cancellationToken)
    {
        return await _backendClient.TestConnectionAsync(cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Triggers backend sync.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Backend response.</returns>
    [HttpPost("Sync")]
    public async Task<ActionResult<string>> Sync(CancellationToken cancellationToken)
    {
        return await _backendClient.TriggerSyncAsync(cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Gets backend sync state.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Backend response.</returns>
    [HttpGet("SyncState")]
    public async Task<ActionResult<string>> SyncState(CancellationToken cancellationToken)
    {
        return await _backendClient.GetSyncStateAsync(cancellationToken).ConfigureAwait(false);
    }
}

/// <summary>
/// MovieNight plugin status response.
/// </summary>
/// <param name="Enabled">Whether integration is enabled.</param>
/// <param name="BackendBaseUrl">Backend base URL.</param>
/// <param name="PeriodicSyncEnabled">Whether periodic sync is enabled.</param>
/// <param name="PlaybackEventsEnabled">Whether playback events are enabled.</param>
/// <param name="SyncIntervalMinutes">Sync interval in minutes.</param>
public sealed record MovieNightPluginStatus(
    bool Enabled,
    string BackendBaseUrl,
    bool PeriodicSyncEnabled,
    bool PlaybackEventsEnabled,
    int SyncIntervalMinutes);
