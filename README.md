# Lumen Stream Mobile

**PT-BR** · [English below](#english)

O Lumen Stream Mobile leva o [Lumen Stream](https://github.com/Lumen-Connection/lumen-stream) para o Android: cole um link (ou use "Compartilhar" de qualquer app), escolha o formato e o arquivo entra numa biblioteca local pesquisável. **Livre, aberto e sem rastreamento** — sem contas, sem telemetria, tudo no seu aparelho.

## Recursos

- Download de vídeo/áudio de sites compatíveis com **NewPipe Extractor** e **yt-dlp** (fallback automático com Python embutido)
- Fila de downloads resumível com limite de velocidade e notificações de progresso
- Conversão para MP3/Opus e merge de qualidade máxima via **ffmpeg embarcado**
- Legendas (srt) e playlists completas
- Biblioteca local com thumbnails, busca, tags e favoritos + player integrado
- Salva em Movies/Música (MediaStore) ou numa pasta à sua escolha
- Interface em PT-BR e EN

## Instalação

Baixe o APK da sua arquitetura (ou o `universal`) na página de [Releases](../../releases) e instale. O Android pode exibir um aviso do Play Protect por ser um app fora da Play Store — é esperado para apps distribuídos livremente.

## Build

Requisitos: JDK 17 e Android SDK (API 35).

```
./gradlew assembleDebug
```

## Licença

AGPL-3.0, a mesma do [Lumen Stream](https://github.com/Lumen-Connection/lumen-stream) desktop. O app embarca NewPipe Extractor (GPLv3) e ffmpeg (GPL), compatíveis com a AGPL.

---

## English

Lumen Stream Mobile brings [Lumen Stream](https://github.com/Lumen-Connection/lumen-stream) to Android: paste a link (or use "Share" from any app), pick a format, and the file lands in a searchable local library. **Free, open, tracking-free** — no accounts, no telemetry, everything on your device.

### Features

- Video/audio downloads from sites supported by **NewPipe Extractor** and **yt-dlp** (automatic fallback, embedded Python)
- Resumable download queue with rate limiting and progress notifications
- MP3/Opus conversion and best-quality merging via **embedded ffmpeg**
- Subtitles (srt) and full playlists
- Local library with thumbnails, search, tags and favorites + built-in player
- Saves to Movies/Music (MediaStore) or a folder of your choice
- PT-BR and EN interface

### Install

Grab the APK for your architecture (or the `universal` one) from [Releases](../../releases). Android may show a Play Protect warning for sideloaded apps — expected for freely distributed software.

### License

AGPL-3.0, the same as the [Lumen Stream](https://github.com/Lumen-Connection/lumen-stream) desktop app. It embeds NewPipe Extractor (GPLv3) and ffmpeg (GPL), both AGPL-compatible.
