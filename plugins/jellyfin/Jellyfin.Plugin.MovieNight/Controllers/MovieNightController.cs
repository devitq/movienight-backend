using System;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Tasks;
using Jellyfin.Plugin.MovieNight.Services;
using MediaBrowser.Controller.Library;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace Jellyfin.Plugin.MovieNight.Controllers;

/// <summary>
/// Admin endpoints for the MovieNight plugin.
/// </summary>
[ApiController]
[Route("MovieNight")]
public class MovieNightController : ControllerBase
{
    private readonly MovieNightBackendClient _backendClient;
    private readonly MovieNightSyncService _syncService;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightController"/> class.
    /// </summary>
    public MovieNightController(
        MovieNightBackendClient backendClient,
        MovieNightSyncService syncService)
    {
        _backendClient = backendClient;
        _syncService = syncService;
    }

    /// <summary>
    /// Ping endpoint for connectivity checks.
    /// </summary>
    [HttpGet("Ping")]
    public ActionResult Ping() => Ok("Pong");

    /// <summary>
    /// Returns plugin status.
    /// </summary>
    /// <returns>Status response.</returns>
    [HttpGet("Status")]
    [Authorize]
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
    [Authorize]
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
    [Authorize]
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
    [Authorize]
    public async Task<ActionResult<string>> SyncState(CancellationToken cancellationToken)
    {
        return await _backendClient.GetSyncStateAsync(cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Gets recommendations for the current user.
    /// </summary>
    [HttpGet("Users/{userId}/Recommendations")]
    [Authorize]
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
    [Authorize]
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
    [Authorize]
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
    /// Gets user preferences.
    /// </summary>
    [HttpGet("Users/{userId}/Preferences")]
    [Authorize]
    public async Task<ActionResult<string?>> GetPreferences(
        [FromRoute] string userId,
        CancellationToken cancellationToken)
    {
        return await _backendClient.GetPreferencesAsync(userId, cancellationToken).ConfigureAwait(false);
    }

    /// <summary>
    /// Completes onboarding for a user.
    /// </summary>
    [HttpPost("Users/{userId}/Onboarding")]
    [Authorize]
    public async Task<ActionResult> CompleteOnboarding(
        [FromRoute] string userId,
        [FromBody] object payload,
        CancellationToken cancellationToken)
    {
        await _backendClient.CompleteOnboardingAsync(userId, payload, cancellationToken).ConfigureAwait(false);
        return Ok();
    }

    /// <summary>
    /// Creates a new film by generating a .strm file in a folder-per-movie structure.
    /// Structure: Movie Name (Year) [imdbid-ttXXXXXXX]/Movie Name (Year) [imdbid-ttXXXXXXX].strm
    /// </summary>
    [HttpPost("Films")]
    [Authorize]
    public async Task<ActionResult> CreateFilm([FromBody] CreateFilmRequest request)
    {
        var config = Plugin.Instance?.Configuration;
        if (config == null || string.IsNullOrWhiteSpace(config.StrmOutputPath))
        {
            return BadRequest("STRM output path is not configured.");
        }

        if (string.IsNullOrWhiteSpace(request.Title))
        {
            return BadRequest("Movie title is required.");
        }

        try
        {
            // Construct name: "Movie Name (Year) [imdbid-ttXXXXXXX]"
            var folderName = request.Title.Trim();
            if (request.Year.HasValue)
            {
                folderName += $" ({request.Year})";
            }
            if (!string.IsNullOrWhiteSpace(request.ImdbId))
            {
                var ttId = request.ImdbId.Trim().ToLowerInvariant();
                if (!ttId.StartsWith("tt")) ttId = "tt" + ttId;
                folderName += $" [imdbid-{ttId}]";
            }

            // Sanitize for file system
            var invalidChars = Path.GetInvalidFileNameChars();
            var safeFolderName = new string(folderName.Select(c => invalidChars.Contains(c) ? '_' : c).ToArray());

            var movieDirectory = Path.Combine(config.StrmOutputPath, safeFolderName);
            if (!Directory.Exists(movieDirectory))
            {
                Directory.CreateDirectory(movieDirectory);
            }

            var filePath = Path.Combine(movieDirectory, $"{safeFolderName}.strm");

            var strmContent = string.IsNullOrWhiteSpace(request.Url)
                ? "http://placeholder.url/upload_me_later"
                : request.Url.Trim();

            await System.IO.File.WriteAllTextAsync(filePath, strmContent).ConfigureAwait(false);

            return Ok(new { FilePath = filePath, FolderName = safeFolderName });
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
public sealed record CreateFilmRequest(string Title, string? Url, int? Year, string? ImdbId);

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
