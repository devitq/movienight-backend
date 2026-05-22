using System;
using System.IO;
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
[Route("MovieNight")]
[Authorize(Policy = "DefaultAuthorization")]
public class MovieNightController : ControllerBase
{
    private readonly MovieNightBackendClient _backendClient;
    private readonly MovieNightSyncService _syncService;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightController"/> class.
    /// </summary>
    public MovieNightController(MovieNightBackendClient backendClient, MovieNightSyncService syncService)
    {
        _backendClient = backendClient;
        _syncService = syncService;
    }

    /// <summary>
    /// Ping endpoint for connectivity checks.
    /// </summary>
    [HttpGet("Ping")]
    [AllowAnonymous]
    public ActionResult Ping() => Ok("Pong");

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
    [Authorize(Policy = "RequiresAdmin")]
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
    [Authorize(Policy = "RequiresAdmin")]
    public async Task<ActionResult<string>> Sync(CancellationToken cancellationToken)
    {
        await _syncService.PerformSyncAsync(cancellationToken).ConfigureAwait(false);
        return Ok("Sync triggered");
    }

    /// <summary>
    /// Gets backend sync state.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Backend response.</returns>
    [HttpGet("SyncState")]
    [Authorize(Policy = "RequiresAdmin")]
    public async Task<ActionResult<string>> SyncState(CancellationToken cancellationToken)
    {
        return await _backendClient.GetSyncStateAsync(cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Gets recommendations for the current user.
    /// </summary>
    [HttpGet("Users/{userId}/Recommendations")]
    public async Task<ActionResult<string>> GetRecommendations(
        [FromRoute] string userId,
        [FromQuery] string? contentType,
        [FromQuery] string? mood,
        [FromQuery] int limit = 10,
        CancellationToken cancellationToken = default)
    {
        return await _backendClient.GetRecommendationsAsync(userId, contentType, mood, limit, cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Posts a rating for a film.
    /// </summary>
    [HttpPost("Users/{userId}/Ratings/Films/{filmId}")]
    public async Task<ActionResult> PostRating(
        [FromRoute] string userId,
        [FromRoute] string filmId,
        [FromBody] RatingRequest request,
        CancellationToken cancellationToken)
    {
        await _backendClient.PostRatingAsync(userId, filmId, request.Score, request.Note, cancellationToken).ConfigureAwait(false);
        return Ok();
    }

    /// <summary>
    /// Marks a film as viewed.
    /// </summary>
    [HttpPost("Users/{userId}/Library/Films/{filmId}/Viewed")]
    public async Task<ActionResult> MarkViewed(
        [FromRoute] string userId,
        [FromRoute] string filmId,
        [FromBody] ViewedRequest request,
        CancellationToken cancellationToken)
    {
        await _backendClient.MarkViewedAsync(userId, filmId, request.WatchedAt, cancellationToken).ConfigureAwait(false);
        return Ok();
    }

    /// <summary>
    /// Creates a new film by generating a .strm file.
    /// </summary>
    [HttpPost("Films")]
    [Authorize(Policy = "RequiresAdmin")]
    public async Task<ActionResult> CreateFilm([FromBody] CreateFilmRequest request)
    {
        var config = Plugin.Instance?.Configuration;
        if (config == null || string.IsNullOrWhiteSpace(config.StrmOutputPath))
        {
            return BadRequest("STRM output path is not configured.");
        }

        try
        {
            if (!Directory.Exists(config.StrmOutputPath))
            {
                Directory.CreateDirectory(config.StrmOutputPath);
            }

            var safeTitle = string.Join("_", request.Title.Split(Path.GetInvalidFileNameChars()));
            var fileName = $"{safeTitle}.strm";
            var filePath = Path.Combine(config.StrmOutputPath, fileName);

            // Use the provided URL or a placeholder if missing
            var strmContent = string.IsNullOrWhiteSpace(request.Url)
                ? "http://placeholder.url/upload_me_later"
                : request.Url;

            await System.IO.File.WriteAllTextAsync(filePath, strmContent).ConfigureAwait(false);

            return Ok(new { FilePath = filePath });
        }
        catch (Exception ex)
        {
            return StatusCode(500, $"Failed to create film: {ex.Message}");
        }
    }
}

/// <summary>
/// Create film request.
/// </summary>
public sealed record CreateFilmRequest(string Title, string? Url);

/// <summary>
/// Rating request.
/// </summary>
public sealed record RatingRequest(int Score, string? Note);

/// <summary>
/// Viewed request.
/// </summary>
public sealed record ViewedRequest(DateTimeOffset? WatchedAt);

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
