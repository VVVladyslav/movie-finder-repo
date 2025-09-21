/**
 app.js — minimal frontend for Movie Finder.
 Uses native browser validation, fetches search (/api/movies), details (/api/movies/{id}),
 and favorites (/api/favorites) to render a responsive grid + details modal.
 Debounced search, simple pager, status/error messages; favorites synced per session and reflected in UI.
 */

(() => {
    "use strict";

    const API = {
        search: (q, p) => `/api/movies?query=${encodeURIComponent(q)}&page=${p}`,
        details: (id) => `/api/movies/${encodeURIComponent(id)}`,
        favorites: {
            list: () => `/api/favorites`,
            add: () => `/api/favorites`,
            remove: (id) => `/api/favorites/${encodeURIComponent(id)}`
        }
    };

    const PLACEHOLDER_POSTER =
        "data:image/svg+xml;utf8," +
        encodeURIComponent(
            `<svg xmlns="http://www.w3.org/2000/svg" width="400" height="600">
         <rect width="100%" height="100%" fill="#e5e7eb"/>
         <text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle"
               font-family="Arial, sans-serif" font-size="18" fill="#6b7280">No image</text>
       </svg>`
        );

    const state = {
        query: "",
        page: 1,
        total: 0,
        pageSizeHint: 20,
        lastResponseItemsCount: 0,
        results: [],
        favorites: new Map()
    };

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
    const escapeHtml = (s) =>
        String(s ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    const escapeAttr = (s) => escapeHtml(s).replaceAll('"', "&quot;");

    const form = $("#searchForm");
    const input = $("#queryInput");
    const statusArea = $("#statusArea");
    const grid = $("#results");
    const prevPageBtn = $("#prevPage");
    const nextPageBtn = $("#nextPage");
    const pageInfo = $("#pageInfo");
    const favList = $("#favoritesList");
    const favCount = $("#favoritesCount");

    const modalEl = $("#detailsModal");
    const modalClose = $("#modalClose");
    const modalContent = $("#modalContent");

    function setStatus(msg, busy = false) {
        if (!statusArea) return;
        statusArea.textContent = msg || "";
        statusArea.setAttribute("aria-busy", busy ? "true" : "false");
    }

    function updatePager() {
        pageInfo.textContent = `Page ${state.page}`;
        const canPrev = state.page > 1;
        let canNext = true;
        if (state.total > 0 && state.lastResponseItemsCount > 0) {
            const perPage = state.lastResponseItemsCount || state.pageSizeHint;
            const lastPage = Math.max(1, Math.ceil(state.total / perPage));
            canNext = state.page < lastPage;
        } else {
            canNext = state.lastResponseItemsCount > 0;
        }
        prevPageBtn.disabled = !canPrev;
        nextPageBtn.disabled = !canNext;
    }

    const isFav = (id) => state.favorites.has(Number(id));

    async function loadFavorites() {
        try {
            const resp = await fetch(API.favorites.list(), { headers: { Accept: "application/json" } });
            if (!resp.ok) throw new Error(`Favorites load failed (${resp.status})`);
            const arr = await resp.json();
            state.favorites.clear();
            for (const f of Array.isArray(arr) ? arr : []) {
                if (f && f.id != null) state.favorites.set(Number(f.id), f);
            }
            renderFavorites();
            markFavoriteButtonsInResults();
        } catch (e) {
            console.warn(e);
        }
    }

    async function addFavorite(movie) {
        try {
            const resp = await fetch(API.favorites.add(), {
                method: "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body: JSON.stringify({
                    id: movie.id,
                    title: movie.title ?? null,
                    year: movie.year ?? null,
                    posterUrl: movie.posterUrl ?? null
                })
            });
            if (!resp.ok) throw new Error(`Add favorite failed (${resp.status})`);
            await loadFavorites();
        } catch (e) {
            setStatus(`Failed to add to favorites. ${e.message || ""}`, false);
            setTimeout(() => setStatus("", false), 2500);
        }
    }

    async function removeFavorite(id) {
        try {
            const resp = await fetch(API.favorites.remove(id), { method: "DELETE" });
            if (!resp.ok) throw new Error(`Remove favorite failed (${resp.status})`);
            await loadFavorites();
        } catch (e) {
            setStatus(`Failed to remove from favorites. ${e.message || ""}`, false);
            setTimeout(() => setStatus("", false), 2500);
        }
    }

    function renderFavorites() {
        favList.innerHTML = "";
        const items = Array.from(state.favorites.values());
        favCount.textContent = String(items.length);
        if (items.length === 0) {
            favList.innerHTML = `<p class="muted">No favorites yet.</p>`;
            return;
        }
        const frag = document.createDocumentFragment();
        for (const f of items) {
            const item = document.createElement("div");
            item.className = "fav-item";
            item.innerHTML = `
        <img class="fav-thumb" src="${escapeAttr(f.posterUrl || PLACEHOLDER_POSTER)}" alt="${escapeAttr(f.title || "Poster")}" />
        <div class="fav-meta">
          <div class="fav-title" title="${escapeAttr(f.title || "")}">${escapeHtml(f.title || "Untitled")}</div>
          <div class="fav-year">${escapeHtml(f.year || "")}</div>
        </div>
        <button class="fav-remove" aria-label="Remove from favorites" data-id="${Number(f.id)}">✕</button>
      `;
            item.querySelector(".fav-remove").addEventListener("click", () => removeFavorite(f.id));
            frag.appendChild(item);
        }
        favList.appendChild(frag);
    }

    function renderResults(items) {
        state.results = Array.isArray(items) ? items : [];
        grid.innerHTML = "";
        if (state.results.length === 0) {
            grid.innerHTML = `<p class="muted">No results.</p>`;
            return;
        }
        const frag = document.createDocumentFragment();
        for (const m of state.results) {
            const id = Number(m.id);
            const card = document.createElement("article");
            card.className = "card";
            card.innerHTML = `
        <div class="card-media">
          <img src="${escapeAttr(m.posterUrl || PLACEHOLDER_POSTER)}" alt="${escapeAttr(m.title || "Poster")}" loading="lazy">
          <button class="btn-fav ${isFav(id) ? "is-active" : ""}" title="${isFav(id) ? "Remove from favorites" : "Add to favorites"}" data-id="${id}">
            ${isFav(id) ? "★" : "☆"}
          </button>
        </div>
        <div class="card-body">
          <div class="card-title" title="${escapeAttr(m.title || "")}">${escapeHtml(m.title || "Untitled")}</div>
          <div class="card-meta">${escapeHtml(m.year || "")}</div>
          <div class="card-actions">
            <button class="btn btn-details" data-id="${id}">Details</button>
          </div>
        </div>
      `;
            card.querySelector(".btn-details").addEventListener("click", () => openDetails(id));
            card.querySelector(".btn-fav").addEventListener("click", () => toggleFavoriteFromList(id));
            frag.appendChild(card);
        }
        grid.appendChild(frag);
    }

    function markFavoriteButtonsInResults() {
        $$(".btn-fav", grid).forEach((btn) => {
            const id = Number(btn.getAttribute("data-id"));
            const active = isFav(id);
            btn.classList.toggle("is-active", active);
            btn.textContent = active ? "★" : "☆";
            btn.title = active ? "Remove from favorites" : "Add to favorites";
        });
    }

    async function toggleFavoriteFromList(id) {
        const movie = state.results.find((x) => Number(x.id) === Number(id));
        if (!movie) {
            if (isFav(id)) {
                await removeFavorite(id);
            } else {
                try {
                    const d = await fetchDetailsRaw(id);
                    await addFavorite({ id: d.id, title: d.title, year: d.year, posterUrl: d.posterUrl });
                } catch {
                    setStatus("Unable to toggle favorite.", false);
                    setTimeout(() => setStatus("", false), 2500);
                }
            }
            markFavoriteButtonsInResults();
            return;
        }
        if (isFav(id)) {
            await removeFavorite(id);
        } else {
            await addFavorite({ id: movie.id, title: movie.title, year: movie.year, posterUrl: movie.posterUrl });
        }
        markFavoriteButtonsInResults();
    }

    async function doSearch(page = 1) {
        if (!input.checkValidity()) { input.reportValidity(); return; }
        state.query = (input.value || "").trim();
        state.page = page;
        setStatus("Loading…", true);
        try {
            const resp = await fetch(API.search(state.query, state.page), { headers: { Accept: "application/json" } });
            if (!resp.ok) throw await asError(resp, "Search failed");
            const data = await resp.json();
            const items = Array.isArray(data.items) ? data.items : [];
            state.total = Number.isFinite(data.total) ? data.total : 0;
            state.lastResponseItemsCount = items.length;
            if (state.page === 1 && items.length > 0) state.pageSizeHint = items.length;
            renderResults(items);
            updatePager();
            setStatus(items.length ? "" : "No results.", false);
        } catch (e) {
            setStatus(e.message || "Search failed.", false);
        }
    }

    async function fetchDetailsRaw(id) {
        const resp = await fetch(API.details(id), { headers: { Accept: "application/json" } });
        if (!resp.ok) throw await asError(resp, "Details failed");
        return resp.json();
    }

    async function openDetails(id) {
        setStatus("Loading…", true);
        try {
            const d = await fetchDetailsRaw(id);
            renderDetailsModal(d);
            showModal();
        } catch (e) {
            setStatus(e.message || "Failed to load details.", false);
        } finally {
            setStatus("", false);
        }
    }

    function renderDetailsModal(d) {
        const title = escapeHtml(d.title || "Untitled");
        const year = escapeHtml(d.year || "");
        const runtime = d.runtime ? `${d.runtime} min` : "";
        const genres = (d.genres || []).join(", ");
        const actors = (d.actors || []).join(", ");
        const plot = escapeHtml(d.plot || "No overview available.");
        const rating = typeof d.rating === "number" && !isNaN(d.rating) ? d.rating.toFixed(1) : "";
        const id = Number(d.id);

        modalContent.innerHTML = `
      <div class="details">
        <div class="poster-wrap">
          <img class="poster" src="${escapeAttr(d.posterUrl || PLACEHOLDER_POSTER)}" alt="${title} poster" />
        </div>
        <div class="meta">
          <h3 id="modalTitle" class="title">${title}</h3>
          <p class="sub">
            ${[year, runtime, rating ? `Rating: ${rating}` : ""].filter(Boolean).join(" • ")}
          </p>
          ${genres ? `<div class="block"><strong>Genres:</strong> ${escapeHtml(genres)}</div>` : ""}
          ${actors ? `<div class="block"><strong>Cast:</strong> ${escapeHtml(actors)}</div>` : ""}
          <div class="block"><strong>Overview:</strong> ${plot}</div>
          <div class="actions">
            <button id="favToggleInModal" class="btn ${isFav(id) ? "is-active" : ""}" data-id="${id}">
              ${isFav(id) ? "★ Remove from Favorites" : "☆ Add to Favorites"}
            </button>
          </div>
          <p class="legal">This product uses the TMDB API but is not endorsed or certified by TMDB.</p>
        </div>
      </div>
    `;

        const favBtn = $("#favToggleInModal", modalContent);
        favBtn?.addEventListener("click", async () => {
            if (isFav(id)) {
                await removeFavorite(id);
            } else {
                await addFavorite({ id, title: d.title ?? null, year: d.year ?? null, posterUrl: d.posterUrl ?? null });
            }
            favBtn.classList.toggle("is-active", isFav(id));
            favBtn.textContent = isFav(id) ? "★ Remove from Favorites" : "☆ Add to Favorites";
            markFavoriteButtonsInResults();
        });
    }

    function showModal() {
        modalEl.hidden = false;
        document.body.style.overflow = "hidden";
    }
    function closeModal() {
        modalEl.hidden = true;
        document.body.style.overflow = "";
        modalContent.innerHTML = "";
    }
    modalClose?.addEventListener("click", closeModal);
    modalEl?.addEventListener("click", (e) => {
        if (e.target && e.target.classList.contains("modal-backdrop")) closeModal();
    });
    document.addEventListener("keydown", (e) => {
        if (!modalEl.hidden && e.key === "Escape") closeModal();
    });

    async function asError(resp, fallback) {
        let msg = fallback;
        try {
            const t = await resp.text();
            if (t) {
                try {
                    const o = JSON.parse(t);
                    msg = o?.message || o?.error || o?.detail || fallback;
                } catch {
                    msg = t || fallback;
                }
            }
        } catch {}
        return new Error(msg + ` (${resp.status})`);
    }

    let debounceTimer = null;
    function debounceSearch() {
        clearTimeout(debounceTimer);
        if (!input.checkValidity()) return;
        debounceTimer = setTimeout(() => doSearch(1), 300);
    }

    form?.addEventListener("submit", (e) => {
        e.preventDefault();
        if (!input.checkValidity()) { input.reportValidity(); return; }
        doSearch(1);
    });
    input?.addEventListener("input", debounceSearch);
    prevPageBtn?.addEventListener("click", () => state.page > 1 && doSearch(state.page - 1));
    nextPageBtn?.addEventListener("click", () => doSearch(state.page + 1));

    (async function init() {
        await loadFavorites();
        const params = new URLSearchParams(location.search);
        const q0 = params.get("q");
        if (q0 && q0.trim().length >= 2) {
            input.value = q0.trim();
            await doSearch(1);
        }
        input?.focus();
    })();
})();
