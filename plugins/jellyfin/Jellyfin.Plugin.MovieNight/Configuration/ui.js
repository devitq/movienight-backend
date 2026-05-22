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
        const dialog = createDialogBase('Welcome to MovieNight!');
        dialog.style.minWidth = '450px';
        const content = dialog.querySelector('.dialog-content');
        const footer = dialog.querySelector('.dialog-footer');

        content.innerHTML = `
            <p style="margin-bottom:1.5em; opacity:0.8; text-align:center;">Pick your preferences to get better recommendations.</p>
            <div style="margin-bottom:1.5em;">
                <label style="display:block; margin-bottom:0.6em; font-weight:600;">Favorite Genres</label>
                <div class="genre-chips" style="display:flex; flex-wrap:wrap; gap:0.5em;"></div>
            </div>
            <div style="margin-bottom:1.5em;">
                <label style="display:block; margin-bottom:0.6em; font-weight:600;">Preferred Eras</label>
                <div class="era-chips" style="display:flex; flex-wrap:wrap; gap:0.5em;"></div>
            </div>
            <div>
                <label style="display:block; margin-bottom:0.6em; font-weight:600;">Content Types</label>
                <div class="type-chips" style="display:flex; flex-wrap:wrap; gap:0.5em;"></div>
            </div>
        `;

        const genres = ["Action", "Comedy", "Drama", "Sci-Fi", "Horror", "Thriller", "Animation", "Documentary"];
        const eras = ["1980s", "1990s", "2000s", "2010s", "2020s"];
        const types = ["FILM", "SERIES"];

        const selections = { genres: new Set(), eras: new Set(), types: new Set() };

        const createChip = (text, container, type) => {
            const chip = document.createElement('div');
            chip.innerText = text;
            chip.style.cssText = 'padding:0.4em 1em; border-radius:2em; border:1px solid #444; cursor:pointer; font-size:0.9em; transition:all 0.2s;';
            chip.onclick = () => {
                if (selections[type].has(text)) {
                    selections[type].delete(text);
                    chip.style.backgroundColor = 'transparent';
                    chip.style.borderColor = '#444';
                } else {
                    selections[type].add(text);
                    chip.style.backgroundColor = '#0064d2';
                    chip.style.borderColor = '#0064d2';
                }
            };
            container.appendChild(chip);
        };

        genres.forEach(g => createChip(g, content.querySelector('.genre-chips'), 'genres'));
        eras.forEach(e => createChip(e, content.querySelector('.era-chips'), 'eras'));
        types.forEach(t => createChip(t, content.querySelector('.type-chips'), 'types'));

        const btnSave = document.createElement('button');
        btnSave.className = 'emby-button raised button-submit';
        btnSave.style.flex = '2';
        btnSave.style.backgroundColor = '#0064d2';
        btnSave.innerHTML = '<span>Save & Start</span>';
        footer.insertBefore(btnSave, footer.firstChild);

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        btnSave.onclick = async () => {
            const payload = {
                weightedGenres: Object.fromEntries([...selections.genres].map(g => [g, 5])),
                eras: [...selections.eras],
                contentTypes: [...selections.types]
            };
            cleanup();
            await completeOnboarding(payload);
        };

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
    }

    async function checkOnboarding() {
        if (window.movieNightOnboardingChecked) return;

        const userId = ApiClient.getCurrentUserId();
        if (!userId) return;
        window.movieNightOnboardingChecked = true;

        try {
            const prefs = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/Users/${userId}/Preferences`));
            if (!prefs || (!Object.keys(prefs.weightedGenres || {}).length && !prefs.eras?.length)) {
                showOnboardingDialog();
            }
        } catch (err) {
            if (err.status === 404) showOnboardingDialog();
        }
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
