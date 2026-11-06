package com.wearports.ai;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "ai_prefs";
    private static final String KEY_SAVED_IS_SQUARE = "saved_is_square";
    private static final int SPEECH_REQUEST_CODE = 1001;

    // Zoom base a 50% comprovado no app de redes sociais para caber todo o conteúdo sem scroll horizontal
    private static final float INITIAL_ZOOM = 0.50f;
    private static final float MIN_ZOOM = 0.40f;
    private static final float MAX_ZOOM = 1.20f;
    private static final float ZOOM_STEP = 0.05f;

    // Proporção do quadrado interno no círculo (SQUARE_FACTOR = 0.76f) ~328px em tela de 432px
    private static final float SQUARE_FACTOR = 0.76f;

    private static GeckoRuntime sGeckoRuntime;
    private GeckoSession geckoSession;
    private GeckoView geckoView;

    private FrameLayout rootContainer;
    private ScrollView scrollAi;
    private FrameLayout browserContainer;
    private FrameLayout geckoBox;
    private ProgressBar progressBar;
    private TextView txtZoomIndicator;
    private LinearLayout bottomControlsBar;
    private FrameLayout btnBackHome;
    private FrameLayout btnToggleAspect;
    private TextView txtAspectIcon;

    private boolean isSquareMode = true;
    private boolean isBrowserActive = false;
    private float currentZoom = INITIAL_ZOOM;

    private int screenWidthPx = 432;
    private int screenHeightPx = 432;

    // Controle de gesto para invocar controles arrastando da borda inferior para cima
    private float touchDownY = 0f;
    private float touchDownX = 0f;
    private boolean isTrackingBottomGesture = false;

    // Timer para auto-ocultar a barra de controles após 4.5 segundos de inatividade
    private final Handler controlsHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = this::hideControlsBar;

    private final Handler zoomHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideZoomRunnable = () -> {
        if (txtZoomIndicator != null) {
            txtZoomIndicator.setVisibility(View.GONE);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isSquareMode = prefs.getBoolean(KEY_SAVED_IS_SQUARE, true);

        // Detecta resolução real da tela do Wear OS
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidthPx = size.x > 0 ? size.x : 432;
        screenHeightPx = size.y > 0 ? size.y : 432;

        rootContainer = findViewById(R.id.root_container);
        scrollAi = findViewById(R.id.scroll_ai);
        browserContainer = findViewById(R.id.browser_container);
        geckoBox = findViewById(R.id.gecko_box);
        geckoView = findViewById(R.id.geckoview);
        progressBar = findViewById(R.id.progress_loading);
        txtZoomIndicator = findViewById(R.id.txt_zoom_indicator);
        bottomControlsBar = findViewById(R.id.bottom_controls_bar);
        btnBackHome = findViewById(R.id.btn_back_home);
        btnToggleAspect = findViewById(R.id.btn_toggle_aspect);
        txtAspectIcon = findViewById(R.id.txt_aspect_icon);

        txtZoomIndicator.setVisibility(View.GONE);

        // Seleção direta de Inteligência Artificial
        findViewById(R.id.btn_gemini).setOnClickListener(v -> openAiWeb("https://gemini.google.com/app"));
        findViewById(R.id.btn_chatgpt).setOnClickListener(v -> openAiWeb("https://chatgpt.com"));
        findViewById(R.id.btn_perplexity).setOnClickListener(v -> openAiWeb("https://www.perplexity.ai"));

        // Ações do dock retrátil inferior
        btnBackHome.setOnClickListener(v -> closeBrowser());
        btnToggleAspect.setOnClickListener(v -> toggleAspectRatio());

        // Inicializa o motor GeckoView com cookies e sessões persistentes
        if (sGeckoRuntime == null) {
            GeckoRuntimeSettings runtimeSettings = new GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .consoleOutput(false)
                .build();
            sGeckoRuntime = GeckoRuntime.create(getApplicationContext(), runtimeSettings);
        }

        // Suporte à Coroa Rotativa do Galaxy Watch (Rotary Encoder)
        rootContainer.requestFocus();
        rootContainer.setOnGenericMotionListener((v, event) -> {
            if (isBrowserActive && event.getAction() == MotionEvent.ACTION_SCROLL &&
                event.isFromSource(InputDeviceCompat.SOURCE_ROTARY_ENCODER)) {

                float axisValue = -event.getAxisValue(MotionEventCompat.AXIS_SCROLL);
                if (axisValue > 0) {
                    adjustZoom(ZOOM_STEP);
                } else if (axisValue < 0) {
                    adjustZoom(-ZOOM_STEP);
                }
                return true;
            }
            return false;
        });
    }

    private void openAiWeb(String url) {
        isBrowserActive = true;
        scrollAi.setVisibility(View.GONE);
        browserContainer.setVisibility(View.VISIBLE);

        applyAspectRatioLayout();

        // Inicia com os controles recolhidos para tela 100% limpa
        if (bottomControlsBar != null) {
            bottomControlsBar.setVisibility(View.GONE);
            bottomControlsBar.setTranslationY(80f);
            bottomControlsBar.setAlpha(0f);
        }

        if (geckoSession == null) {
            GeckoSessionSettings sessionSettings = new GeckoSessionSettings.Builder()
                .usePrivateMode(false) // Salva login e contas do usuário
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
                .build();

            geckoSession = new GeckoSession(sessionSettings);
            geckoSession.open(sGeckoRuntime);

            // Suprime popups e alertas indesejados
            geckoSession.setPromptDelegate(new GeckoSession.PromptDelegate() {
                @Nullable
                @Override
                public GeckoResult<PromptResponse> onAlertPrompt(@NonNull GeckoSession session, @NonNull AlertPrompt prompt) {
                    return GeckoResult.fromValue(prompt.dismiss());
                }

                @Nullable
                @Override
                public GeckoResult<PromptResponse> onButtonPrompt(@NonNull GeckoSession session, @NonNull ButtonPrompt prompt) {
                    return GeckoResult.fromValue(prompt.dismiss());
                }
            });

            geckoSession.setProgressDelegate(new GeckoSession.ProgressDelegate() {
                @Override
                public void onPageStart(@NonNull GeckoSession session, @NonNull String pageUrl) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(20);
                    applyZoomToWeb();
                }

                @Override
                public void onPageStop(@NonNull GeckoSession session, boolean success) {
                    progressBar.setVisibility(View.GONE);
                    applyZoomToWeb();
                    optimizeAiInterface();
                }

                @Override
                public void onProgressChange(@NonNull GeckoSession session, int progress) {
                    if (progress < 100) {
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(progress);
                        if (progress > 35) {
                            applyZoomToWeb();
                            optimizeAiInterface();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            });

            geckoView.setSession(geckoSession);
        }

        applyZoomToWeb();
        geckoSession.loadUri(url);
        rootContainer.requestFocus();
    }

    /**
     * Intercepta o gesto de arrastar da base para cima para invocar o painel inferior de ações.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isBrowserActive) {
            float y = ev.getRawY();
            float x = ev.getRawX();
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (y >= screenHeightPx - 110) {
                        touchDownY = y;
                        touchDownX = x;
                        isTrackingBottomGesture = true;
                    } else {
                        isTrackingBottomGesture = false;
                        if (bottomControlsBar != null && bottomControlsBar.getVisibility() == View.VISIBLE) {
                            if (y < screenHeightPx - 120) {
                                hideControlsBar();
                            }
                        }
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isTrackingBottomGesture) {
                        float dy = touchDownY - y; // Positivo ao arrastar para cima
                        float dx = Math.abs(x - touchDownX);
                        if (dy > 18 && dy > dx) {
                            showControlsBar();
                            isTrackingBottomGesture = false;
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isTrackingBottomGesture && y >= screenHeightPx - 50) {
                        showControlsBar();
                    }
                    isTrackingBottomGesture = false;
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * Mostra suavemente o dock inferior retrátil
     */
    private void showControlsBar() {
        if (bottomControlsBar == null) return;

        controlsHideHandler.removeCallbacks(hideControlsRunnable);

        float height = bottomControlsBar.getHeight() > 0 ? bottomControlsBar.getHeight() : 100f;
        if (bottomControlsBar.getVisibility() != View.VISIBLE) {
            bottomControlsBar.setVisibility(View.VISIBLE);
            bottomControlsBar.setAlpha(0f);
            bottomControlsBar.setTranslationY(height);
            bottomControlsBar.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(240)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        } else {
            bottomControlsBar.animate().alpha(1f).translationY(0f).setDuration(150).start();
        }

        controlsHideHandler.postDelayed(hideControlsRunnable, 4500);
    }

    /**
     * Oculta suavemente o dock inferior retrátil
     */
    private void hideControlsBar() {
        if (bottomControlsBar == null || bottomControlsBar.getVisibility() != View.VISIBLE) return;

        float hideY = bottomControlsBar.getHeight() > 0 ? bottomControlsBar.getHeight() : 100f;
        bottomControlsBar.animate()
            .translationY(hideY)
            .alpha(0f)
            .setDuration(240)
            .setInterpolator(new android.view.animation.AccelerateInterpolator())
            .withEndAction(() -> {
                if (bottomControlsBar != null) {
                    bottomControlsBar.setVisibility(View.GONE);
                }
            })
            .start();
    }

    private void toggleAspectRatio() {
        isSquareMode = !isSquareMode;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SAVED_IS_SQUARE, isSquareMode)
            .apply();

        applyAspectRatioLayout();
        applyZoomToWeb();
        showControlsBar();
    }

    private void applyAspectRatioLayout() {
        if (geckoBox == null) return;

        ViewGroup.LayoutParams lp = geckoBox.getLayoutParams();
        if (isSquareMode) {
            int squareSide = (int) (Math.min(screenWidthPx, screenHeightPx) * SQUARE_FACTOR);
            lp.width = squareSide;
            lp.height = squareSide;
            geckoBox.setLayoutParams(lp);

            if (txtAspectIcon != null) {
                txtAspectIcon.setText("◯"); // Clicar vai para tela cheia / redondo
            }
        } else {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
            geckoBox.setLayoutParams(lp);

            if (txtAspectIcon != null) {
                txtAspectIcon.setText("▢"); // Clicar volta para quadrado sem cortes
            }
        }
    }

    /**
     * Técnica comprovada de Zoom do App de Redes Sociais:
     * Aplica escala CSS combinada com largura proporcional inversa.
     * Renderiza o layout mobile completo sem scroll horizontal, perfeitamente nítido e adaptado ao visor do relógio.
     */
    private void applyZoomToWeb() {
        if (geckoSession == null || !isBrowserActive) return;

        float scale = currentZoom;
        String widthPct = String.format(Locale.US, "%.2f%%", (100.0f / scale));

        String js = "(function() {" +
            "  var scale = " + scale + ";" +
            "  var widthStr = '" + widthPct + "';" +
            "  var styleId = '__wearos_ai_zoom__';" +
            "  var st = document.getElementById(styleId);" +
            "  if (!st) {" +
            "    st = document.createElement('style');" +
            "    st.id = styleId;" +
            "    (document.head || document.documentElement).appendChild(st);" +
            "  }" +
            "  st.innerHTML = " +
            "    'html {' +" +
            "    '  transform: scale(' + scale + ') !important;' +" +
            "    '  transform-origin: top left !important;' +" +
            "    '  width: ' + widthStr + ' !important;' +" +
            "    '  min-width: ' + widthStr + ' !important;' +" +
            "    '  height: ' + widthStr + ' !important;' +" +
            "    '  overflow-x: hidden !important;' +" +
            "    '}' +" +
            "    'body {' +" +
            "    '  width: 100% !important;' +" +
            "    '  min-width: 100% !important;' +" +
            "    '  overflow-x: hidden !important;' +" +
            "    '  margin: 0 !important;' +" +
            "    '  padding: 0 !important;' +" +
            "    '}' +" +
            "    'main, article, section, [role=\"main\"], div[id*=\"mount\"] {' +" +
            "    '  max-width: 100% !important;' +" +
            "    '  box-sizing: border-box !important;' +" +
            "    '}';" +
            "  document.documentElement.style.transform = 'scale(' + scale + ')';" +
            "  document.documentElement.style.transformOrigin = 'top left';" +
            "  document.documentElement.style.width = widthStr;" +
            "  document.documentElement.style.minWidth = widthStr;" +
            "  document.documentElement.style.height = widthStr;" +
            "  document.documentElement.style.overflowX = 'hidden';" +
            "  document.body.style.width = '100%';" +
            "  document.body.style.minWidth = '100%';" +
            "  document.body.style.overflowX = 'hidden';" +
            "})();";

        geckoSession.loadUri("javascript:" + js);
    }

    /**
     * Otimização de Interface para IAs:
     * Remove banners de 'Instalar App' e elementos flutuantes intrusivos, maximizando a área de leitura e conversa.
     */
    private void optimizeAiInterface() {
        if (geckoSession == null || !isBrowserActive) return;

        String css =
            // Remove banners de "Instalar aplicativo", popups e smartbanners
            "div[class*='app-banner'], div[id*='app-banner'], .smartbanner, .google-play-banner, " +
            "div[class*='install_app'], div[class*='upsell'], div[id*='upsell'], " +
            "div[role='dialog']:has(button:contains('App')), " +
            "div[class*='download-app'], a[href*='play.google.com'], " +
            // Otimiza cabeçalho para não roubar espaço no relógio
            "header { max-height: 44px !important; } " +
            // Garante tamanho de toque e legibilidade no campo de entrada
            "textarea, input[type='text'], [contenteditable='true'] { font-size: 16px !important; }";

        String script = "(function() {" +
            "  var styleId = '__wearos_ai_cleanup__';" +
            "  if (!document.getElementById(styleId)) {" +
            "    var st = document.createElement('style');" +
            "    st.id = styleId;" +
            "    st.innerHTML = `" + css + "`;" +
            "    (document.head || document.documentElement).appendChild(st);" +
            "  }" +
            "  try {" +
            "    localStorage.setItem('hasDismissedAppBanner', 'true');" +
            "    sessionStorage.setItem('hasDismissedAppBanner', 'true');" +
            "  } catch(e){}" +
            "})();";

        geckoSession.loadUri("javascript:" + script);
    }

    /**
     * Inicia a gravação de voz direta / Speech-to-Text nativo do sistema
     */
    private void startVoiceRecognition() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale sua pergunta...");
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Reconhecimento de voz indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                injectTextAndSubmit(spokenText);
            }
        }
    }

    /**
     * Injeta o texto falado diretamente no chat da IA e dispara o envio
     */
    private void injectTextAndSubmit(String text) {
        if (geckoSession == null || !isBrowserActive) return;

        String safeText = text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

        String script = "(function() {" +
            "  var input = document.querySelector('textarea, [contenteditable=\"true\"], input[type=\"search\"], input[type=\"text\"]');" +
            "  if (input) {" +
            "    input.focus();" +
            "    if (input.isContentEditable) {" +
            "      input.innerText = \"" + safeText + "\";" +
            "    } else {" +
            "      input.value = \"" + safeText + "\";" +
            "    }" +
            "    input.dispatchEvent(new Event('input', { bubbles: true }));" +
            "    input.dispatchEvent(new Event('change', { bubbles: true }));" +
            "    setTimeout(function() {" +
            "      var btn = document.querySelector('button[aria-label*=\"Send\" i], button[aria-label*=\"Enviar\" i], button[data-testid*=\"send\" i], button[class*=\"send\" i]');" +
            "      if (btn) {" +
            "        btn.click();" +
            "      } else {" +
            "        var enter = new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true });" +
            "        input.dispatchEvent(enter);" +
            "      }" +
            "    }, 300);" +
            "  }" +
            "})();";

        geckoSession.loadUri("javascript:" + script);
    }

    private void adjustZoom(float delta) {
        float newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom + delta));
        if (Math.abs(newZoom - currentZoom) > 0.01f) {
            currentZoom = newZoom;
            applyZoomToWeb();

            int percent = Math.round(currentZoom * 100);
            showZoomFeedback(percent + "%");
        }
    }

    private void showZoomFeedback(String text) {
        if (txtZoomIndicator != null) {
            txtZoomIndicator.setText(text);
            txtZoomIndicator.setVisibility(View.VISIBLE);
            zoomHideHandler.removeCallbacks(hideZoomRunnable);
            zoomHideHandler.postDelayed(hideZoomRunnable, 1100);
        }
    }

    private void closeBrowser() {
        isBrowserActive = false;
        controlsHideHandler.removeCallbacks(hideControlsRunnable);
        zoomHideHandler.removeCallbacks(hideZoomRunnable);
        if (bottomControlsBar != null) {
            bottomControlsBar.setVisibility(View.GONE);
        }
        if (txtZoomIndicator != null) {
            txtZoomIndicator.setVisibility(View.GONE);
        }
        browserContainer.setVisibility(View.GONE);
        scrollAi.setVisibility(View.VISIBLE);
        rootContainer.requestFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isBrowserActive) {
            if (keyCode == KeyEvent.KEYCODE_NAVIGATE_NEXT) {
                adjustZoom(ZOOM_STEP);
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_NAVIGATE_PREVIOUS) {
                adjustZoom(-ZOOM_STEP);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (isBrowserActive) {
            closeBrowser();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (geckoSession != null) {
            geckoSession.close();
        }
    }
}
