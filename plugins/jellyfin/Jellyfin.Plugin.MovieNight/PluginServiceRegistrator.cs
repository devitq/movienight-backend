using Jellyfin.Plugin.MovieNight.Services;
using MediaBrowser.Controller;
using MediaBrowser.Controller.Plugins;
using Microsoft.Extensions.DependencyInjection;

namespace Jellyfin.Plugin.MovieNight;

/// <summary>
/// Registers MovieNight services with Jellyfin.
/// </summary>
public class PluginServiceRegistrator : IPluginServiceRegistrator
{
    /// <inheritdoc />
    public void RegisterServices(IServiceCollection serviceCollection, IServerApplicationHost applicationHost)
    {
        serviceCollection.AddSingleton<MovieNightBackendClient>();
        serviceCollection.AddHostedService<MovieNightPeriodicSyncService>();
        serviceCollection.AddHostedService<MovieNightPlaybackEventService>();
    }
}
