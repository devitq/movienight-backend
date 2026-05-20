using System.Collections.Generic;
using MediaBrowser.Model.Plugins;

namespace Jellyfin.Plugin.MovieNight.Configuration;

/// <summary>
/// MovieNight plugin settings persisted by Jellyfin.
/// </summary>
public class PluginConfiguration : BasePluginConfiguration
{
    /// <summary>
    /// Gets or sets a value indicating whether integration calls are enabled.
    /// </summary>
    public bool Enabled { get; set; }

    /// <summary>
    /// Gets or sets the MovieNight backend base URL.
    /// </summary>
    public string BackendBaseUrl { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the backend plugin token.
    /// </summary>
    public string ApiToken { get; set; } = string.Empty;

    /// <summary>
    /// Gets or sets the periodic sync interval in minutes.
    /// </summary>
    public int SyncIntervalMinutes { get; set; } = 30;

    /// <summary>
    /// Gets or sets a value indicating whether playback stop events are pushed to MovieNight.
    /// </summary>
    public bool EnablePlaybackEvents { get; set; } = true;

    /// <summary>
    /// Gets or sets a value indicating whether periodic backend sync is enabled.
    /// </summary>
    public bool EnablePeriodicSync { get; set; } = true;

    /// <summary>
    /// Gets or sets enabled Jellyfin library ids. Empty means all libraries.
    /// </summary>
    public List<string> EnabledLibraryIds { get; set; } = new();

    /// <summary>
    /// Gets or sets the path where .strm files will be created.
    /// </summary>
    public string StrmOutputPath { get; set; } = string.Empty;
}
