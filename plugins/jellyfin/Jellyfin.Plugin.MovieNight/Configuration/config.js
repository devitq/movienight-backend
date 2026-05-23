const movieNightConfigPage = {
  pluginId: "42c72919-d6ff-4f62-bb8c-0fac39efafdb",

  loadConfiguration(view) {
    Dashboard.showLoadingMsg();

    return ApiClient.getPluginConfiguration(this.pluginId)
      .then((config) => {
        view.querySelector("#BackendBaseUrl").value =
          config.BackendBaseUrl || "";
        view.querySelector("#ApiToken").value = config.ApiToken || "";
        view.querySelector("#SyncIntervalMinutes").value =
          config.SyncIntervalMinutes || 30;
        view.querySelector("#StrmOutputPath").value =
          config.StrmOutputPath || "";
        view.querySelector("#Enabled").checked = config.Enabled || false;
        view.querySelector("#EnablePeriodicSync").checked =
          config.EnablePeriodicSync !== false;
        view.querySelector("#EnablePlaybackEvents").checked =
          config.EnablePlaybackEvents !== false;

        const uiScriptUrl = ApiClient.getUrl("web/ConfigurationPage", {
          name: "MovieNight.ui.js",
        });
        view.querySelector("#UIScriptUrl").innerText = uiScriptUrl;
      })
      .finally(() => {
        Dashboard.hideLoadingMsg();
      });
  },

  saveConfiguration(view) {
    const form = view.querySelector("#MovieNightConfigForm");
    Dashboard.showLoadingMsg();

    return ApiClient.getPluginConfiguration(this.pluginId)
      .then((config) => {
        config.BackendBaseUrl = form.querySelector("#BackendBaseUrl").value;
        config.ApiToken = form.querySelector("#ApiToken").value;
        config.SyncIntervalMinutes = parseInt(
          form.querySelector("#SyncIntervalMinutes").value || "30",
          10,
        );
        config.StrmOutputPath = form.querySelector("#StrmOutputPath").value;
        config.Enabled = form.querySelector("#Enabled").checked;
        config.EnablePeriodicSync =
          form.querySelector("#EnablePeriodicSync").checked;
        config.EnablePlaybackEvents =
          form.querySelector("#EnablePlaybackEvents").checked;

        return ApiClient.updatePluginConfiguration(this.pluginId, config);
      })
      .then((result) => {
        Dashboard.processPluginConfigurationUpdateResult(result);
      })
      .finally(() => {
        Dashboard.hideLoadingMsg();
      });
  },

  testConnection() {
    Dashboard.showLoadingMsg();

    return ApiClient.ajax({
      type: "POST",
      url: ApiClient.getUrl("MovieNight/TestConnection"),
    })
      .then((result) => {
        Dashboard.alert((result && result.message) || "OK");
      })
      .catch(() => {
        Dashboard.alert("MovieNight connection test failed");
      })
      .finally(() => {
        Dashboard.hideLoadingMsg();
      });
  },
};

export default function (view) {
  movieNightConfigPage.loadConfiguration(view);

  view
    .querySelector("#MovieNightConfigForm")
    .addEventListener("submit", (event) => {
      event.preventDefault();
      movieNightConfigPage.saveConfiguration(view);
    });

  view.querySelector("#TestConnection").addEventListener("click", (event) => {
    event.preventDefault();
    movieNightConfigPage.testConnection();
  });
}
