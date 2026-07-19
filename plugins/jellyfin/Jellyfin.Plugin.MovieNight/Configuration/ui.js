(function () {
    if (typeof window.movieNightUiCleanup === 'function') {
        window.movieNightUiCleanup();
    }

    const PLUGIN_ID = "42c72919-d6ff-4f62-bb8c-0fac39efafdb";
    const ROUTE_RETRY_DELAYS_MS = [0, 100, 300, 700, 1500, 3000];

    function getAlert() {
        if (typeof Dashboard !== 'undefined' && Dashboard.alert) {
            return (options) => Dashboard.alert(formatAlertMessage(options));
        }
        return (options) => {
            alert(formatAlertMessage(options));
        };
    }

    const showMsg = getAlert();

    function formatAlertMessage(options) {
        if (typeof options === 'string') return options;
        if (!options) return '';
        return [options.title, options.text || options.message].filter(Boolean).join('\n\n');
    }

    function ensureMovieNightStyles() {
        if (document.getElementById('movieNightUiStyles')) return;

        const style = document.createElement('style');
        style.id = 'movieNightUiStyles';
        style.textContent = `
            .movieNightActionButton {
                align-items: center;
                border: var(--defaultLighterBorder, 1px solid rgba(255,255,255,.16));
                border-radius: var(--smallRadius, 8px);
                display: inline-flex;
                gap: .55em;
                min-height: 2.65em;
                padding: .55em .9em;
                transition: background-color .16s ease, border-color .16s ease, color .16s ease;
            }
            .movieNightActionButton:hover,
            .movieNightActionButton:focus {
                border-color: var(--dimTextColor, rgba(255,255,255,.45));
                color: #fff;
            }
            .movieNightActionButton .material-icons {
                font-size: 1.35em;
            }
            .movieNightDetailButton {
                color: var(--textColor, #fff);
            }
            .movieNightDetailButton .detailButton-content {
                border-radius: 999px;
                outline: 1px solid rgba(255,255,255,.16);
                outline-offset: -1px;
            }
            .movieNightDetailButton:focus .detailButton-content,
            .movieNightDetailButton:hover .detailButton-content {
                background: rgba(255,255,255,.18);
            }
            .movieNightHomeButtons {
                margin-bottom: 1.25em;
            }
            .movieNightPanel {
                background: color-mix(in srgb, var(--headerColor, #202020) 78%, transparent);
                border: var(--defaultBorder, 1px solid rgba(255,255,255,.12));
                border-radius: var(--smallRadius, 8px);
                box-sizing: border-box;
                padding: 1em;
            }
            .movieNightPanelHeader {
                align-items: center;
                display: flex;
                gap: 1em;
                justify-content: space-between;
                margin-bottom: .75em;
            }
            .movieNightPanelHeader .sectionTitle {
                margin: 0;
            }
            .movieNightSyncStatus {
                color: var(--dimTextColor, rgba(255,255,255,.65));
                font-size: .86em;
                text-align: right;
            }
            .movieNightBtnContainer {
                display: flex;
                flex-wrap: wrap;
                gap: .65em;
            }
            .movieNightDialog {
                background: color-mix(in srgb, var(--drawerColor, #1f1f1f) 92%, transparent) !important;
                border: var(--defaultBorder, 1px solid rgba(255,255,255,.15)) !important;
                border-radius: var(--smallRadius, 8px) !important;
                box-shadow: var(--shadow, 0 18px 55px rgba(0,0,0,.55)) !important;
                box-sizing: border-box;
                color: var(--textColor, #fff) !important;
                max-width: calc(100vw - 2em);
            }
            .movieNightDialogTitle {
                align-items: center;
                display: flex;
                gap: .55em;
                margin: 0;
                font-size: 1.35em;
                font-weight: 500;
            }
            .movieNightDialogTitle .material-icons {
                color: var(--uiAccentColor, #00a4dc);
                font-size: 1.25em;
            }
            .movieNightDialog .dialog-content {
                color: var(--textColor, #fff);
            }
            .movieNightDialog .dialog-footer {
                justify-content: flex-end;
            }
            .movieNightRecommendationList {
                display: grid;
                gap: .8em;
            }
            .movieNightRecommendation {
                background: rgba(255,255,255,.055);
                border: var(--defaultLighterBorder, 1px solid rgba(255,255,255,.14));
                border-radius: var(--smallRadius, 8px);
                display: grid;
                gap: .9em;
                grid-template-columns: 76px minmax(0, 1fr);
                padding: .75em;
            }
            .movieNightRecommendationPoster {
                align-self: start;
                aspect-ratio: 2 / 3;
                background: rgba(255,255,255,.08);
                border-radius: var(--smallerRadius, 6px);
                object-fit: cover;
                overflow: hidden;
                width: 76px;
            }
            .movieNightRecommendationBody {
                min-width: 0;
            }
            .movieNightRecommendationHeader {
                align-items: start;
                display: flex;
                gap: 1em;
                justify-content: space-between;
            }
            .movieNightRecommendationTitle {
                color: #fff;
                font-size: 1.08em;
                font-weight: 600;
                line-height: 1.25;
                overflow-wrap: anywhere;
            }
            .movieNightRecommendationMeta,
            .movieNightRecommendationReason {
                color: var(--dimTextColor, rgba(255,255,255,.68));
                font-size: .9em;
                margin-top: .25em;
            }
            .movieNightRecommendationScore {
                background: rgba(255,255,255,.1);
                border-radius: 999px;
                color: #fff;
                flex: 0 0 auto;
                font-size: .82em;
                padding: .28em .65em;
                white-space: nowrap;
            }
            .movieNightRecommendationDescription {
                color: rgba(255,255,255,.84);
                display: -webkit-box;
                line-height: 1.35;
                margin-top: .65em;
                overflow: hidden;
                -webkit-box-orient: vertical;
                -webkit-line-clamp: 3;
            }
            .movieNightRecommendationActions {
                display: flex;
                flex-wrap: wrap;
                gap: .5em;
                margin-top: .75em;
            }
            .movieNightRecommendationActions .emby-button {
                min-height: 2.35em;
            }
            .movieNightField {
                margin-bottom: 1em;
            }
            .movieNightField label {
                color: var(--dimTextColor, rgba(255,255,255,.72));
                display: block;
                font-size: .9em;
                margin-bottom: .35em;
            }
            .movieNightFieldRow {
                display: grid;
                gap: 1em;
                grid-template-columns: minmax(6em, .7fr) minmax(0, 1.3fr);
            }
            .movieNightDialog .emby-input {
                box-sizing: border-box;
                width: 100%;
            }
            .movieNightOnboardingDialog {
                display: flex;
                flex-direction: column;
                max-height: min(820px, calc(100vh - 2em));
                width: min(720px, calc(100vw - 2em));
            }
            .movieNightOnboardingDialog .dialog-content {
                overflow-y: auto;
                padding-right: .25em;
            }
            .movieNightOnboardingGrid {
                display: grid;
                gap: 1em;
                grid-template-columns: 1fr 1fr;
            }
            .movieNightOnboardingWide {
                grid-column: 1 / -1;
            }
            .movieNightChipGroup {
                display: flex;
                flex-wrap: wrap;
                gap: .5em;
            }
            .movieNightChip {
                background: rgba(255,255,255,.045);
                border: var(--defaultLighterBorder, 1px solid rgba(255,255,255,.18));
                border-radius: 999px;
                color: var(--textColor, #fff);
                cursor: pointer;
                font-size: .9em;
                line-height: 1.2;
                padding: .45em .85em;
                transition: background-color .16s ease, border-color .16s ease;
                user-select: none;
            }
            .movieNightChip:hover,
            .movieNightChip:focus {
                border-color: var(--uiAccentColor, #00a4dc);
                outline: none;
            }
            .movieNightChip.is-selected {
                background: var(--uiAccentColor, #0064d2);
                border-color: var(--uiAccentColor, #0064d2);
                color: #fff;
            }
            .movieNightSelect,
            .movieNightTextarea {
                background: rgba(0,0,0,.18);
                border: var(--defaultLighterBorder, 1px solid rgba(255,255,255,.22));
                border-radius: var(--smallRadius, 8px);
                box-sizing: border-box;
                color: var(--textColor, #fff);
                min-height: 2.7em;
                padding: .55em .7em;
                width: 100%;
            }
            .movieNightTextarea {
                min-height: 5.5em;
                resize: vertical;
            }
            @media (max-width: 42em) {
                .movieNightRecommendation {
                    grid-template-columns: 56px minmax(0, 1fr);
                }
                .movieNightRecommendationPoster {
                    width: 56px;
                }
                .movieNightRecommendationHeader {
                    display: block;
                }
                .movieNightRecommendationScore {
                    display: inline-flex;
                    margin-top: .45em;
                }
                .movieNightPanelHeader {
                    align-items: flex-start;
                    flex-direction: column;
                    gap: .35em;
                }
                .movieNightSyncStatus {
                    text-align: left;
                }
                .movieNightFieldRow {
                    grid-template-columns: 1fr;
                    gap: 0;
                }
                .movieNightOnboardingGrid {
                    grid-template-columns: 1fr;
                }
            }
        `;
        document.head.appendChild(style);
    }

    function createTextButton(text, className, onClick, icon) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.is = 'emby-button';
        btn.className = `emby-button raised movieNightActionButton ${className}`;
        btn.innerHTML = icon
            ? `<span class="material-icons ${icon}" aria-hidden="true"></span><span>${text}</span>`
            : `<span>${text}</span>`;
        btn.onclick = onClick;
        return btn;
    }

    function createIconButton(icon, title, className, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.is = 'emby-button';
        btn.className = `button-flat detailButton emby-button movieNightDetailButton ${className}`;
        btn.title = title;
        btn.innerHTML = `
            <div class="detailButton-content">
                <span class="material-icons detailButton-icon ${icon}" aria-hidden="true"></span>
            </div>
        `;
        btn.onclick = onClick;
        return btn;
    }

    async function injectUI() {
        ensureMovieNightStyles();
        await checkOnboarding();

        // Item Detail Page
        const itemId = getItemIdFromUrl();
        document.querySelectorAll('.mainDetailButtons').forEach((detailButtons) => {
            if (!itemId) return;

            // MovieNight Rating
            const existingRateBtn = detailButtons.querySelector('.btnMovieNightRate');
            if (!existingRateBtn || existingRateBtn.dataset.movieNightItemId !== itemId) {
                existingRateBtn?.remove();
                const rateBtn = createIconButton('star_rate', 'Rate on MovieNight', 'btnMovieNightRate', (e) => {
                    e.preventDefault(); e.stopPropagation(); showRatingDialog(itemId);
                });
                rateBtn.dataset.movieNightItemId = itemId;
                insertInDetailRow(detailButtons, rateBtn);
            }
            // Mark Viewed in MovieNight
            const existingViewedBtn = detailButtons.querySelector('.btnMovieNightMarkViewed');
            if (!existingViewedBtn || existingViewedBtn.dataset.movieNightItemId !== itemId) {
                existingViewedBtn?.remove();
                const viewedBtn = createIconButton('visibility', 'Mark Viewed in MovieNight', 'btnMovieNightMarkViewed', (e) => {
                    e.preventDefault(); e.stopPropagation(); submitViewed(itemId);
                });
                viewedBtn.dataset.movieNightItemId = itemId;
                insertInDetailRow(detailButtons, viewedBtn);
            }
        });

        // Library Pages
        document
            .querySelectorAll('.libraryPage:not(.itemDetailPage) .flex.align-items-center.justify-content-center.focuscontainer-x')
            .forEach((toolBar) => {
                if (!toolBar.querySelector('.btnMovieNightRecommend')) {
                    toolBar.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', (e) => {
                        e.preventDefault(); showRecommendation();
                    }, 'auto_awesome'));
                }
                if (!toolBar.querySelector('.btnMovieNightAddMovie')) {
                    toolBar.appendChild(createTextButton('Add Movie', 'btnMovieNightAddMovie', (e) => {
                        e.preventDefault(); showAddMovieDialog();
                    }, 'add'));
                }
            });

        // Home Page
        document.querySelectorAll('.sections.homeSectionsContainer').forEach((homeSections) => {
            if (homeSections.querySelector('.movieNightHomeButtons')) return;

            const section = document.createElement('div');
            section.className = 'verticalSection movieNightHomeButtons';
            section.style.padding = '0 var(--sidePadding)';
            section.innerHTML = `
                <div class="movieNightPanel">
                    <div class="movieNightPanelHeader">
                        <h2 class="sectionTitle">MovieNight</h2>
                        <span class="movieNightSyncStatus"></span>
                    </div>
                    <div class="movieNightBtnContainer"></div>
                </div>
            `;
            const btnContainer = section.querySelector('.movieNightBtnContainer');
            btnContainer.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', showRecommendation, 'auto_awesome'));
            btnContainer.appendChild(createTextButton('Add Movie', 'btnMovieNightAddMovie', showAddMovieDialog, 'add'));
            btnContainer.appendChild(createTextButton('Sync Library', 'btnMovieNightSync', triggerSync, 'sync'));

            homeSections.insertBefore(section, homeSections.firstChild);
            updateSyncStatus();
        });
    }

    function insertInDetailRow(container, btn) {
        const moreBtn = container.querySelector('.btnMoreCommands');
        if (moreBtn) container.insertBefore(btn, moreBtn);
        else container.appendChild(btn);
    }

    function getItemIdFromUrl() {
        const queryString = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : window.location.search;
        const params = new URLSearchParams(queryString);
        return params.get('id') || params.get('itemId');
    }

    function createOverlay() {
        const overlay = document.createElement('div');
        overlay.className = 'dialogBackdrop dialogBackdropOpened';
        overlay.style.zIndex = '99998';
        overlay.style.backgroundColor = 'rgba(0,0,0,0.7)';
        overlay.style.position = 'fixed';
        overlay.style.top = '0'; overlay.style.left = '0'; overlay.style.right = '0'; overlay.style.bottom = '0';
        overlay.style.backdropFilter = 'blur(8px)';
        overlay.style.opacity = '1';
        return overlay;
    }

    function createDialogBase(title) {
        const dialog = document.createElement('div');
        dialog.className = 'dialog movieNightDialog';
        dialog.style.position = 'fixed';
        dialog.style.top = '50%'; dialog.style.left = '50%';
        dialog.style.transform = 'translate(-50%, -50%)';
        dialog.style.zIndex = '99999';
        dialog.style.padding = '1.25em';
        dialog.style.minWidth = '350px';
        dialog.style.opacity = '1';

        dialog.innerHTML = `
            <h2 class="movieNightDialogTitle">
                <span class="material-icons auto_awesome" aria-hidden="true"></span>
                <span>${title}</span>
            </h2>
            <div class="dialog-content" style="margin:1em 0; opacity:1;"></div>
            <div class="dialog-footer" style="display:flex; gap:1em; opacity:1;">
                <button is="emby-button" class="emby-button button-flat btnCancel" style="flex:1; color: white !important; opacity:1;">Cancel</button>
            </div>
        `;
        return dialog;
    }

    async function showRatingDialog(itemId) {
        const overlay = createOverlay();
        const dialog = createDialogBase('Rate on MovieNight');
        const content = dialog.querySelector('.dialog-content');

        content.innerHTML = `<div class="rating-grid" style="display:grid; grid-template-columns:repeat(5, 1fr); gap:0.8em;"></div>`;
        const grid = content.querySelector('.rating-grid');

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        for (let i = 1; i <= 10; i++) {
            const btn = document.createElement('button');
            btn.type = 'button'; btn.is = 'emby-button';
            btn.className = 'emby-button raised';
            btn.innerText = i;
            btn.style.padding = '0.8em 0';
            btn.style.textAlign = 'center';
            btn.style.display = 'flex';
            btn.style.alignItems = 'center';
            btn.style.justifyContent = 'center';
            btn.style.fontSize = '1.2em';
            btn.onclick = async () => { cleanup(); await submitRating(itemId, i); };
            grid.appendChild(btn);
        }

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
    }

    async function showAddMovieDialog() {
        const overlay = createOverlay();
        const dialog = createDialogBase('Add Movie');
        const content = dialog.querySelector('.dialog-content');
        const footer = dialog.querySelector('.dialog-footer');

        content.innerHTML = `
            <div class="movieNightField">
                <label>Movie Title</label>
                <input type="text" class="emby-input txtTitle" placeholder="e.g. Inception">
            </div>
            <div class="movieNightFieldRow">
                <div class="movieNightField">
                    <label>Year</label>
                    <input type="number" class="emby-input txtYear" placeholder="2010">
                </div>
                <div class="movieNightField">
                    <label>IMDb ID</label>
                    <input type="text" class="emby-input txtImdb" placeholder="tt1375666">
                </div>
            </div>
            <div class="movieNightField">
                <label>Stream URL</label>
                <input type="text" class="emby-input txtUrl" placeholder="http://...">
            </div>
        `;

        const btnAdd = document.createElement('button');
        btnAdd.className = 'emby-button raised button-submit';
        btnAdd.style.flex = '2';
        btnAdd.style.backgroundColor = '#0064d2';
        btnAdd.innerHTML = '<span>Add Film</span>';
        footer.insertBefore(btnAdd, footer.firstChild);

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        btnAdd.onclick = async () => {
            const title = dialog.querySelector('.txtTitle').value;
            const year = dialog.querySelector('.txtYear').value;
            const imdbId = dialog.querySelector('.txtImdb').value;
            const url = dialog.querySelector('.txtUrl').value;
            if (!title) return;
            cleanup();
            await addMovie(title, url, year, imdbId);
        };

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
        dialog.querySelector('.txtTitle').focus();
    }

    async function showOnboardingDialog() {
        const overlay = createOverlay();
        const dialog = createDialogBase('Welcome to MovieNight');
        dialog.classList.add('movieNightOnboardingDialog');
        dialog.style.minWidth = '0';
        const content = dialog.querySelector('.dialog-content');
        const footer = dialog.querySelector('.dialog-footer');

        content.innerHTML = `
            <div class="movieNightOnboardingGrid">
                <div class="movieNightField">
                    <label>Favorite genres</label>
                    <div class="movieNightChipGroup genre-chips"></div>
                </div>
                <div class="movieNightField">
                    <label>Preferred eras</label>
                    <div class="movieNightChipGroup era-chips"></div>
                </div>
                <div class="movieNightField movieNightOnboardingWide">
                    <label>Plot types</label>
                    <div class="movieNightChipGroup plot-chips"></div>
                    <textarea class="movieNightTextarea plot-input" placeholder="heist, mystery, road trip"></textarea>
                </div>
                <div class="movieNightField">
                    <label>Moods</label>
                    <div class="movieNightChipGroup mood-chips"></div>
                </div>
                <div class="movieNightField">
                    <label>Content types</label>
                    <div class="movieNightChipGroup type-chips"></div>
                </div>
                <div class="movieNightField movieNightOnboardingWide">
                    <label>Actors and directors</label>
                    <textarea class="movieNightTextarea people-input" placeholder="Christopher Nolan, Denis Villeneuve, Florence Pugh"></textarea>
                </div>
                <div class="movieNightField movieNightOnboardingWide">
                    <label>Recommendation style</label>
                    <select class="movieNightSelect recommendation-style">
                        <option value="BALANCED">Balanced</option>
                        <option value="QUALITY_FIRST">Quality first</option>
                        <option value="MOOD_FIRST">Mood first</option>
                        <option value="DISCOVERY">Discovery</option>
                        <option value="SIMILAR_TO_FAVORITES">Similar to favorites</option>
                    </select>
                </div>
            </div>
        `;

        const genres = [
            "Action",
            "Adventure",
            "Animation",
            "Comedy",
            "Crime",
            "Documentary",
            "Drama",
            "Fantasy",
            "Horror",
            "Mystery",
            "Romance",
            "Sci-Fi",
            "Thriller"
        ];
        const eras = ["1970s", "1980s", "1990s", "2000s", "2010s", "2020s"];
        const plotTypes = [
            "coming of age",
            "crime investigation",
            "family drama",
            "heist",
            "mind bending",
            "political intrigue",
            "quest",
            "road trip",
            "space adventure",
            "survival"
        ];
        const moods = [
            { label: "Tense", value: "tense" },
            { label: "Slow burn", value: "slow-burn" },
            { label: "Feel good", value: "feel-good" },
            { label: "Dark", value: "dark" },
            { label: "Romantic", value: "romantic" },
            { label: "Focused", value: "focused" }
        ];
        const types = [
            { label: "Movies", value: "FILM" },
            { label: "Series", value: "SERIES" },
            { label: "Episodes", value: "EPISODE" },
            { label: "Other", value: "OTHER" }
        ];

        const selections = {
            genres: new Set(),
            eras: new Set(),
            plotTypes: new Set(),
            moods: new Set(),
            types: new Set(["FILM"])
        };

        genres.forEach((genre) => createOnboardingChip(genre, content.querySelector('.genre-chips'), selections.genres));
        eras.forEach((era) => createOnboardingChip(era, content.querySelector('.era-chips'), selections.eras));
        plotTypes.forEach((plotType) => createOnboardingChip(plotType, content.querySelector('.plot-chips'), selections.plotTypes));
        moods.forEach((mood) => createOnboardingChip(mood, content.querySelector('.mood-chips'), selections.moods));
        types.forEach((type) => createOnboardingChip(type, content.querySelector('.type-chips'), selections.types));

        const btnSave = document.createElement('button');
        btnSave.className = 'emby-button raised button-submit';
        btnSave.style.flex = '2';
        btnSave.style.backgroundColor = '#0064d2';
        btnSave.innerHTML = '<span>Save & Start</span>';
        footer.insertBefore(btnSave, footer.firstChild);

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        btnSave.onclick = async () => {
            const customPlotTypes = splitOnboardingList(content.querySelector('.plot-input').value);
            const people = splitOnboardingList(content.querySelector('.people-input').value);
            const payload = {
                weightedGenres: Object.fromEntries([...selections.genres].map(g => [g, 5])),
                plotTypes: uniqueValues([...selections.plotTypes, ...customPlotTypes]),
                eras: [...selections.eras],
                castAndDirectors: people,
                moods: [...selections.moods],
                contentTypes: [...selections.types],
                likedFilmIds: [],
                dislikedFilmIds: [],
                libraryFilmIds: [],
                watchedFilmIds: [],
                recommendationStyle: content.querySelector('.recommendation-style').value
            };
            cleanup();
            await completeOnboarding(payload);
        };

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
    }

    function createOnboardingChip(option, container, selections) {
        const normalized = typeof option === 'string' ? { label: option, value: option } : option;
        const chip = document.createElement('button');
        chip.type = 'button';
        chip.className = 'movieNightChip';
        chip.textContent = normalized.label;
        chip.setAttribute('aria-pressed', selections.has(normalized.value) ? 'true' : 'false');
        if (selections.has(normalized.value)) {
            chip.classList.add('is-selected');
        }

        chip.onclick = () => {
            if (selections.has(normalized.value)) {
                selections.delete(normalized.value);
            } else {
                selections.add(normalized.value);
            }

            const selected = selections.has(normalized.value);
            chip.classList.toggle('is-selected', selected);
            chip.setAttribute('aria-pressed', selected ? 'true' : 'false');
        };
        container.appendChild(chip);
    }

    function splitOnboardingList(value) {
        return uniqueValues((value || '').split(/[,\n;]+/).map((item) => item.trim()).filter(Boolean));
    }

    function uniqueValues(values) {
        return [...new Set(values.map((value) => value.trim()).filter(Boolean))];
    }

    async function checkOnboarding() {
        if (window.movieNightOnboardingChecked) return;

        const userId = ApiClient.getCurrentUserId();
        if (!userId) return;
        window.movieNightOnboardingChecked = true;

        try {
            const prefs = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/Users/${userId}/Preferences`));
            if (!hasOnboardingPreferences(prefs)) {
                showOnboardingDialog();
            }
        } catch (err) {
            if (err.status === 404) showOnboardingDialog();
        }
    }

    function hasOnboardingPreferences(prefs) {
        if (!prefs) return false;

        return Boolean(
            Object.keys(prefs.weightedGenres || {}).length ||
            prefs.plotTypes?.length ||
            prefs.eras?.length ||
            prefs.castAndDirectors?.length ||
            prefs.moods?.length ||
            prefs.contentTypes?.length
        );
    }

    async function completeOnboarding(payload) {
        const userId = ApiClient.getCurrentUserId();
        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Users/${userId}/Onboarding`),
                data: JSON.stringify(payload),
                contentType: 'application/json'
            });
            showMsg('Welcome! Your preferences have been saved.');
        } catch (err) {
            showMsg('Failed to save onboarding preferences.');
        }
    }

    async function showRecommendation() {
        const userId = ApiClient.getCurrentUserId();
        try {
            const response = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/Users/${userId}/Recommendations`));
            const recommendations = normalizeRecommendations(response);

            if (recommendations && recommendations.length > 0) {
                showRecommendationsDialog(recommendations);
            } else {
                showMsg('No recommendations found at the moment.');
            }
        } catch (err) {
            console.error('Failed to get recommendations', err);
            showMsg('Failed to get recommendations. Check your API token and MovieNight status.');
        }
    }

    function normalizeRecommendations(response) {
        if (typeof response === 'string') {
            return JSON.parse(response);
        }

        if (Array.isArray(response)) {
            return response;
        }

        return response?.items || response?.recommendations || [];
    }

    function getRecommendationFilm(recommendation) {
        return recommendation?.film || recommendation || {};
    }

    function getRecommendationTitle(recommendation) {
        const film = getRecommendationFilm(recommendation);
        return film.title || recommendation?.title || 'Untitled';
    }

    function getRecommendationItemId(recommendation) {
        const film = getRecommendationFilm(recommendation);
        return recommendation?.jellyfinItemId || film.jellyfinItemId;
    }

    function getRecommendationWatchUrl(recommendation) {
        const itemId = getRecommendationItemId(recommendation);
        if (recommendation?.watchUrl) return recommendation.watchUrl;
        return itemId ? `${window.location.origin}/web/#/details?id=${encodeURIComponent(itemId)}` : null;
    }

    function getRecommendationPosterUrl(recommendation) {
        const itemId = getRecommendationItemId(recommendation);
        if (!itemId) return null;

        if (typeof ApiClient !== 'undefined' && ApiClient.getUrl) {
            return ApiClient.getUrl(`Items/${itemId}/Images/Primary`, {
                fillHeight: 330,
                fillWidth: 220,
                quality: 90
            });
        }

        return `/Items/${encodeURIComponent(itemId)}/Images/Primary?fillHeight=330&fillWidth=220&quality=90`;
    }

    function formatRecommendationScore(score) {
        return typeof score === 'number' ? `${Math.round(score * 100)}% match` : '';
    }

    function showRecommendationsDialog(recommendations) {
        const overlay = createOverlay();
        const dialog = createDialogBase('MovieNight Recommendations');
        dialog.style.width = 'min(760px, calc(100vw - 2em))';
        dialog.style.maxHeight = 'min(760px, calc(100vh - 2em))';
        dialog.style.display = 'flex';
        dialog.style.flexDirection = 'column';

        const content = dialog.querySelector('.dialog-content');
        const footer = dialog.querySelector('.dialog-footer');
        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        content.style.overflowY = 'auto';
        content.style.paddingRight = '.25em';
        content.style.marginBottom = '1em';
        footer.querySelector('.btnCancel').textContent = 'Close';

        const list = document.createElement('div');
        list.className = 'movieNightRecommendationList';
        content.replaceChildren(list);

        recommendations.forEach((recommendation) => {
            list.appendChild(createRecommendationRow(recommendation, cleanup));
        });

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
    }

    function createRecommendationRow(recommendation, cleanup) {
        const film = getRecommendationFilm(recommendation);
        const itemId = getRecommendationItemId(recommendation);
        const watchUrl = getRecommendationWatchUrl(recommendation);
        const posterUrl = getRecommendationPosterUrl(recommendation);

        const row = document.createElement('div');
        row.className = 'movieNightRecommendation';

        const poster = document.createElement('img');
        poster.className = 'movieNightRecommendationPoster';
        poster.alt = '';
        if (posterUrl) {
            poster.src = posterUrl;
        }
        poster.onerror = () => {
            poster.removeAttribute('src');
        };

        const body = document.createElement('div');
        body.className = 'movieNightRecommendationBody';

        const header = document.createElement('div');
        header.className = 'movieNightRecommendationHeader';

        const titleBlock = document.createElement('div');
        titleBlock.style.minWidth = '0';

        const title = document.createElement('div');
        title.className = 'movieNightRecommendationTitle';
        title.textContent = getRecommendationTitle(recommendation);
        titleBlock.appendChild(title);

        const meta = [film.releaseYear, ...(film.genres || [])].filter(Boolean).join(' · ');
        if (meta) {
            const metaEl = document.createElement('div');
            metaEl.className = 'movieNightRecommendationMeta';
            metaEl.textContent = meta;
            titleBlock.appendChild(metaEl);
        }

        const score = document.createElement('div');
        score.className = 'movieNightRecommendationScore';
        score.textContent = formatRecommendationScore(recommendation?.score);

        header.appendChild(titleBlock);
        if (score.textContent) header.appendChild(score);
        body.appendChild(header);

        if (film.description) {
            const description = document.createElement('div');
            description.className = 'movieNightRecommendationDescription';
            description.textContent = film.description;
            body.appendChild(description);
        }

        const reasons = recommendation?.reasons || [];
        if (reasons.length) {
            const reason = document.createElement('div');
            reason.className = 'movieNightRecommendationReason';
            reason.textContent = reasons.join(', ');
            body.appendChild(reason);
        }

        const actions = document.createElement('div');
        actions.className = 'movieNightRecommendationActions';

        if (watchUrl) {
            actions.appendChild(createRecommendationAction('Open', 'play_arrow', () => {
                cleanup();
                window.location.href = watchUrl;
            }));
        }

        if (itemId) {
            actions.appendChild(createRecommendationAction('Rate', 'star_rate', () => {
                cleanup();
                showRatingDialog(itemId);
            }));
            actions.appendChild(createRecommendationAction('Viewed', 'visibility', () => {
                cleanup();
                submitViewed(itemId);
            }));
        }

        body.appendChild(actions);
        row.appendChild(poster);
        row.appendChild(body);
        return row;
    }

    function createRecommendationAction(text, icon, onClick) {
        return createTextButton(text, 'movieNightRecommendationAction', (e) => {
            e.preventDefault();
            e.stopPropagation();
            onClick();
        }, icon);
    }

    async function addMovie(title, url, year, imdbId) {
        try {
            const data = { title, url };
            if (year) data.year = parseInt(year);
            if (imdbId) data.imdbId = imdbId;

            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Films`),
                data: JSON.stringify(data),
                contentType: 'application/json'
            });
            showMsg(`STRM file created for "${title}". Refresh your library to see it.`);
        } catch (err) {
            console.error('Failed to create movie', err);
            showMsg('Failed to create movie. Ensure STRM output path is configured.');
        }
    }

    async function triggerSync() {
        try {
            await ApiClient.ajax({ type: 'POST', url: ApiClient.getUrl(`MovieNight/Sync`) });
            showMsg('Library sync triggered!');
            setTimeout(updateSyncStatus, 2000);
        } catch (err) {
            showMsg('Failed to trigger sync.');
        }
    }

    async function updateSyncStatus() {
        const statusEl = document.querySelector('.movieNightSyncStatus');
        if (!statusEl) return;
        try {
            const state = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/SyncState`));
            const states = Array.isArray(state) ? state : [];
            const latest = states
                .map(s => s.lastSuccessfulSyncAt || s.lastSyncedAt)
                .filter(Boolean)
                .sort()
                .pop();
            if (latest) {
                statusEl.innerText = `Last sync: ${new Date(latest).toLocaleString()}`;
            }
        } catch (err) { /* ignore */ }
    }

    async function submitRating(itemId, score) {
        const userId = ApiClient.getCurrentUserId();
        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Users/${userId}/Ratings/Films/${itemId}`),
                data: JSON.stringify({ score: parseInt(score), note: 'From Jellyfin UI' }),
                contentType: 'application/json'
            });
            showMsg('Rating submitted to MovieNight!');
        } catch (err) {
            showMsg('Failed to submit rating.');
        }
    }

    async function submitViewed(itemId) {
        const userId = ApiClient.getCurrentUserId();
        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Users/${userId}/Library/Films/${itemId}/Viewed`),
                data: JSON.stringify({ watchedAt: new Date().toISOString() }),
                contentType: 'application/json'
            });
            showMsg('Marked as viewed in MovieNight!');
        } catch (err) {
            showMsg('Failed to mark as viewed.');
        }
    }

    let injectTimeout;
    let injectInFlight = false;
    let rerunAfterInject = false;
    let routeRetryTimeouts = [];
    const routeEventListeners = [];
    const patchedHistoryMethods = [];

    async function runInject() {
        if (injectInFlight) {
            rerunAfterInject = true;
            return;
        }

        injectInFlight = true;
        try {
            await injectUI();
        } catch (err) {
            console.error('MovieNight UI injection failed', err);
        } finally {
            injectInFlight = false;
            if (rerunAfterInject) {
                rerunAfterInject = false;
                scheduleInject();
            }
        }
    }

    function scheduleInject(delay = 100) {
        if (injectTimeout) return;
        injectTimeout = setTimeout(() => {
            injectTimeout = null;
            runInject();
        }, delay);
    }

    function scheduleRouteInject() {
        routeRetryTimeouts.forEach(clearTimeout);
        routeRetryTimeouts = ROUTE_RETRY_DELAYS_MS.map((delay) => {
            return setTimeout(() => runInject(), delay);
        });
    }

    function patchHistoryMethod(name) {
        const current = history[name];
        const original = current?._movieNightOriginal || current;
        if (typeof original !== 'function') return;

        history[name] = function () {
            const result = original.apply(this, arguments);
            scheduleRouteInject();
            return result;
        };
        history[name]._movieNightOriginal = original;
        patchedHistoryMethods.push(name);
    }

    function addRouteEventListener(name) {
        window.addEventListener(name, scheduleRouteInject);
        routeEventListeners.push(name);
    }

    patchHistoryMethod('pushState');
    patchHistoryMethod('replaceState');
    addRouteEventListener('hashchange');
    addRouteEventListener('popstate');
    addRouteEventListener('pageshow');

    const observer = new MutationObserver(() => scheduleInject());
    observer.observe(document.body, { childList: true, subtree: true });

    window.movieNightUiCleanup = () => {
        observer.disconnect();
        if (injectTimeout) clearTimeout(injectTimeout);
        routeRetryTimeouts.forEach(clearTimeout);
        routeEventListeners.forEach((name) => window.removeEventListener(name, scheduleRouteInject));
        patchedHistoryMethods.forEach((name) => {
            const original = history[name]?._movieNightOriginal;
            if (original) history[name] = original;
        });
        window.movieNightUiCleanup = null;
    };

    scheduleRouteInject();
})();
