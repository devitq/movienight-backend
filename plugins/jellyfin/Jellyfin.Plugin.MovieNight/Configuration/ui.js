(function () {
    const PLUGIN_ID = "42c72919-d6ff-4f62-bb8c-0fac39efafdb";

    function injectUI() {
        // 1. Inject "Recommend me a film" button in Library views
        const headerButtons = document.querySelector('.headerViewButtons');
        if (headerButtons && !document.querySelector('.btnMovieNightRecommend')) {
            const btn = document.createElement('button');
            btn.className = 'emby-button raised btnMovieNightRecommend';
            btn.innerHTML = '<span>Recommend Film</span>';
            btn.style.marginLeft = '1em';
            btn.onclick = showRecommendation;
            headerButtons.appendChild(btn);
        }

        // 2. Inject Rating UI in Item Details
        const detailButtons = document.querySelector('.itemDetailButtons');
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
                container.appendChild(label);

                const select = document.createElement('select');
                select.className = 'emby-select';
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
        return normalizeJellyfinId(params.get('id'));
    }

    function normalizeJellyfinId(value) {
        if (!value) {
            return null;
        }

        const normalized = value.replace(/-/g, '').toLowerCase();
        return /^[0-9a-f]{32}$/.test(normalized) ? normalized : null;
    }

    async function showRecommendation() {
        const userId = ApiClient.getCurrentUserId();
        try {
            const response = await ApiClient.getJSON(ApiClient.getUrl(`MovieNight/Users/${encodeURIComponent(userId)}/Recommendations`));
            const recommendations = typeof response === 'string' ? JSON.parse(response) : response;

            if (recommendations && recommendations.length > 0) {
                const rec = recommendations[0];
                const film = rec.film || rec;
                Dashboard.alert({
                    title: 'MovieNight Recommendation',
                    text: `How about watching: ${film.title}?\n\nReason: ${rec.reasons?.join(', ') || 'Based on your preferences'}`
                });
            } else {
                Dashboard.alert('No recommendations found at the moment.');
            }
        } catch (err) {
            console.error('Failed to get recommendations', err);
            Dashboard.alert('Failed to get recommendations from MovieNight.');
        }
    }

    async function submitRating(itemId, score) {
        if (score === "0") return;
        const userId = ApiClient.getCurrentUserId();
        try {
            await ApiClient.ajax({
                type: 'POST',
                url: ApiClient.getUrl(`MovieNight/Users/${encodeURIComponent(userId)}/Ratings/Films/${encodeURIComponent(itemId)}`),
                data: JSON.stringify({ score: parseInt(score), note: 'From Jellyfin UI' }),
                contentType: 'application/json'
            });
            Dashboard.alert('Rating submitted!');
        } catch (err) {
            console.error('Failed to submit rating', err);
            Dashboard.alert('Failed to submit rating to MovieNight.');
        }
    }

    let pendingInjection = false;
    const observer = new MutationObserver(() => {
        if (pendingInjection) {
            return;
        }

        pendingInjection = true;
        requestAnimationFrame(() => {
            pendingInjection = false;
            injectUI();
        });
    });
    observer.observe(document.body, { childList: true, subtree: true });

    // Initial call
    injectUI();
})();
