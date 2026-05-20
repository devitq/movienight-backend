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
        btn.is = 'emby-button';
        btn.className = `emby-button raised ${className}`;
        btn.style.margin = '0.5em';
        btn.style.padding = '0.4em 1em';
        btn.innerHTML = `<span>${text}</span>`;
        btn.onclick = onClick;
        return btn;
    }

    function createIconButton(icon, title, className, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.is = 'emby-button';
        btn.className = `button-flat detailButton emby-button ${className}`;
        btn.title = title;
        btn.innerHTML = `
            <div class="detailButton-content">
                <span class="material-icons detailButton-icon ${icon}" aria-hidden="true"></span>
            </div>
        `;
        btn.onclick = onClick;
        return btn;
    }

    function injectUI() {
        // 1. Item Detail Page - Add icon button for rating
        const detailButtons = document.querySelector('.mainDetailButtons');
        if (detailButtons && !document.querySelector('.btnMovieNightRate')) {
            const itemId = getItemIdFromUrl();
            if (itemId) {
                const rateBtn = createIconButton('star_rate', 'Rate on MovieNight', 'btnMovieNightRate', (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    showRatingDialog(itemId);
                });
                const moreBtn = detailButtons.querySelector('.btnMoreCommands');
                if (moreBtn) {
                    detailButtons.insertBefore(rateBtn, moreBtn);
                } else {
                    detailButtons.appendChild(rateBtn);
                }
            }
        }

        // 2. Library Pages - Add text buttons to toolbar
        const toolBar = document.querySelector('.libraryPage:not(.itemDetailPage) .flex.align-items-center.justify-content-center.focuscontainer-x');
        if (toolBar && !document.querySelector('.btnMovieNightRecommend')) {
             toolBar.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', (e) => {
                 e.preventDefault();
                 showRecommendation();
             }));
             toolBar.appendChild(createTextButton('Add Movie (STRM)', 'btnMovieNightAddMovie', (e) => {
                 e.preventDefault();
                 promptAddMovie();
             }));
        }

        // 3. Home Page - Prepend a MovieNight section
        const homeSections = document.querySelector('.sections.homeSectionsContainer');
        if (homeSections && !document.querySelector('.movieNightHomeButtons')) {
            const section = document.createElement('div');
            section.className = 'verticalSection movieNightHomeButtons';
            section.style.padding = '0 var(--sidePadding)';
            section.innerHTML = '<h2 class="sectionTitle">MovieNight</h2><div class="movieNightBtnContainer" style="display:flex; flex-wrap:wrap;"></div>';
            const btnContainer = section.querySelector('.movieNightBtnContainer');
            btnContainer.appendChild(createTextButton('Recommend Film', 'btnMovieNightRecommend', showRecommendation));
            btnContainer.appendChild(createTextButton('Add Movie (STRM)', 'btnMovieNightAddMovie', promptAddMovie));
            homeSections.insertBefore(section, homeSections.firstChild);
        }
    }

    function getItemIdFromUrl() {
        const queryString = window.location.hash.includes('?') ? window.location.hash.split('?')[1] : window.location.search;
        const params = new URLSearchParams(queryString);
        return params.get('id') || params.get('itemId');
    }

    async function showRatingDialog(itemId) {
        const overlay = document.createElement('div');
        overlay.className = 'dialogBackdrop dialogBackdropOpened';
        overlay.style.zIndex = '99998';
        overlay.style.backgroundColor = 'rgba(0,0,0,0.5)';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.right = '0';
        overlay.style.bottom = '0';

        const dialog = document.createElement('div');
        dialog.className = 'dialog';
        dialog.style.position = 'fixed';
        dialog.style.top = '50%';
        dialog.style.left = '50%';
        dialog.style.transform = 'translate(-50%, -50%)';
        dialog.style.zIndex = '99999';
        dialog.style.padding = '2em';
        dialog.style.minWidth = '250px';
        dialog.style.backgroundColor = '#222';
        dialog.style.borderRadius = '1em';
        dialog.style.color = 'white';

        dialog.innerHTML = `
            <h2 style="margin-top:0; text-align:center;">Rate on MovieNight</h2>
            <div class="rating-grid" style="display:grid; grid-template-columns:repeat(5, 1fr); gap:0.5em; margin:1.5em 0;"></div>
            <button is="emby-button" class="emby-button button-flat btnCancel" style="width:100%; color: white;">Cancel</button>
        `;

        const grid = dialog.querySelector('.rating-grid');
        for (let i = 1; i <= 10; i++) {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.is = 'emby-button';
            btn.className = 'emby-button raised';
            btn.innerText = i;
            btn.style.padding = '0.5em';
            btn.onclick = async () => {
                cleanup();
                await submitRating(itemId, i);
            };
            grid.appendChild(btn);
        }

        const cleanup = () => {
            if (overlay.parentNode) document.body.removeChild(overlay);
        };

        dialog.querySelector('.btnCancel').onclick = cleanup;
        overlay.onclick = (e) => { if (e.target === overlay) cleanup(); };

        overlay.appendChild(dialog);
        document.body.appendChild(overlay);
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
            showMsg('Failed to get recommendations from MovieNight.');
        }
    }

    async function promptAddMovie() {
        const title = prompt("Enter movie title:");
        if (!title) return;

        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Films`),
                data: JSON.stringify({ title: title }),
                contentType: 'application/json'
            });
            showMsg(`STRM file created for "${title}". Refresh your library to see it.`);
        } catch (err) {
            console.error('Failed to create movie', err);
            showMsg('Failed to create movie. Check plugin configuration and logs.');
        }
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
            showMsg('Rating submitted!');
        } catch (err) {
            console.error('Failed to submit rating', err);
            showMsg('Failed to submit rating to MovieNight.');
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
