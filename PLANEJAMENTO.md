# Lumen Stream Mobile — Planejamento e Handoff

> **Propósito deste arquivo**: documento de planejamento e de retomada. Se o agente que estava executando for interrompido (falta de crédito, sessão encerrada), qualquer outro agente de IA ou pessoa deve conseguir continuar o trabalho lendo este arquivo. **Mantenha a seção "Estado atual" atualizada ao final de cada etapa concluída.**

---

## 1. Contexto

O [Lumen Stream Desktop](https://github.com/Lumen-Connection/lumen-stream) é um app **Rust** (egui/eframe, SQLite, yt-dlp + ffmpeg) de download e organização de mídia: fila resumível com rate-limit, legendas, playlists, metadata Spotify, galeria local com thumbnails e mini-player, tags, favoritos, PT/EN, **zero telemetria, tudo local**.

Módulos do desktop (referência para portar semântica): `config/`, `db/`, `download/`, `ui/`, `app.rs`, `queue.rs`, `notify.rs`, `paths.rs`.

**Objetivo**: criar o **Lumen Stream Mobile** (Android) em `d:\HubLumen\LumenStreamMobile`, mantendo a filosofia local-first e sem tracking.

Ecossistema Lumen Connection: Hub (Next.js, em `d:\HubLumen\HubdaLumenConnection`), lumen-music (C++, em `d:\HubLumen\lumen-music`), lumen-stream (Rust desktop, só no GitHub). A pasta `d:\HubLumen\LumenStreamFiles` é a biblioteca de mídia do usuário (saída do app desktop), **não** é código.

## 2. Decisões acordadas com o usuário (entrevistas — NÃO reabrir sem necessidade)

| Tema | Decisão |
|---|---|
| Papel | **Híbrido**: downloader standalone; integração/sync com desktop fica para a **fase 2** |
| Plataforma | **Android primeiro** (iOS fora do escopo por ora) |
| Distribuição | **APK direto via GitHub Releases** (Play Store descartada; F-Droid é pós-MVP) |
| Público | Comunidade open-source |
| Stack | **Kotlin nativo + Jetpack Compose** (Material 3) |
| Engine de extração | **NewPipe Extractor primeiro, yt-dlp (Python embutido) como fallback — AMBOS já no MVP** |
| Processamento | **FFmpeg embarcado** (merge DASH, conversão mp3/opus) — via módulo ffmpeg do youtubedl-android |
| Features MVP | Download vídeo/áudio + fila; biblioteca/galeria + player; legendas + playlists; metadata Spotify |
| Armazenamento | **MediaStore por padrão + pasta configurável via SAF** |
| Entrada de links | Share sheet (intent SEND) + colar manual + detecção de clipboard |
| Player | **Foreground apenas** no MVP (background/MediaSession/PiP ficam para 1.x) |
| Min SDK | **API 26 (Android 8.0)**; compileSdk/targetSdk atual (35) |
| Repo/CI | Novo repo **`Lumen-Connection/lumen-stream-mobile`** + GitHub Actions gerando APK assinado em tag |
| Idiomas | **PT-BR + EN** desde o MVP (`values/strings.xml` EN + `values-pt-rBR/`) |

## 3. Arquitetura

Projeto Android Gradle (Kotlin DSL), módulo único `app/`, pacote raiz `com.lumenconnection.stream`:

```
com.lumenconnection.stream/
├─ config/      → DataStore Preferences (pasta destino, rate limit, idioma, extractor preferido)
├─ db/          → Room (SQLite): entidades media, downloads, tags, favoritos
├─ extractor/   → interface MediaExtractor
│   ├─ NewPipeExtractorImpl   → lib TeamNewPipe/NewPipeExtractor (JitPack); exige implementar
│   │                           a classe Downloader (OkHttp) e NewPipe.init(...)
│   └─ YtDlpExtractorImpl     → lib yausername/youtubedl-android (JitPack): módulos
│                               :library (Python+yt-dlp) e :ffmpeg; suporta atualizar yt-dlp em runtime
├─ download/    → DownloadService (Foreground Service + coroutines): fila resumível,
│                 notificação de progresso, rate limiting (semântica do queue.rs do desktop)
├─ media/       → escrita via MediaStore (padrão) ou SAF; merge/conversão via ffmpeg
├─ metadata/    → resolução Spotify p/ nomes limpos (portar lógica do src/download do desktop)
├─ ui/          → Compose: Home (link/fila), Library (grade+busca+tags+favoritos),
│                 Player (Media3/ExoPlayer, foreground), Settings
└─ share/       → Activity alvo do share sheet + leitura de clipboard ao abrir
```

Notas técnicas:
- **Fallback**: NewPipe tenta primeiro (leve/rápido); se falhar ou site não suportado → yt-dlp.
- NewPipeExtractor requer **core library desugaring** e Java 11+ target.
- Dependências via **JitPack** (`maven { url = "https://jitpack.io" }`).
- **Sem analytics/telemetria de nenhum tipo.**
- Licença: NewPipe Extractor é GPLv3 e ffmpeg é GPL → o app deve ser **GPLv3** (verificar licença do desktop e alinhar).

## 4. Fases e checklist

### Fase 0 — Bootstrap
- [ ] 0.1 Toolchain local (ver seção 6): JDK 17, Android cmdline-tools + SDK, Gradle
- [ ] 0.2 Scaffold do projeto Android (Kotlin DSL, minSdk 26, compileSdk 35, Compose BOM)
- [ ] 0.3 `git init` + primeiro commit
- [ ] 0.4 Criar repo `Lumen-Connection/lumen-stream-mobile` via `gh` (público) e push
- [ ] 0.5 GitHub Actions: workflow build + APK assinado em tag de release (espelhar padrão do desktop)
- [ ] 0.6 README PT/EN + LICENSE (GPLv3)

### Fase 1 — MVP
- [ ] 1.1 Room DB + entidades (media, downloads, tags, favorites)
- [ ] 1.2 Camada extractor: interface + NewPipeExtractorImpl + YtDlpExtractorImpl + init do ffmpeg
- [ ] 1.3 DownloadService (foreground) com fila resumível, notificações, rate limit
- [ ] 1.4 Escrita MediaStore + opção SAF nas configurações
- [ ] 1.5 UI Home: colar link, clipboard, escolha formato/qualidade, fila com progresso
- [ ] 1.6 Share sheet target (ACTION_SEND de YouTube/X/etc.)
- [ ] 1.7 Biblioteca: grade com thumbnails, busca, tags, favoritos
- [ ] 1.8 Player foreground (Media3/ExoPlayer) vídeo e áudio
- [ ] 1.9 Legendas (srt/vtt) e playlists
- [ ] 1.10 Metadata Spotify (nomes limpos de áudio)
- [ ] 1.11 i18n PT-BR + EN completo; release v0.1.0 com APK no GitHub

### Fase 1.x — Pós-MVP (backlog)
Áudio em background (MediaSession), PiP, transcrição, edição avançada de tags, F-Droid.

### Fase 2 — Integração com desktop
Servidor HTTP local no repo Rust do desktop + descoberta mDNS; mobile acessa biblioteca/fila remota na mesma rede Wi-Fi. Desenhar protocolo só quando o standalone estiver sólido.

## 5. Riscos conhecidos

- **Tamanho do APK**: Python (yt-dlp) + ffmpeg ≈ +60–80 MB → usar ABI splits no release.
- **Play Protect** pode alertar sideload → documentar no README.
- **Licenças GPL** (NewPipe/ffmpeg) → app deve ser GPL-compatível.
- youtubedl-android só suporta certas ABIs (arm64-v8a, armeabi-v7a, x86, x86_64) — configurar `abiFilters`/splits.

## 6. Toolchain local (máquina do usuário — Windows 11, sem admin)

Máquina **não tem** Java nem Android SDK. `gh` (autenticado) e `git` existem. Provisionar em `d:\HubLumen\.tools\`:

| Item | URL | Destino |
|---|---|---|
| JDK 17 (Temurin zip) | `https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk` | `d:\HubLumen\.tools\jdk17\` |
| Android cmdline-tools | `https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip` | `d:\HubLumen\.tools\android-sdk\cmdline-tools\latest\` |
| Gradle 8.10.2 | `https://services.gradle.org/distributions/gradle-8.10.2-bin.zip` | `d:\HubLumen\.tools\gradle\` |

Passos após extrair:
1. `JAVA_HOME=d:\HubLumen\.tools\jdk17\<subpasta jdk>`; `ANDROID_HOME=d:\HubLumen\.tools\android-sdk`
2. `sdkmanager --licenses` (aceitar tudo), depois `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"`
3. Criar `local.properties` no projeto com `sdk.dir=d:\\HubLumen\\.tools\\android-sdk`
4. Gerar wrapper: `gradle wrapper --gradle-version 8.10.2` na raiz do projeto
5. Build: `.\gradlew.bat assembleDebug`

## 7. Verificação (critérios de pronto do MVP)

- `gradlew assembleDebug` sem erros; instalar em emulador/dispositivo
- E2E: share sheet do YouTube → escolher formato → download conclui → arquivo em Movies/Music (MediaStore) → aparece na biblioteca → reproduz no player
- Fallback: link não coberto pelo NewPipe (ex.: X/Twitter) cai no yt-dlp e baixa
- Playlist + legendas + conversão mp3/opus funcionam
- Trocar idioma PT↔EN e conferir strings
- CI: push de tag gera APK assinado em GitHub Releases

---

## 8. ESTADO ATUAL (atualizar sempre!)

**Última atualização**: 2026-08-22

- ✅ Entrevistas concluídas e decisões registradas (seção 2)
- ✅ Plano aprovado pelo usuário
- ✅ Toolchain instalado: JDK 17 em `d:\HubLumen\.tools\jdk17\jdk-17.0.20+8`, SDK em `d:\HubLumen\.tools\android-sdk` (platform-tools, android-35, build-tools 35.0.0, licenças aceitas), Gradle 8.10.2 em `d:\HubLumen\.tools\gradle\gradle-8.10.2`
- ✅ Scaffold completo escrito: Gradle KTS + version catalog, manifest, res (strings EN/PT-BR, tema, ícones), e todo o código Kotlin do MVP (LumenApp, Graph, SettingsRepository, Room, NewPipeDownloaderImpl/NewPipeEngine/YtDlpEngine, HttpDownloader, MediaSaver, DownloadService, MainActivity, ShareActivity, AppNav, Home/Library/Player/Settings screens, SpotifyMetadata stub)
- ✅ CI escrito (`.github/workflows/android.yml`), README PT/EN, LICENSE GPL-3.0
- ✅ **BUILD VERDE**: `gradlew assembleDebug` compila (APKs: arm64 74,6 MB; universal 213 MB — tamanho vem do Python/ffmpeg embutidos, como previsto). Atenção: youtubedl-android agora publica no **Maven Central** como `io.github.junkfood02.youtubedl-android:library/ffmpeg:0.18.1` (JitPack do yausername quebrou nas versões novas); os pacotes Kotlin continuam `com.yausername.*`
- ✅ Repo criado e push feito: https://github.com/Lumen-Connection/lumen-stream-mobile (branch main)
- ✅ **CI verde** no GitHub Actions (job build; job release só roda em tag `v*`)
- ⚠️ Commits **sem** trailer de co-autoria do Claude (preferência do usuário; histórico foi reescrito para remover)
- ✅ **Metadata Spotify portada do desktop** (item 1.10): Spotify é DRM — faixa via oEmbed público, playlist/álbum via parse do embed `__NEXT_DATA__`; cada faixa vira `ytsearch1:Artista - Faixa` baixado pelo yt-dlp no YouTube. Playlists expandem em múltiplas tasks na fila. Testes unitários com as mesmas fixtures do desktop em `app/src/test/` (passando)
- ✅ **Assinatura configurada**: keystore em `d:\HubLumen\.secrets\lumen-stream-mobile.keystore` (senha em `lumen-stream-mobile-signing.txt` na mesma pasta — FAZER BACKUP); secrets KEYSTORE_BASE64/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD setados no repo via `gh secret set`
- ✅ **E2E validado em emulador** (AVD `lumen_test`, android-35 x86_64, WHPX): share sheet → diálogo de formato → download → MediaStore → biblioteca → player reproduzindo; fallback NewPipe→yt-dlp confirmado; Spotify track → "Mr. Brightside" → MP3 no MediaStore Music. Dois bugs achados e corrigidos no teste: (1) share recebido fora da Home não abria o diálogo → AppNav agora navega para Home; (2) yt-dlp embarcado (2025.11) toma HTTP 403 do YouTube → auto-update + retry no 403, como o desktop
- ✅ **Release v0.1.0 PUBLICADO**: https://github.com/Lumen-Connection/lumen-stream-mobile/releases/tag/v0.1.0 com 4 APKs assinados (arm64 57 MB, armv7 50 MB, x86_64 60 MB, universal 202 MB). Dois pontos de CI resolvidos no caminho: (1) secret KEYSTORE_BASE64 setado via pipe do PowerShell carregava CRLF e quebrava o `base64 -d` → regravado com `gh secret set --body` e decode endurecido com `tr -d '\r\n '`; (2) a org desabilita escrita padrão do GITHUB_TOKEN → workflow agora declara `permissions: contents: write` explícito
- ✅ **v0.1.1 publicado e verificado** (https://github.com/Lumen-Connection/lumen-stream-mobile/releases/tag/v0.1.1, marcado Latest): release build testado E2E no emulador antes de publicar e o APK arm64 entregue foi inspecionado (classes `AsiExtraField`/`ExtraFieldUtils` presentes sem ofuscação). **v0.1.0 está quebrado — não instalar** (avaliar deletar aquele release)
- ⚠️ **LIÇÃO: testar sempre o APK de RELEASE, não só o debug.** O v0.1.0 subiu quebrado (crash no launch) porque o E2E rodou no debug, que não passa por R8. Duas causas: (1) R8 tornava `AsiExtraField` não-concreta e o `ExtraFieldUtils.<clinit>` do commons-compress (usado pelo ZipUtils do youtubedl-android para extrair o Python) morria com "class ... is not a concrete class" → regra `-keep class org.apache.commons.compress.**`; (2) `LumenApp` capturava `Exception`, mas `ExceptionInInitializerError` é `Error` → escapava da corrotina e derrubava o app; agora captura `Throwable` e degrada para só-NewPipe. Corrigido no **v0.1.1**. Para testar release localmente: `gradlew assembleRelease` gera APK unsigned; assinar com `apksigner sign --ks d:\HubLumen\.secrets\lumen-stream-mobile.keystore --ks-key-alias lumenstream`
- ✅ **Paridade de UI com o desktop** (2026-08-24): tema portado 1:1 de `src/ui/theme.rs` (accent #FF5722 fixo, bg_app #0A0E12, bg_card #121821, border #263240, text #EEF3F6/#93A1AD, claro/alto-contraste/compacto), componentes equivalentes a `card_frame`/`page_header`/`accent_button`/`ghost_button`/`nav_item` em `ui/LumenComponents.kt`, e a barra lateral do `dashboard.rs` virou drawer (marca no topo, mesma ordem/ícones de abas, rodapé de armazenamento com anel de uso). Abas: Home, Música, Vídeo, Fila, Biblioteca, Configurações, Ajuda — as exclusivas de desktop (Converter, Folders, Games, Cloud, Achievements) ficaram de fora
- ✅ **Logo oficial**: `assets/LogoOficialLumenStream2.png` e `assets/LogoOficialLumenStreamTransparente2.png` (baixados do repo desktop, nomes originais preservados). Como recurso Android exige nome minúsculo, geram-se `drawable-nodpi/logo_lumen_stream.png` (usado na sidebar) e os `mipmap-*/ic_launcher*` em todas as densidades via script PIL
- ✅ **Backlog 1.x executado** (menos player em background, conforme pedido): mensagens de erro amigáveis portadas de `ytdlp_util.rs` (`download/FriendlyError.kt`, i18n em tempo de exibição), edição de tags na biblioteca + filtro por tag, PiP no player (só por botão, nunca automático ao sair), metadados F-Droid em `fastlane/metadata/android/{en-US,pt-BR}`, e a falha do NewPipe investigada: era `ContentNotAvailableException: The page needs to be reloaded` do extractor v0.24.3 — **bump para v0.26.5 resolveu** e o YouTube volta a resolver pelo NewPipe sem cair no yt-dlp
- ✅ **Ícones no lugar dos emojis** (v0.2.1): a navegação, os botões e as ações dos cartões usavam glifos emoji, que o Android renderiza coloridos e destoavam do desktop (onde o egui desenha monocromático). Agora usam `material-icons-extended` (`Icon` + `ImageVector`, tingidos pela cor do tema) e o losango da marca virou uma forma desenhada (quadrado rotacionado 45°), sem depender de fonte
- ⚠️ Regressão encontrada e corrigida no teste: com o drawer, o link compartilhado se perdia porque o diálogo de formato vivia na Home e a navegação recriava a tela. O diálogo passou para o nível do `AppNav` — agora o compartilhamento funciona de qualquer tela, sem navegar
- ⏭️ **Backlog restante (fase 1.x)**: player em background/MediaSession (excluído a pedido do usuário), fase 2 sync desktop. **Transcrição está FORA DO ESCOPO do mobile** (decisão do usuário em 2026-08-24): exigiria embutir whisper.cpp mais um modelo, somando centenas de MB a um APK que já tem ~55 MB
- Pendências conhecidas de design: player background/MediaSession (fase 1.x); legendas/playlists só via caminho yt-dlp (NewPipe cai automaticamente para yt-dlp nesses casos)
