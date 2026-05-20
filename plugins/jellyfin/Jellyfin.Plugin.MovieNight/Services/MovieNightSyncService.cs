using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Controller.Library;
using MediaBrowser.Controller.Entities;
using MediaBrowser.Controller.Entities.Movies;
using MediaBrowser.Model.Entities;
using MediaBrowser.Model.Querying;
using Jellyfin.Data.Enums;
using Microsoft.Extensions.Logging;

namespace Jellyfin.Plugin.MovieNight.Services;

/// <summary>
/// Service for synchronizing the Jellyfin library with MovieNight.
/// </summary>
public class MovieNightSyncService
{
    private readonly MovieNightBackendClient _backendClient;
    private readonly ILibraryManager _libraryManager;
    private readonly IUserManager _userManager;
    private readonly IUserDataManager _userDataManager;
    private readonly ILogger<MovieNightSyncService> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightSyncService"/> class.
    /// </summary>
    public MovieNightSyncService(
        MovieNightBackendClient backendClient,
        ILibraryManager libraryManager,
        IUserManager userManager,
        IUserDataManager userDataManager,
        ILogger<MovieNightSyncService> logger)
    {
        _backendClient = backendClient;
        _libraryManager = libraryManager;
        _userManager = userManager;
        _userDataManager = userDataManager;
        _logger = logger;
    }

    /// <summary>
    /// Performs a full library sync.
    /// </summary>
    public async Task PerformSyncAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting MovieNight library sync");

        var items = _libraryManager.GetItemList(new InternalItemsQuery
        {
            IncludeItemTypes = new[] { BaseItemKind.Movie },
            Recursive = true
        });

        var users = _userManager.Users;
        var syncItems = new List<object>();

        foreach (var item in items)
        {
            if (item is not Movie movie) continue;

            var jellyfinItemId = movie.Id.ToString("N");

            var itemData = new Dictionary<string, object?>
            {
                ["jellyfinItemId"] = jellyfinItemId,
                ["title"] = movie.Name,
                ["originalTitle"] = movie.OriginalTitle,
                ["description"] = movie.Overview,
                ["year"] = movie.ProductionYear,
                ["duration"] = movie.RunTimeTicks,
                ["genres"] = movie.Genres,
                ["imdbId"] = movie.GetProviderId(MetadataProvider.Imdb),
                ["tmdbId"] = movie.GetProviderId(MetadataProvider.Tmdb),
                ["userStates"] = users.Select(u => {
                    var userData = _userDataManager.GetUserData(u, movie);
                    return new {
                        jellyfinUserId = u.Id.ToString("N"),
                        isViewed = userData?.Played ?? false,
                        playCount = userData?.PlayCount ?? 0,
                        lastPlayedAt = userData?.LastPlayedDate,
                        userRating = userData?.Rating
                    };
                }).ToList()
            };

            syncItems.Add(itemData);
        }

        await _backendClient.SyncAsync(new { items = syncItems }, cancellationToken).ConfigureAwait(false);
        _logger.LogInformation("MovieNight library sync completed");
    }
}
