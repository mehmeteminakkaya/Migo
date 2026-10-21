import { UserFacingError, json } from "./http.js";
import { callNvidia, withAiCoachRules } from "./nvidia.js";
import { clampInt, clean, parseJsonObject, stringList } from "./text.js";

export async function careerAnalysis(body, env) {
  const profile = body?.profile || {};
  const interests = stringList(body?.interests, 8);
  const skills = stringList(body?.skills, 8);
  if (!clean(profile.university, 160) || !clean(profile.department, 160)) {
    return json({ error: "Üniversite veya bölüm bilgisi eksik." }, 400);
  }

  const system = `Sen Migo adlı kariyer planlama uygulamasının Türkçe kariyer danışmanısın.
Öğrencinin üniversite, bölüm ve sınıf bilgisini analiz et. Bölümün akademik yapısını, ilişkili kariyerleri,
öğrencinin bu aşamada geliştirmesi mantıklı olan odak alanlarını ve yetkinlikleri çıkar.
Kullanıcının açıkça söylemediği bir beceriyi kazanılmış yetkinlik gibi sunma; bunları "önerilen gelişim alanı" olarak değerlendir.
Gerçekçi ve uygulanabilir öneriler ver. Kesin başarı, iş veya maaş garantisi verme.
Yalnızca geçerli JSON döndür. Şema:
{"headline":"", "summary":"", "inferred_interests":[""], "inferred_skills":[""], "strengths":[""], "careers":[{"title":"","reason":""}], "next_steps":[""]}.
Tam olarak 5 önerilen ilgi/odak alanı, 5 geliştirilmesi önerilen yetkinlik, 3 potansiyel güçlü yön,
3 kariyer ve 4 somut adım üret. Yanıt tamamen Türkçe olsun.`;
  const student = {
    fullName: clean(profile.fullName, 100),
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    selfReportedInterests: interests,
    selfReportedSkills: skills
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bu öğrenci için kariyer analizi hazırla: ${JSON.stringify(student)}` }
  ], 1400, 0.35);

  const parsed = parseJsonObject(output);
  return json(parsed);
}

export async function careerDetail(body, env) {
  const profile = body?.profile || {};
  const branch = body?.branch || {};
  const university = clean(profile.university, 160);
  const department = clean(profile.department, 160);
  const branchTitle = clean(branch.title, 120);
  if (!university || !department || !branchTitle) {
    return json({ error: "Üniversite, bölüm veya branş bilgisi eksik." }, 400);
  }

  const system = `Sen Migo adlı kariyer planlama uygulamasının Türkçe kariyer danışmanısın.
Öğrencinin üniversite/bölüm/sınıf bilgisiyle seçtiği kariyer dalı için ayrıntılı bir künye hazırla.
Kesin iş, maaş veya işe alım garantisi verme; maaş bir tahmindir, bunu salary_note alanında belirt.
Düzenlenmiş meslekler için gerçek yasal/lisans şartlarını yaz, uydurma.
Yalnızca geçerli JSON döndür. Şema:
{"title":"","overview":"","roles":[""],"work_areas":[""],"example_employers":[""],
"salary_range":"","salary_note":"","hiring_outlook":"","demand_note":"",
"requirements":[""],"daily_work":[""],"first_90_days":[""]}.
Tam olarak 3 rol, 3 çalışma alanı, 3 örnek işveren türü, 4 gereksinim, 3 günlük iş maddesi
ve 3 ilk 90 gün adımı üret. Yanıt tamamen Türkçe olsun.`;
  const student = {
    university,
    department,
    classLevel: clean(profile.classLevel, 30),
    branchTitle,
    branchReason: clean(branch.reason, 500)
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bu kariyer dalı için künye hazırla: ${JSON.stringify(student)}` }
  ], 900, 0.35);

  const parsed = parseJsonObject(output);
  const result = {
    title: clean(parsed?.title, 120) || branchTitle,
    overview: clean(parsed?.overview, 500),
    roles: stringList(parsed?.roles, 6),
    work_areas: stringList(parsed?.work_areas, 6),
    example_employers: stringList(parsed?.example_employers, 6),
    salary_range: clean(parsed?.salary_range, 120),
    salary_note: clean(parsed?.salary_note, 200),
    hiring_outlook: clean(parsed?.hiring_outlook, 40),
    demand_note: clean(parsed?.demand_note, 200),
    requirements: stringList(parsed?.requirements, 8),
    daily_work: stringList(parsed?.daily_work, 6),
    first_90_days: stringList(parsed?.first_90_days, 6)
  };
  if (!result.overview || result.roles.length < 2 || result.requirements.length < 2) {
    throw new UserFacingError("AI branş detayını eksik oluşturdu.");
  }
  return json(result);
}

