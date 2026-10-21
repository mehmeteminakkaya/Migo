import { CATALOG_CACHE_SECONDS, MAX_CATALOG_BYTES, YOK_ATLAS_API } from "./config.js";
import { UserFacingError, json, readTextBody } from "./http.js";
import { clean, normalizeSearch } from "./text.js";

export async function educationSearch(request, ctx, type) {
  const query = clean(new URL(request.url).searchParams.get("q"), 80);
  if (query.length < 2) {
    return json({ items: [], source: "YÖK Atlas", total: 0 });
  }

  const isUniversity = type === "universities";
  const endpoint = isUniversity ? "universiteler" : "universite-programlar";
  const records = await getOfficialCatalog(`${YOK_ATLAS_API}/${endpoint}`, ctx);
  const nameKey = isUniversity ? "universiteAdi" : "birimGrupAdi";
  const idKey = isUniversity ? "universiteId" : "birimGrupId";
  const normalizedQuery = normalizeSearch(query);
  const matches = records
    .filter((item) => normalizeSearch(item?.[nameKey]).includes(normalizedQuery))
    .sort((left, right) => {
      const leftName = normalizeSearch(left?.[nameKey]);
      const rightName = normalizeSearch(right?.[nameKey]);
      const leftStarts = leftName.startsWith(normalizedQuery) ? 0 : 1;
      const rightStarts = rightName.startsWith(normalizedQuery) ? 0 : 1;
      return leftStarts - rightStarts || leftName.localeCompare(rightName, "tr-TR");
    });

  const items = matches.slice(0, 30).map((item) => ({
    id: String(item?.[idKey] ?? ""),
    name: clean(item?.[nameKey], 180),
    meta: isUniversity ? "Üniversite" : clean(item?.puanTuru, 20)
  })).filter((item) => item.id && item.name);

  return json(
    { items, source: "YÖK Atlas", total: matches.length },
    200,
    "public, max-age=300"
  );
}

export async function getOfficialCatalog(url, ctx) {
  const cache = caches.default;
  let response = await cache.match(url);
  if (!response) {
    const upstream = await fetch(url, {
      headers: {
        Accept: "application/json",
        Referer: "https://yokatlas.yok.gov.tr/",
        "User-Agent": "MigoCareerApp/1.0"
      }
    });
    if (!upstream.ok) throw new UserFacingError(`YÖK Atlas veri hatası: ${upstream.status}`);
    const contentLength = Number(upstream.headers.get("content-length") || 0);
    if (contentLength > MAX_CATALOG_BYTES) throw new UserFacingError("YÖK Atlas yanıtı beklenenden büyük.");

    response = new Response(upstream.body, {
      status: upstream.status,
      headers: upstream.headers
    });
    response.headers.set("cache-control", `public, max-age=${CATALOG_CACHE_SECONDS}`);
    ctx.waitUntil(cache.put(url, response.clone()));
  }

  const text = await readTextBody(response, MAX_CATALOG_BYTES);
  const parsed = JSON.parse(text);
  if (!Array.isArray(parsed)) throw new UserFacingError("YÖK Atlas yanıtı okunamadı.");
  return parsed;
}
