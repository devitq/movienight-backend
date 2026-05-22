(function () {
    const PLUGIN_ID = "42c72919-d6ff-4f62-bb8c-0fac39efafdb";

    function getAlert() {
        if (typeof Dashboard !== 'undefined' && Dashboard.alert) {
            return (options) => Dashboard.alert(options);
        }
        return (options) => {
            const msg = typeof options === 'string' ? options : (options.text || options.title);
            alert(msg);
        };
    }

    const showMsg = getAlert();

    function createTextButton(text, className, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.setAttribute('is', 'emby-button');
        btn.className = `emby-button raised ${className}`;
        btn.style.margin = '0.5em';
        btn.style.padding = '0.4em 1em';
        const span = document.createElement('span');
        span.textContent = text;
        btn.appendChild(span);
        btn.onclick = onClick;
        return btn;
    }

    function createIconButton(icon, title, className, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.setAttribute('is', 'emby-button');
        btn.className = `button-flat detailButton emby-button ${className}`;
        btn.title = title;
        btn.setAttribute('aria-label', title);
        const content = document.createElement('div');
        content.className = 'detailButton-content';
        const iconSpan = document.createElement('span');
        iconSpan.className = 'material-icons detailButton-icon';
        iconSpan.setAttribute('aria-hidden', 'true');
        iconSpan.textContent = icon;
        content.appendChild(iconSpan);
        btn.appendChild(content);
        btn.onclick = onClick;
        return btn;
    }

    function injectUI() {
        // 1. Item Detail Page
        const detailButtons = document.querySelector('.mainDetailButtons');
        if (detailButtons) {
            const itemId = getItemIdFromUrl();
            if (itemId) {
                // MovieNight Rating
                if (!detailButtons.querySelector('.btnMovieNightRate')) {
                    const rateBtn = createIconButton('star_rate', 'Rate on MovieNight', 'btnMovieNightRate', (e) => {
                        e.preventDefault(); e.stopPropagation(); showRatingDialog(itemId);
                    });
                    insertInDetailRow(detailButtons, rateBtn);
                }
                // Mark Viewed in MovieNight
                if (!detailButtons.querySelector('.btnMovieNightMarkViewed')) {
                    const viewedBtn = createIconButton('visibility', 'Mark Viewed in MovieNight', 'btnMovieNightMarkViewed', (e) => {
                        e.preventDefault(); e.stopPropagation(); submitViewed(itemId);
                    });
                    insertInDetailRow(detailButtons, viewedBtn);
                }
            }
        }

        // 2. Library Pages - Add text buttons to toolbar
        const toolBar = document.querySelector('.libraryPage:not(.itemDetailPage) .flex.align-items-center.justify-content-center.focuscontainer-x');
        if (toolBar) {
            if (!toolBar.querySelector('.btnMovieNightRecommend')) {
                toolBar.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', (e) => {
                    e.preventDefault(); showRecommendation();
                }));
            }
            if (!toolBar.querySelector('.btnMovieNightAddMovie')) {
                toolBar.appendChild(createTextButton('Add Movie (STRM)', 'btnMovieNightAddMovie', (e) => {
                    e.preventDefault(); showAddMovieDialog();
                }));
            }
        }

        // 3. Home Page - Prepend a MovieNight section
        const homeSections = document.querySelector('.sections.homeSectionsContainer');
        if (homeSections && !document.querySelector('.movieNightHomeButtons')) {
            const section = document.createElement('div');
            section.className = 'verticalSection movieNightHomeButtons';
            section.style.padding = '0 var(--sidePadding)';
            section.innerHTML = `
                <div class="sectionTitleContainer" style="display:flex; align-items:center; justify-content:space-between;">
                    <h2 class="sectionTitle">MovieNight</h2>
                    <span class="movieNightSyncStatus" style="font-size:0.8em; opacity:0.7;"></span>
                </div>
                <div class="movieNightBtnContainer" style="display:flex; flex-wrap:wrap; margin-top:0.5em;"></div>
            `;
            const btnContainer = section.querySelector('.movieNightBtnContainer');
            btnContainer.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', showRecommendation));
            btnContainer.appendChild(createTextButton('Add Movie (STRM)', 'btnMovieNightAddMovie', showAddMovieDialog));
            btnContainer.appendChild(createTextButton('Sync Library', 'btnMovieNightSync', triggerSync));

            homeSections.insertBefore(section, homeSections.firstChild);
            updateSyncStatus();
        }
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
        overlay.style.backgroundColor = 'var(--dialog-backdrop, rgba(0,0,0,0.6))';
        overlay.style.position = 'fixed';
        overlay.style.top = '0'; overlay.style.left = '0'; overlay.style.right = '0'; overlay.style.bottom = '0';
        overlay.style.backdropFilter = 'blur(4px)';
        return overlay;
    }

    function createDialogBase(title) {
        const dialog = document.createElement('div');
        dialog.className = 'dialog';
        dialog.style.position = 'fixed';
        dialog.style.top = '50%'; dialog.style.left = '50%';
        dialog.style.transform = 'translate(-50%, -50%)';
        dialog.style.zIndex = '99999';
        dialog.style.padding = '2em';
        dialog.style.minWidth = '320px';
        dialog.style.backgroundColor = 'var(--theme-body-background)';
        dialog.style.borderRadius = '1em';
        dialog.style.color = 'var(--theme-body-color)';
        dialog.style.boxShadow = '0 10px 25px rgba(0,0,0,0.5)';
        dialog.style.border = '1px solid var(--theme-light-btn-border-color, transparent)';

        dialog.innerHTML = `
            <h2 class="dialogTitle">${title}</h2>
            <div class="dialog-content"></div>
            <div class="dialog-footer">
                <button is="emby-button" class="emby-button button-flat btnCancel">Cancel</button>
            </div>
        `;
        const content = dialog.querySelector('.dialog-content');
        content.style.margin = '1.5em 0';
        const footer = dialog.querySelector('.dialog-footer');
        footer.style.display = 'flex';
        footer.style.gap = '1em';
        dialog.querySelector('.btnCancel').style.flex = '1';
        return dialog;
    }

    async function showRatingDialog(itemId) {
        const overlay = createOverlay();
        const dialog = createDialogBase('Rate on MovieNight');
        const content = dialog.querySelector('.dialog-content');
        const grid = document.createElement('div');
        grid.className = 'rating-grid';
        grid.style.display = 'grid';
        grid.style.gridTemplateColumns = 'repeat(5, 1fr)';
        grid.style.gap = '0.6em';
        content.appendChild(grid);

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        for (let i = 1; i <= 10; i++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.setAttribute('is', 'emby-button');
            btn.className = 'emby-button raised';
            btn.innerText = i;
            btn.style.padding = '0.8em 0';
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
        const dialog = createDialogBase('Add Movie (STRM)');
        const content = dialog.querySelector('.dialog-content');
        const footer = dialog.querySelector('.dialog-footer');

        content.innerHTML = `
            <div style="margin-bottom:1em;">
                <label style="display:block; margin-bottom:0.4em; font-size:0.9em; opacity:0.8;">Movie Title</label>
                <input type="text" class="emby-input txtTitle" style="width:100%; box-sizing:border-box;" placeholder="e.g. Inception">
            </div>
            <div>
                <label style="display:block; margin-bottom:0.4em; font-size:0.9em; opacity:0.8;">Stream URL (Optional)</label>
                <input type="text" class="emby-input txtUrl" style="width:100%; box-sizing:border-box;" placeholder="http://...">
            </div>
        `;

        const btnAdd = document.createElement('button');
        btnAdd.className = 'emby-button raised button-submit';
        btnAdd.style.flex = '2';
        btnAdd.innerHTML = '<span>Add Film</span>';
        footer.insertBefore(btnAdd, footer.firstChild);

        const cleanup = () => { if (overlay.parentNode) document.body.removeChild(overlay); };

        btnAdd.onclick = async () => {
            const title = dialog.querySelector('.txtTitle').value;
            const url = dialog.querySelector('.txtUrl').value;
            if (!title) return;
            cleanup();
            await addMovie(title, url);
        };

        dialog.querySelector('.btnCancel').onclick = cleanup;
        dialog.querySelector('.btnCancel').style.width = '100%';
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };
        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
        dialog.querySelector('.txtTitle').focus();
    }

    async function showRecommendation() {
        const userId = ApiClient.getCurrentUserId();
        try {
            const response = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/Users/${userId}/Recommendations`));
            const recommendations = typeof response === 'string' ? JSON.parse(response) : response;

            if (recommendations && recommendations.length > 0) {
                const rec = recommendations[0];
                const film = rec.film || rec;
                showMsg({
                    title: 'MovieNight Recommendation',
                    text: `How about watching: ${film.title}?\n\nReason: ${rec.reasons?.join(', ') || 'Based on your preferences'}`
                });
            } else {
                showMsg('No recommendations found at the moment.');
            }
        } catch (err) {
            console.error('Failed to get recommendations', err);
            showMsg('Failed to get recommendations. Check your API token and MovieNight status.');
        }
    }

    async function addMovie(title, url) {
        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Films`),
                data: JSON.stringify({ title, url }),
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
            if (state && state.lastSyncAt) {
                statusEl.innerText = `Last sync: ${new Date(state.lastSyncAt).toLocaleString()}`;
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

    let timeout;
    const throttledInject = () => {
        if (timeout) return;
        timeout = setTimeout(() => {
            injectUI();
            timeout = null;
        }, 100);
    };

    const observer = new MutationObserver(throttledInject);
    observer.observe(document.body, { childList: true, subtree: true });

    injectUI();
})();
