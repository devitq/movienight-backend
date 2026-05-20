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

    function injectUI() {
        const headerButtons = document.querySelector('.headerViewButtons, .view-library .content-primary, .home-section .sectionTitleContainer');

        if (headerButtons) {
             if (!document.querySelector('.btnMovieNightRecommend')) {
                const btn = document.createElement('button');
                btn.className = 'emby-button raised btnMovieNightRecommend';
                btn.innerHTML = '<span>Recommend Film</span>';
                btn.style.marginLeft = '1em';
                btn.onclick = showRecommendation;
                headerButtons.appendChild(btn);
            }

            if (!document.querySelector('.btnMovieNightAddMovie')) {
                const btn = document.createElement('button');
                btn.className = 'emby-button raised btnMovieNightAddMovie';
                btn.innerHTML = '<span>Add Movie (STRM)</span>';
                btn.style.marginLeft = '1em';
                btn.onclick = promptAddMovie;
                headerButtons.appendChild(btn);
            }
        }

        const detailButtons = document.querySelector('.itemDetailButtons, .itemDetailsButtons');
        if (detailButtons && !document.querySelector('.movieNightRatingContainer')) {
            const itemId = getItemIdFromUrl();
            if (itemId) {
                const container = document.createElement('div');
                container.className = 'movieNightRatingContainer';
                container.style.display = 'inline-flex';
                container.style.alignItems = 'center';
                container.style.marginLeft = '1em';

                const label = document.createElement('span');
                label.innerText = 'MovieNight: ';
                label.style.marginRight = '0.5em';
                container.appendChild(label);

                const select = document.createElement('select');
                select.className = 'emby-select';
                select.style.padding = '0.2em';
                for (let i = 0; i <= 10; i++) {
                    const opt = document.createElement('option');
                    opt.value = i;
                    opt.innerText = i === 0 ? 'Rate...' : i;
                    select.appendChild(opt);
                }
                select.onchange = (e) => submitRating(itemId, e.target.value);
                container.appendChild(select);

                detailButtons.appendChild(container);
            }
        }
    }

    function getItemIdFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('id') || params.get('itemId');
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
        if (score === "0") return;
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

    const observer = new MutationObserver(injectUI);
    observer.observe(document.body, { childList: true, subtree: true });

    injectUI();
})();
