# Nodus BDUI CMP

Репозиторий для CMP-песочницы Nodus и будущей библиотеки runtime для рендера backend-driven JSON-схем форм.

## Структура

- `nodus-app/` — текущее Compose Multiplatform sandbox-приложение
- `nodus-app/sharedUI/` — общий UI-слой приложения
- `nodus-app/androidApp/` — Android launcher
- `nodus-app/desktopApp/` — Desktop launcher
- `nodus-app/webApp/` — Web launcher
- `nodus-app/iosApp/` — iOS launcher
- `nodus-parser/` — отдельный runtime-модуль для парсинга и рендера JSON-схем

В этом репозитории sandbox-приложение используется как тестовый клиент для сценария:

`Проект -> Контракт -> Версия -> Экран -> Построить -> Рендер формы по JSON-схеме`.

## Текущий фокус

План работ зафиксирован в основном workspace Nodus:

- `tasks/prd_06_cmp-sandbox-and-json-form-runtime/`
- `doc/nodus-bdui-cmp-work-report.md`

Целевое направление:

1. добавить selector screen для выбора `project / contract / version / screen`;
2. добавить экран рендера по кнопке `Построить`;
3. выделить внутри приложения reusable CMP runtime library для parse/validate/render pipeline.

Текущее состояние MVP:

1. selector screen реализован;
2. build-to-render flow работает;
3. по умолчанию sandbox использует `FakeRegistryGateway`;
4. в кодовой базе уже есть `HttpRegistryGateway` для подключения к реальному backend registry API.

## Запуск

Все команды выполняются из каталога `nodus-app/`.

### Android

```bash
./gradlew :androidApp:assembleDebug
```

### Desktop

```bash
./gradlew :desktopApp:run
```

### Web

```bash
./gradlew :webApp:jsBrowserDevelopmentRun
```

### iOS

Откройте `nodus-app/iosApp/iosApp.xcodeproj` в Xcode и запустите стандартную конфигурацию.

## Примечания

1. Корневой `.gitignore` настроен для Gradle/KMP, IDE и platform build artifacts.
2. Вложенный `nodus-app/README.MD` описывает базовые команды sandbox-приложения.
3. Реализация runtime-библиотеки вынесена в отдельный каталог `nodus-parser/` на уровне репозитория.
