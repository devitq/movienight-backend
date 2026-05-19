using System;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace Jellyfin.Plugin.MovieNight.Services;

/// <summary>
/// Periodically asks MovieNight to run its current Jellyfin sync.
/// </summary>
public sealed class MovieNightPeriodicSyncService : BackgroundService
{
    private readonly MovieNightBackendClient _backendClient;
    private readonly ILogger<MovieNightPeriodicSyncService> _logger;

    /// <summary>
    /// Initializes a new instance of the <see cref="MovieNightPeriodicSyncService"/> class.
    /// </summary>
    /// <param name="backendClient">Backend client.</param>
    /// <param name="logger">Logger.</param>
    public MovieNightPeriodicSyncService(
        MovieNightBackendClient backendClient,
        ILogger<MovieNightPeriodicSyncService> logger)
    {
        _backendClient = backendClient;
        _logger = logger;
    }

    /// <inheritdoc />
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        while (!stoppingToken.IsCancellationRequested)
        {
            var delay = GetDelay();
            try
            {
                await Task.Delay(delay, stoppingToken).ConfigureAwait(false);
                if (!ShouldRun())
                {
                    continue;
                }

                await _backendClient.TriggerSyncAsync(stoppingToken).ConfigureAwait(false);
            }
            catch (OperationCanceledException) when (stoppingToken.IsCancellationRequested)
            {
                return;
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "MovieNight periodic sync failed");
            }
        }
    }

    private static bool ShouldRun()
    {
        var configuration = Plugin.Instance?.Configuration;
        return configuration is { Enabled: true, EnablePeriodicSync: true };
    }

    private static TimeSpan GetDelay()
    {
        var minutes = Plugin.Instance?.Configuration.SyncIntervalMinutes ?? 30;
        return TimeSpan.FromMinutes(Math.Clamp(minutes, 1, 1440));
    }
}
