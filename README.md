# Rede Sociais (Wear OS) - `social_wearos`

Aplicativo dedicado para Wear OS (otimizado especialmente para o ecossistema Galaxy Watch, telas circulares de 432x432 e One UI Watch).

---

## 🎯 Objetivo e Recursos

- **Acesso direto com 1 toque** às principais redes sociais móveis:
  - 🔵 **Facebook** (`m.facebook.com`)
  - 🟣 **Instagram** (`www.instagram.com`)
  - 🔷 **LinkedIn** (`www.linkedin.com/feed`)
  - 🔴 **Reddit** (`www.reddit.com`)
- **Ícone personalizado**: Símbolo moderno estilizado de conexão entre redes sociais.
- **Lista curva responsiva (`WearableRecyclerView`)**: Adaptada ergonomicamente para navegação circular com `setEdgeItemsCenteringEnabled(true)`.
- **Persistência total de login & Sessão**: Execução integrada que aproveita o motor de navegação seguro do dispositivo, mantendo cookies, sessões salvas, biometria e gerenciamento de senhas sem desconectar a cada uso.
- **Zoom de verdade (como no navegador do PC)**: o Gecko roda com densidade 1 (1 px CSS = 1 pixel da tela, o "100%") e a extensão embutida `app/src/main/assets/viewport/` troca o `<meta viewport>` da página. Zoom 50% = a página é montada com o dobro da largura e reduzida para caber, exatamente como o zoom out do PC. Barras fixas, rolagem e toques continuam funcionando (nada de `transform: scale`).
  - Padrão **100%**; girar a coroa/aro: horário aproxima, anti-horário afasta. Níveis: 50 (mín) · 55 · 60 · 67 · 75 · 80 · 90 · 100 · 110 · 125 · 150 · 175 · 200 (máx). O último zoom fica salvo.
- **Modo quadrado (padrão)**: a página fica num quadrado dentro do círculo (`SQUARE_FACTOR = 0.74` em `MainActivity`), então os cantos da barra do site (ex.: perfil do Instagram) ficam visíveis. O zoom aparece na faixa preta de cima; `‹ Redes` e `◯` (tela cheia) na faixa de baixo, sem cobrir o site. Em tela cheia, o `▢` na borda esquerda volta ao quadrado.
- **Botão voltar do relógio**: volta uma página dentro da rede; na primeira página, volta para a lista.

---

## 🏗️ Estrutura do Projeto

```
social_wearos/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/wearports/social/
│       │   ├── MainActivity.java
│       │   ├── SocialAdapter.java
│       │   └── BrowserActivity.java
│       └── res/
│           ├── drawable/
│           │   ├── ic_social_logo.xml
│           │   ├── ic_facebook.xml
│           │   ├── ic_instagram.xml
│           │   ├── ic_linkedin.xml
│           │   ├── ic_reddit.xml
│           │   └── bg_card_selector.xml
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_browser.xml
│           │   └── item_social.xml
│           └── values/
│               ├── strings.xml
│               └── styles.xml
├── build.gradle
├── gradle.properties
├── local.properties
└── settings.gradle
```

---

## 🚀 Como Compilar e Instalar

### 1. Build via Gradle
```powershell
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
cd "C:\dev\wearOS ports\social_wearos"
.\gradlew.bat assembleDebug
```

### 2. Jeito rápido: `build_install.bat`
Dois cliques nele: compila, conecta no relógio (`WATCH=` no topo do arquivo, troque se a porta da depuração Wi‑Fi mudar), instala, abre o Instagram em 100/50/150% e salva prints em `shots/` e o log em `build_log.txt`.

Teste manual por ADB: `adb shell am start -n com.wearports.social/.MainActivity --es open 1 --ei zoom 50` (`open`: 0 Facebook, 1 Instagram, 2 LinkedIn, 3 Reddit).

### 3. Instalação no Galaxy Watch via ADB
```powershell
& "C:\Users\GUS\AppData\Local\Android\Sdk\platform-tools\adb.exe" -s 192.168.15.84:42335 install -r app\build\outputs\apk\debug\app-debug.apk
```
