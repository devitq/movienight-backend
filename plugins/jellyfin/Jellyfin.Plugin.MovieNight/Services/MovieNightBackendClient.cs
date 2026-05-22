using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;

namespace Jellyfin.Plugin.MovieNight.Services;

/// <summary>
/// Thin HTTP client for the MovieNight backend.
/// </summary>
public class MovieNightBackendClient
{
    private static readonly JsonSerializerOptions JsonOptions = new(JsonSerializerDefaults.Web);
    private readonly ILogger<MovieNightBackendClient> _logger;
    private readonly HttpClient _httpClient;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightBackendClient"/> class.
    /// </summary>
    /// <param name="logger">Logger.</param>
    public MovieNightBackendClient(ILogger<MovieNightBackendClient> logger)
    {
        _logger = logger;
        _httpClient = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(20)
        };
    }

    /// <summary>
    /// Calls backend health.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Connection result.</returns>
    public async Task<MovieNightConnectionResult> TestConnectionAsync(CancellationToken cancellationToken)
    {
        var payload = new MovieNightEventPayload(
            EventId: $"plugin-test:{Guid.NewGuid():N}",
            EventType: "plugin.test",
            OccurredAt: DateTimeOffset.UtcNow,
            JellyfinUserId: "movienight-plugin-test-user",
            ItemId: "movienight-plugin-test-item",
            PayloadVersion: 1,
            Payload: new Dictionary<string, object?>
            {
                ["source"] = "config-test"
            });
        var request = CreateEventRequest(payload);
        if (request is null)
        {
            return MovieNightConnectionResult.Failed("Plugin is not configured.");
        }

        try
        {
            using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
            return response.IsSuccessStatusCode
                ? MovieNightConnectionResult.Ok()
                : MovieNightConnectionResult.Failed($"Backend event endpoint returned {(int)response.StatusCode}.");
        }
        catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException)
        {
            _logger.LogWarning(ex, "MovieNight connection test failed");
            return MovieNightConnectionResult.Failed(ex.Message);
        }
    }

    /// <summary>
    /// Pushes library sync data to the backend.
    /// </summary>
    /// <param name="payload">Sync payload.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Backend response body.</returns>
    public async Task<string> SyncAsync(object payload, CancellationToken cancellationToken)
    {
        var request = CreateRequest(HttpMethod.Post, "/api/integrations/jellyfin/sync");
        if (request is null)
        {
            return "Plugin is not configured.";
        }

        request.Content = JsonContent.Create(payload, options: JsonOptions);
        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        return body;
    }

    /// <summary>
    /// Gets recommendations for a user.
    /// </summary>
    public async Task<string> GetRecommendationsAsync(string userId, string? contentType, string? mood, int limit, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        var query = $"?limit={limit}";
        if (!string.IsNullOrEmpty(contentType)) query += $"&contentType={Uri.EscapeDataString(contentType)}";
        if (!string.IsNullOrEmpty(mood)) query += $"&mood={Uri.EscapeDataString(mood)}";

        var request = CreateRequest(HttpMethod.Get, $"/api/integrations/jellyfin/users/{userId}/recommendations{query}");
        if (request is null) return "Plugin is not configured.";

        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        return body;
    }

    /// <summary>
    /// Posts a rating for a film.
    /// </summary>
    public async Task PostRatingAsync(string userId, string filmId, int score, string? note, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        filmId = NormalizeJellyfinId(filmId);
        var request = CreateRequest(HttpMethod.Post, $"/api/integrations/jellyfin/users/{userId}/ratings/items/{filmId}");
        if (request is null) return;

        request.Content = JsonContent.Create(new { score, note }, options: JsonOptions);
        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
    }

    /// <summary>
    /// Gets ratings for a user.
    /// </summary>
    public async Task<string> GetRatingsAsync(string userId, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        var request = CreateRequest(HttpMethod.Get, $"/api/integrations/jellyfin/users/{userId}/ratings");
        if (request is null) return "[]";

        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        return body;
    }

    /// <summary>
    /// Marks a film as viewed.
    /// </summary>
    public async Task MarkViewedAsync(string userId, string filmId, DateTimeOffset? watchedAt, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        filmId = NormalizeJellyfinId(filmId);
        var request = CreateRequest(HttpMethod.Post, $"/api/integrations/jellyfin/users/{userId}/library/items/{filmId}/viewed");
        if (request is null) return;

        request.Content = JsonContent.Create(new { watchedAt }, options: JsonOptions);
        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
    }

    /// <summary>
    /// Reads backend sync state.
    /// </summary>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>Backend response body.</returns>
    public async Task<string> GetSyncStateAsync(CancellationToken cancellationToken)
    {
        var request = CreateRequest(HttpMethod.Get, "/api/integrations/jellyfin/sync-state");
        if (request is null)
        {
            return "Plugin is not configured.";
        }

        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        return body;
    }

    /// <summary>
    /// Gets user preferences.
    /// </summary>
    public async Task<string?> GetPreferencesAsync(string userId, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        var request = CreateRequest(HttpMethod.Get, $"/api/integrations/jellyfin/users/{userId}/preferences");
        if (request is null) return null;

        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        if (response.StatusCode == System.Net.HttpStatusCode.NotFound) return null;

        var body = await response.Content.ReadAsStringAsync(cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
        return body;
    }

    /// <summary>
    /// Completes onboarding for a user.
    /// </summary>
    public async Task CompleteOnboardingAsync(string userId, object payload, CancellationToken cancellationToken)
    {
        userId = NormalizeJellyfinId(userId);
        var request = CreateRequest(HttpMethod.Post, $"/api/integrations/jellyfin/users/{userId}/recommendation-onboarding");
        if (request is null) return;

        request.Content = JsonContent.Create(payload, options: JsonOptions);
        using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
        response.EnsureSuccessStatusCode();
    }

    /// <summary>
    /// Pushes an event payload to the backend event endpoint.
    /// </summary>
    /// <param name="payload">Event payload.</param>
    /// <param name="cancellationToken">Cancellation token.</param>
    /// <returns>A task.</returns>
    public async Task PushEventAsync(MovieNightEventPayload payload, CancellationToken cancellationToken)
    {
        for (var attempt = 1; attempt <= 3; attempt++)
        {
            var request = CreateEventRequest(payload);
            if (request is null)
            {
                return;
            }

            try
            {
                using var response = await _httpClient.SendAsync(request, cancellationToken).ConfigureAwait(false);
                if (response.IsSuccessStatusCode)
                {
                    return;
                }

                if ((int)response.StatusCode == 401)
                {
                    _logger.LogWarning("MovieNight event push was rejected with 401 Unauthorized");
                    return;
                }

                _logger.LogDebug("MovieNight event push returned status {StatusCode}", response.StatusCode);
            }
            catch (Exception ex) when (ex is HttpRequestException or TaskCanceledException)
            {
                _logger.LogDebug(ex, "MovieNight event push attempt {Attempt} failed", attempt);
            }

            if (attempt < 3)
            {
                await Task.Delay(TimeSpan.FromSeconds(attempt * 2), cancellationToken).ConfigureAwait(false);
            }
        }
    }

    private static HttpRequestMessage? CreateEventRequest(MovieNightEventPayload payload)
    {
        var request = CreateRequest(HttpMethod.Post, "/api/integrations/jellyfin/events");
        if (request is null)
        {
            return null;
        }

        request.Content = JsonContent.Create(payload, options: JsonOptions);
        return request;
    }

    private static string? GetBaseUrl()
    {
        var value = Plugin.Instance?.Configuration.BackendBaseUrl?.Trim();
        return string.IsNullOrWhiteSpace(value) ? null : value.TrimEnd('/');
    }

    private static string NormalizeJellyfinId(string value)
    {
        return Guid.TryParse(value, out var guid) ? guid.ToString("N") : value;
    }

    private static bool IsEnabled()
    {
        var configuration = Plugin.Instance?.Configuration;
        return configuration is { Enabled: true } && !string.IsNullOrWhiteSpace(configuration.BackendBaseUrl);
    }

    private static HttpRequestMessage? CreateRequest(HttpMethod method, string path)
    {
        if (!IsEnabled())
        {
            return null;
        }

        var baseUrl = GetBaseUrl();
        if (baseUrl is null)
        {
            return null;
        }

        var request = new HttpRequestMessage(method, new Uri(baseUrl + path));
        var token = Plugin.Instance?.Configuration.ApiToken;
        if (!string.IsNullOrWhiteSpace(token))
        {
            request.Headers.Add("X-MovieNight-Plugin-Token", token);
        }

        return request;
    }
}

/// <summary>
/// Backend connection result.
/// </summary>
/// <param name="Success">Whether the call succeeded.</param>
/// <param name="Message">Result message.</param>
public sealed record MovieNightConnectionResult(bool Success, string Message)
{
    /// <summary>
    /// Creates a successful result.
    /// </summary>
    /// <returns>Connection result.</returns>
    public static MovieNightConnectionResult Ok() => new(true, "OK");

    /// <summary>
    /// Creates a failed result.
    /// </summary>
    /// <param name="message">Failure message.</param>
    /// <returns>Connection result.</returns>
    public static MovieNightConnectionResult Failed(string message) => new(false, message);
}
