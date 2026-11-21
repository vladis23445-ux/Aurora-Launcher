# Aurora Visuals + Aurora Launcher

Клиентский визуальный мод для **Minecraft 1.21.11 / Fabric** и собственный лаунчер,
который ставит игру, Fabric, Fabric API, собирает мод и запускает всё это с оффлайн-аккаунтом.

Проверенные значения (по официальным API на 20.11.2026):

| Параметр | Значение |
|---|---|
| Minecraft | `1.21.11` (есть в манифесте Mojang) |
| Fabric Loader | `0.20.1` |
| Yarn | `1.21.11+build.6` |
| Fabric API | `0.141.6+1.21.11` |
| Fabric Loom | `1.19.4` |
| Java | 21 |

## Структура

```
Chat-4/
├─ aurora/                        # исходники мода (Gradle + Fabric Loom)
│  ├─ build.gradle, settings.gradle, gradle.properties
│  └─ src/main/
│     ├─ java/dev/aurora/…        # код мода
│     └─ resources/               # fabric.mod.json, aurora.mixins.json, иконка
├─ launcher/
│  ├─ src/                        # исходники лаунчера (C# / WinForms, .NET 8)
│  └─ dist/AuroraLauncher.exe     # готовый лаунчер
└─ README.md
```

## Быстрый старт

1. Запустить `launcher/dist/AuroraLauncher.exe`.
2. «Создать оффлайн» → ник.
3. «УСТАНОВИТЬ ВСЁ» — лаунчер сам:
   - скачает Temurin JDK 21 (если Java нет);
   - скачает ванильный 1.21.11 (client.jar, 107 библиотек, ~4 тыс. ассетов);
   - поставит Fabric-профиль `fabric-loader-0.20.1-1.21.11`;
   - положит Fabric API и соберёт/скопирует `aurora-1.0.0.jar` в `mods/`.
4. «ИГРАТЬ». В игре Right Shift — ClickGUI.

Данные лаунчера: `%APPDATA%\AuroraLauncher\` (конфиг `config.json`, JDK, Gradle, папка игры `game/`).

## Сборка мода вручную

```bat
cd aurora
gradlew build          :: если есть wrapper
:: или
gradle build
```

Результат: `aurora/build/libs/aurora-1.0.0.jar`.

## Управление

| Клавиша | Действие |
|---|---|
| Right Shift | открыть/закрыть ClickGUI |
| клик по модулю | включить/выключить |
| клик по названию | выбрать модуль → настройки справа |
| bind | назначить клавишу модуля |

## Модули

- **TargetESP** — наклонённое вращающееся кольцо + свечение + бокс цели.
- **TargetHUD** — плашка: голова скина, ник, HP, броня, дистанция.
- **Trails** — лента-шлейф с градиентом и затуханием.
- **WorldParticles** — светлячки/снежинки (процедурные, без аллокаций на кадр).
- **HitParticles** — всплеск частиц при ударе (крит — другой цвет).
- **CustomHand** — позиция/масштаб/наклон рук.
- **DamageNumbers** — цифры урона и лечения с проекцией world → screen.
- **JumpCircles** — расширяющиеся кольца на земле.
- **Watermark / ArmorHUD / Keystrokes** — HUD.

## Важно про API рендера

Minecraft меняет рендер-API почти каждый минорный релиз. Все версионно-зависимые
вызовы вынесены в две точки, чтобы правка была точечной:

- `aurora/src/main/java/dev/aurora/render/Render3D.java` — `RenderPipelines.POSITION_COLOR`,
  `Tessellator.begin`, `BufferRenderer.drawWithGlobalProgram`;
- `aurora/src/main/java/dev/aurora/mixin/HeldItemRendererMixin.java` — сигнатура `renderItem`.

Если сборка упадёт на имени метода/константы — правится только соответствующий файл.
Остальной код (математика, GUI, логика модулей) от версии не зависит: 2D рисуется
только через `DrawContext.fill`, 3D — через собственные буферы.

## Ограничения

- Только оффлайн-аккаунты (вход Microsoft не реализован — нужен Azure client id).
- Мод клиентский и косметический; на серверах с запретом ESP/визуалов его использовать нельзя.

## Сборка

### Мод
```powershell
cd aurora
$env:JAVA_HOME = "<путь к JDK 25>"   # Loom 1.19.4 требует Gradle 9.8+ и JDK 25
./gradlew build                       # jar → aurora/build/libs/aurora-1.0.0.jar
```
Поставить jar в `mods` профиля Fabric 1.21.11 (нужен Fabric API).

### Лаунчер
```powershell
cd launcher/src
dotnet publish AuroraLauncher.csproj -c Release -r win-x64 --self-contained true `
  -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o ../dist
```
На выходе `launcher/dist/AuroraLauncher.exe` — самодостаточный файл, .NET ставить не нужно.

## Возможности

**Мод (ClickGUI — правый Shift):**
- Render: TargetEsp, TargetHud, TargetInfo, Trails, WorldParticles, HitParticles, BlockHighlight (заливка + анимации), ItemEsp, HeldShader, FlatItems, CustomHand, ItemSwitch, Fullbright, Zoom, Optimization
- HUD: Watermark, ArmorHud, Keystrokes, SmoothHotbar, DamageNumbers
- Misc: AutoSprint
- Конфиги: профили в `config/aurora/*.json`

**Лаунчер:** установка ванили, Fabric, Fabric API, сборка/установка мода, скачивание JDK,
оффлайн-аккаунты, консоль, Discord Rich Presence, окно «Сеть» (адрес для LAN-игры).

## Ограничения

- Всё «шейдерное» (3D-эффекты, blur, заливка модели предмета) рисуется программно через `DrawContext`:
  собственный `RenderPipeline` для настоящих шейдеров не используется.
- Настоящий туннель «как Radmin VPN» требует сервера-посредника с внешним IP — в проект не входит.