export async function actionPlan(body, env) {
  const targetCareer = clean(body?.targetCareer, 120);
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }
  const profile = body?.profile || {};
  const branch = body?.branch || {};
  const completedTasks = stringList(body?.completedTasks, 20);

  const system = `Sen Migo uygulamasındaki Türkçe kariyer koçusun.
Öğrenciye hedef kariyeri için tam 4 haftalık, uygulanabilir bir eylem planı hazırla.
Bilgi uydurma; gerçekçi ve ölçülebilir görevler ver. İş veya başarı garantisi verme.

Yazım tonu: öğrenciye doğrudan "sen" diyerek, sıcak ama gösterişsiz bir dille yaz —
bir arkadaşının sana ne yapman gerektiğini anlattığı gibi. Kısa, somut cümleler kur.
"Katkı sağlayacak", "değerli bir fırsat sunacak", "potansiyelini keşfet" gibi şişirilmiş,
kalıp AI ifadelerinden kaçın. Her başlık ve açıklama okuyanın "bunu bugün yapabilirim"
demesini sağlamalı; soyut değil, iş tanımı kadar net olsun. objective ve outcome alanları
"neden önemli" sorusuna tek cümlede cevap versin.

Yalnızca geçerli JSON döndür. Şema:
{"title":"","objective":"","duration_days":30,"weeks":[
{"number":1,"title":"","focus":"","outcome":"","actions":[
{"title":"","description":"","duration_minutes":30,"deliverable":""}]}]}.
Tam olarak 4 hafta üret, her haftada tam olarak 4 eylem olsun. Her eylem süresi 15-90 dakika arasında olsun.
Yanıt tamamen Türkçe olsun.`;
  const student = {
    targetCareer,
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    branchReason: clean(branch.reason, 500),
    branchRequirements: stringList(branch.requirements, 8),
    completedTasks
  };
  const output = await callNvidia(env, [
    { role: "system", content: withAiCoachRules(system, body) },
    { role: "user", content: `Bu öğrenci için 30 günlük eylem planı hazırla: ${JSON.stringify(student)}` }
  ], 1700, 0.4);

  const parsed = parseJsonObject(output);
  const weeksRaw = Array.isArray(parsed?.weeks) ? parsed.weeks.slice(0, 4) : [];
  const weeks = weeksRaw.map((week, weekIndex) => {
    const actionsRaw = Array.isArray(week?.actions) ? week.actions.slice(0, 4) : [];
    return {
      number: weekIndex + 1,
      title: clean(week?.title, 120),
      focus: clean(week?.focus, 240),
      outcome: clean(week?.outcome, 220),
      actions: actionsRaw.map((action, actionIndex) => ({
        id: `w${weekIndex + 1}_a${actionIndex + 1}`,
        title: clean(action?.title, 140),
        description: clean(action?.description, 320),
        duration_minutes: clampInt(action?.duration_minutes, 15, 90, 30),
        deliverable: clean(action?.deliverable, 180)
      }))
    };
  });

  const result = {
    title: clean(parsed?.title, 160) || `${targetCareer} için 30 günlük rota`,
    objective: clean(parsed?.objective, 420),
    duration_days: clampInt(parsed?.duration_days, 7, 60, 30),
    weeks
  };
  if (
    !result.objective ||
    result.weeks.length !== 4 ||
    result.weeks.some((week) => week.actions.length < 4)
  ) {
    throw new UserFacingError("AI 30 günlük planı eksik oluşturdu.");
  }
  return json(result);
}

export async function dailyInsight(body, env) {
  const targetCareer = clean(body?.targetCareer, 120);
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }
  const profile = body?.profile || {};
  const dateKey = clean(body?.dateKey, 20);

  const system = `Sen Migo uygulamasındaki Türkçe günlük kariyer koçusun.
Öğrenciye bugün için kısa, motive edici ve uygulanabilir bir öneri ver. Kesin başarı garantisi verme.
Yalnızca geçerli JSON döndür. Şema: {"title":"","message":"","action_label":"","coach_prompt":""}.
title en fazla 6 kelime, message en fazla 2 cümle, action_label somut ve kısa bir eylem cümlesi,
coach_prompt bu eylemi küçük adımlara bölmesini isteyen tek cümlelik bir koç istemi olsun.
Yanıt tamamen Türkçe olsun.`;
  const student = {
    targetCareer,
    university: clean(profile.university, 160),
    department: clean(profile.department, 160),
    classLevel: clean(profile.classLevel, 30),
    nextTask: clean(body?.nextTask, 160),
    completionPercent: clampInt(body?.completionPercent, 0, 100, 0)
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: `Bugün için kısa bir kariyer içgörüsü hazırla: ${JSON.stringify(student)}` }
  ], 350, 0.5);

  const parsed = parseJsonObject(output);
  const result = {
    date_key: dateKey,
    title: clean(parsed?.title, 80),
    message: clean(parsed?.message, 320),
    action_label: clean(parsed?.action_label, 120),
    coach_prompt: clean(parsed?.coach_prompt, 280)
  };
  if (!result.title || !result.message || !result.action_label) {
    throw new UserFacingError("AI bugünün içgörüsünü eksik oluşturdu.");
  }
  return json(result);
}

