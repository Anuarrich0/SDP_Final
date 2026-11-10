# Спрайты

- `loki.png` — лист 192×288: три кадра в строке, размер кадра 64×96.
  Строки сверху вниз: вниз, вверх, вправо. Для движения влево игра зеркально
  отражает правый профиль. Кадр 0 — стойка, 1 и 2 — разные шаги.
- `guard.png` — лист 192×288: три кадра в строке, кадр 64×96.
  Строки: вниз, вверх, вправо; влево отражается в игре.
  Кадр 0 — стойка, 1 и 2 — разные шаги с движением рук и ног.
- `interior.png` — лист 192×48: пол, потёртый пол, стена, стена с лампой.
  Каждый тайл занимает 48×48.
- `objects.png` — лист 144×48: активный портал, закрытый портал и TemPad.
  Кадры по 48×48; у предметов прозрачный фон.

Локи собран из редактируемых фигур вручную в Canva:
https://www.canva.com/design/DAHaVhqU250/prtAP13o427Q_y8PBYn5og/edit
Страницы исходника: 1 — стойка вниз; 2–4 — вправо; 5–6 — шаги вниз;
7–9 — вверх. PNG экспортированы из Canva, затем уменьшены в пять раз,
белый фон заменён прозрачностью и кадры упакованы в один лист.
Рисунки не генерируются при запуске. Охранник и интерьер собраны из фигур
в отдельных дизайнах Canva двумя агентами параллельно:
- Охранник: https://www.canva.com/design/DAHaWmyVLGU/k0eYDZKpNqujDgGgQJNF9g/edit
  Страницы экспорта в порядке кадров атласа: 1, 2, 3, 7, 8, 9, 4, 5, 6.
- Интерьер: https://www.canva.com/design/DAHaWk6-39M/npsVpeJ0YMUslvqCra_ObQ/edit
  Страницы 1–4 совпадают с порядком тайлов в листе, 5–7 — предметы.

`CharacterAnimation` выбирает направление и шаг по реальному перемещению Локи и охранников.
При остановке показывает стойку и сохраняет направление; на паузе кадр замирает.
Иллюзия использует тот же рисунок с голубым оттенком и запоминает направление
в момент создания. Все текстуры загружаются один раз, без сглаживания.

В детализированной версии добавлены окантовка и складки одежды Локи, блики
на шлемах, панели брони охранника, швы пола, решётки, вентиляция и консоль.
PNG экспортированы из редактируемых рисунков Canva и упакованы в листы текстур.


## Новые охотники TVA

- `b15.png` — B-15: https://www.canva.com/design/DAHaatmNnsY/F4zB1WRkKTQN2KzEbNcv7Q/edit
- `d90.png` — D-90: https://www.canva.com/design/DAHaatGvseY/h0xojDwEym_4oCZWobN_8A/edit
- `c20.png` — C-20: https://www.canva.com/design/DAHaao8juTw/O--biCeF1Jwjn5qiMJBwsA/edit
- `x5.png` — X-5: https://www.canva.com/design/DAHaavnakI4/6929v8_tJf5fSGvPMOQhvQ/edit

Каждый лист 192×288, кадр 64×96. Строки: вниз, вверх, вправо.
В каждой строке: стойка, первый шаг, второй шаг. Для движения влево
правый профиль отражается по горизонтали.

У каждого персонажа своя открытая голова, причёска и черты лица.
Детальная форма TVA и движения рук/ног используют общую основу охранника.
Исходники состоят из редактируемых фигур Canva. Страницы дизайнов:
1–3 — вниз, 4–6 — вправо, 7–9 — вверх.

Листы подготовлены для следующих изменений игровой логики.
Текущий обычный охранник продолжает использовать guard.png.

## Фон меню и интерфейс

`menu-background.png` — отдельный фон стартового меню, создан встроенным imagegen.
Варианты с Локи генератор отклонил. В итоговом изображении нет персонажей:
это интерьер временного агентства с оранжевым порталом. Спрайты персонажей не менялись.

Промпт итогового фона:

> Original cinematic environment illustration for a time-travel adventure game main-menu background. Empty monumental retro-futurist time bureaucracy office, no people or characters. Warm amber circular time portal on the right, brass elevators, vintage analog clocks, teak desks, paper case files, green glass computer consoles, Art Deco brutalist architecture, distant delicate luminous branching timeline. Beautiful polished painterly realism, emerald green and warm burnt orange lighting, rich depth and volumetric haze. Wide landscape 16:9. The left 45 percent is dark and quiet negative space reserved for separately rendered game title and menu buttons. No text, no logo, no watermarks, no interface elements. Opaque background.

`ui/interface.fnt` и `ui/interface.png` — растровый шрифт интерфейса с кириллицей
на основе установленного Segoe UI. `ui/title.fnt` и `ui/title.png` — крупные буквы
названия на основе Segoe UI Light. Шрифты уже подготовлены, дополнительных
библиотек для их загрузки не требуется.