export async function coach(body, env) {
  const question = clean(body?.question, 1000);
  if (!question) return json({ error: "Soru boş olamaz." }, 400);

  const targetCareer = clean(body?.targetCareer, 120);
  const department = clean(body?.department, 120);
  const skills = stringList(body?.skills, 8);
  const system = `Sen Migo uygulamasındaki Türkçe AI kariyer koçusun.
Kullanıcının hedefi ${targetCareer}; bölümü ${department}; yetkinlikleri ${skills.join(", ")}.
Kısa, somut ve uygulanabilir cevap ver. İş, maaş veya başarı garantisi verme. Güncel bilgi bilmiyorsan uydurma.
Uygunsa en fazla 4 maddelik eylem listesiyle bitir.`;
  const history = Array.isArray(body?.history) ? body.history.slice(-8).map((item) => ({
    role: item?.role === "assistant" ? "assistant" : "user",
    content: clean(item?.content, 2000)
  })).filter((item) => item.content) : [];

  const answer = await callNvidia(env, [
    { role: "system", content: withAiCoachRules(system, body) },
    ...history,
    { role: "user", content: question }
  ], 700, 0.45);
  return json({ answer });
}

export async function cvAnalysis(body, env) {
  const cvText = clean(body?.cvText, 12_000);
  const targetCareer = clean(body?.targetCareer, 120);
  const department = clean(body?.department, 160);

  if (cvText.length < 80) {
    return json({ error: "CV metni analiz için çok kısa." }, 400);
  }
  if (!targetCareer) {
    return json({ error: "Hedef kariyer bilgisi eksik." }, 400);
  }

  const system = [
    "Sen Migo uygulamasındaki uzman Türkçe CV danışmanısın.",
    "CV'yi adayın hedeflediği role göre değerlendir. Sadece metinde bulunan bilgilere dayan;",
    "olmayan deneyim, teknoloji, başarı, sayı veya kurum uydurma. Ayrımcı değerlendirme yapma.",
    "Kişisel iletişim bilgilerini cevabında tekrar etme. Net, uygulanabilir ve dürüst ol.",
    "Yalnızca geçerli JSON döndür. Şema:",
    '{"score":0,"summary":"","strengths":[""],"improvements":[""],"missing_keywords":[""],"rewritten_bullets":[""]}.',
    "Score 0-100 arasında tam sayı olsun. Tam olarak 3 güçlü yön, 4 öncelikli geliştirme,",
    "en fazla 6 eksik anahtar kelime ve 3 yeniden yazım örneği üret.",
    "Yeniden yazım örneklerinde eksik ölçülebilir bilgiler için [sonuç], [oran] veya [süre] gibi yer tutucular kullan."
  ].join("\n");

  const candidate = {
    targetCareer,
    department,
    cvText
  };
  const output = await callNvidia(env, [
    { role: "system", content: system },
    { role: "user", content: "Bu CV metnini analiz et: " + JSON.stringify(candidate) }
  ], 1300, 0.25);

  const parsed = parseJsonObject(output);
  const score = Number.isFinite(Number(parsed?.score))
    ? Math.max(0, Math.min(100, Math.round(Number(parsed.score))))
    : 0;
  const result = {
    score,
    summary: clean(parsed?.summary, 600),
    strengths: stringList(parsed?.strengths, 3),
    improvements: stringList(parsed?.improvements, 4),
    missing_keywords: stringList(parsed?.missing_keywords, 6),
    rewritten_bullets: stringList(parsed?.rewritten_bullets, 3)
  };
  if (!result.summary || result.strengths.length === 0 || result.improvements.length === 0) {
    throw new UserFacingError("AI CV analizini eksik oluşturdu.");
  }
  return json(result);
}
