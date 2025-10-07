//  Created by react-native-create-bridge

package com.thebylito.navigationbarcolor;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.IllegalViewOperationException;

import java.util.HashMap;
import java.util.Map;

import android.view.WindowInsets;
import android.provider.Settings;

public class NavigationBarColorModule extends ReactContextBaseJavaModule {
    public static final String REACT_CLASS = "NavigationBarColor";
    private static final String ERROR_NO_ACTIVITY = "E_NO_ACTIVITY";
    private static final String ERROR_NO_ACTIVITY_MESSAGE = "Tried to change the navigation bar while not attached to an Activity";
    private static final String ERROR_API_LEVEL_MESSAGE = "Only Android Oreo and above is supported";
    private static ReactApplicationContext reactContext = null;
    private static final int UI_FLAG_HIDE_NAV_BAR = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public NavigationBarColorModule(ReactApplicationContext context) {
        // Pass in the context to the constructor and save it so you can emit events
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        super(context);
        reactContext = context;
    }

    public void setNavigationBarTheme(Activity activity, Boolean light) {
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) { // API 26-30
                Window window = activity.getWindow();
                int flags = window.getDecorView().getSystemUiVisibility();
                if (light != null && light) {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                } else {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                }
                window.getDecorView().setSystemUiVisibility(flags);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                int flag;
                if (light != null && light) {
                    flag = WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                } else {
                    flag = 0; // сбросить флаг, чтобы навигационная панель стала темной.
                }
                final WindowInsetsController insetsController = activity.getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.setSystemBarsAppearance(flag, WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
                }
            }
        }
    }

    @Override
    public String getName() {
        // Tell React the name of the module
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        return REACT_CLASS;
    }

    @Override
    public Map<String, Object> getConstants() {
        // Export any constants to be used in your native module
        // https://facebook.github.io/react-native/docs/native-modules-android.html#the-toast-module
        final Map<String, Object> constants = new HashMap<>();
        constants.put("EXAMPLE_CONSTANT", "example");

        return constants;
    }

    @ReactMethod
    public void changeNavigationBarColor(final String color, final Boolean light, final Boolean animated, final Promise promise) {
        final WritableMap map = Arguments.createMap();
        if (getCurrentActivity() != null) {
            try {
                final Window window = getCurrentActivity().getWindow();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Проверка на getCurrentActivity() внутри runOnUiThread, т.к. Activity может быть уничтожена пока Runnable ждет выполнения
                        Activity currentActivity = getCurrentActivity();
                        if (currentActivity == null) {
                            map.putBoolean("success", false);
                            promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
                            return;
                        }

                        if (color.equals("transparent") || color.equals("translucent")) {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                            if (color.equals("transparent")) {
                                // FLAG_LAYOUT_NO_LIMITS позволяет приложению рисовать в области навигационной панели
                                window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                            } else { // translucent
                                // FLAG_TRANSLUCENT_NAVIGATION делает навигационную панель полупрозрачной
                                window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                            }
                            setNavigationBarTheme(currentActivity, light);
                            map.putBoolean("success", true);
                            promise.resolve(map);
                            return;
                        } else {
                            // Если цвет не transparent/translucent, убираем эти флаги
                            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
                        }

                        if (animated != null && animated) {
                            Integer colorFrom = window.getNavigationBarColor();
                            Integer colorTo = Color.parseColor(color);
                            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override
                                public void onAnimationUpdate(ValueAnimator animator) {
                                    window.setNavigationBarColor((Integer) animator.getAnimatedValue());
                                }
                            });
                            colorAnimation.start();
                        } else {
                            window.setNavigationBarColor(Color.parseColor(color));
                        }
                        setNavigationBarTheme(currentActivity, light);
                        map.putBoolean("success", true);
                        promise.resolve(map);
                    }
                });
            } catch (IllegalViewOperationException e) {
                map.putBoolean("success", false);
                promise.reject("error", e);
            } catch (IllegalArgumentException e) {
                map.putBoolean("success", false);
                promise.reject("E_INVALID_COLOR", e.getMessage());
            }
        } else {
            promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
        }
    }

    @ReactMethod
    public void hideNavigationBar(Promise promise) {
        // Проверка API_LEVEL не нужна, SYSTEM_UI_FLAG_HIDE_NAVIGATION доступен с API 14
        // Но для более новых API, особенно для полного погружения/жестов, лучше использовать WindowInsetsController.
        // Однако UI_FLAG_HIDE_NAV_BAR включает IMMERSIVE_STICKY, что работает и на новых версиях для скрытия.
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
            return;
        }

        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Activity activityInThread = getCurrentActivity(); // повторная проверка
                    if (activityInThread != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                            WindowInsetsController insetsController = activityInThread.getWindow().getInsetsController();
                            if (insetsController != null) {
                                // Hides both system bars (status bar and navigation bar)
                                insetsController.hide(WindowInsets.Type.navigationBars()); // только navigation bar
                                // Также можно использовать: insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                            }
                        } else {
                            View decorView = activityInThread.getWindow().getDecorView();
                            decorView.setSystemUiVisibility(UI_FLAG_HIDE_NAV_BAR);
                        }
                        WritableMap map = Arguments.createMap();
                        map.putBoolean("success", true);
                        promise.resolve(map);
                    } else {
                        promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
                    }
                }
            });
        } catch (Exception e) {
            WritableMap map = Arguments.createMap();
            map.putBoolean("success", false);
            promise.reject("error", e);
        }
    }

    @ReactMethod
    public void showNavigationBar(Promise promise) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
            return;
        }

        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Activity activityInThread = getCurrentActivity(); // повторная проверка
                    if (activityInThread != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+
                            WindowInsetsController insetsController = activityInThread.getWindow().getInsetsController();
                            if (insetsController != null) {
                                insetsController.show(WindowInsets.Type.navigationBars());
                                // insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_DEFAULT); // сбросить поведение, если меняли
                            }
                        } else {
                            View decorView = activityInThread.getWindow().getDecorView();
                            // Вместо просто View.SYSTEM_UI_FLAG_VISIBLE, лучше восстановить исходные флаги
                            // или, как минимум, убрать IMMERSIVE_STICKY, если он был установлен.
                            // Простейший вариант - убрать флаги скрытия.
                            int uiOptions = decorView.getSystemUiVisibility();
                            uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                            uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                            decorView.setSystemUiVisibility(uiOptions);
                        }
                        WritableMap map = Arguments.createMap();
                        map.putBoolean("success", true);
                        promise.resolve(map);
                    } else {
                        promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
                    }
                }
            });
        } catch (Exception e) {
            WritableMap map = Arguments.createMap();
            map.putBoolean("success", false);
            promise.reject("error", e);
        }
    }

    // Проверка типа навигационной панели
    @ReactMethod
    public void isNavigationBarVisible(Promise promise) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Activity activityInThread = getCurrentActivity();
                if (activityInThread == null) {
                    promise.reject(ERROR_NO_ACTIVITY, new Throwable(ERROR_NO_ACTIVITY_MESSAGE));
                    return;
                }

                View rootView = activityInThread.findViewById(android.R.id.content);
                if (rootView == null) {
                    promise.reject("E_NO_ROOT_VIEW", "Could not find root view for navigation bar check.");
                    return;
                }

                rootView.post(() -> {
                    boolean isVisible = false; // true, если это кнопки
                    int navBarHeight = 0;      // фактическая высота (включая индикатор жестов)

                    Log.d("NavBarDetector", "Checking navigation bar visibility. API Level: " + Build.VERSION.SDK_INT);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // API 30+ (Android 11+)
                        Log.d("NavBarDetector", "Using WindowInsets.Type for API 30+.");
                        WindowInsets insets = rootView.getRootWindowInsets();
                        if (insets != null) {
                            int navigationBarsBottomInset = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                            int systemGesturesBottomInset = 0;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // systemGestures доступно с API 29
                                systemGesturesBottomInset = insets.getInsets(WindowInsets.Type.systemGestures()).bottom;
                            }

                            Log.d("NavBarDetector", "API 30+ - navigationBarsBottomInset: " + navigationBarsBottomInset);
                            Log.d("NavBarDetector", "API 30+ - systemGesturesBottomInset: " + systemGesturesBottomInset);

                            // Если navigationBarsBottomInset значительно больше systemGesturesBottomInset,
                            // или systemGesturesBottomInset равен 0 (нет индикатора жестов)
                            // и navigationBarsBottomInset значителен, то это кнопки.
                            // Используем порог, чтобы отличить кнопки от маленького индикатора жестов.
                            float density = getReactApplicationContext().getResources().getDisplayMetrics().density;
                            int minHeightForButtonsPx = (int) (40.0f * density); // 40dp порог для кнопок

                            // Основная логика: если есть navigationBarsBottomInset
                            if (navigationBarsBottomInset > 0) {
                                navBarHeight = navigationBarsBottomInset; // Это высота панели (кнопок или индикатора)

                                // Если высота больше порога ИЛИ нет значительных жестов внизу, то это кнопки.
                                // systemGesturesBottomInset > 0 обычно означает, что есть жесты.
                                // Если systemGesturesBottomInset мал, а navigationBarsBottomInset большой, это кнопки.
                                if (navigationBarsBottomInset >= minHeightForButtonsPx && systemGesturesBottomInset < minHeightForButtonsPx / 2) {
                                    isVisible = true; // Считаем, что это кнопки
                                    Log.d("NavBarDetector", "API 30+: Detected buttons (height " + navigationBarsBottomInset + "px >= threshold " + minHeightForButtonsPx + "px and small gestures inset).");
                                } else {
                                    isVisible = false; // Считаем, что это жесты (индикатор или полные жесты)
                                    Log.d("NavBarDetector", "API 30+: Detected gestures (height " + navigationBarsBottomInset + "px < threshold or large gestures inset).");
                                }
                            } else {
                                // navigationBarsBottomInset == 0: либо скрыто, либо полные жесты без видимого индикатора.
                                isVisible = false;
                                navBarHeight = 0;
                                Log.d("NavBarDetector", "API 30+: No navigation bars bottom inset found (hidden or full gestures).");
                            }

                        } else {
                            Log.w("NavBarDetector", "API 30+ - Root WindowInsets is null, assuming no visible nav bar.");
                            isVisible = false;
                            navBarHeight = 0;
                        }

                    } else { // API 24-29 (Android 7-10) - Используем системные настройки + ресурсную высоту
                        Log.d("NavBarDetector", "Using Settings + Resource height for API 24-29.");

                        // --- Определение isVisible через системные настройки (для старых API) ---
                        // Settings.System.getInt() с дефолтным значением не выбрасывает SettingNotFoundException.
                        int navMode = Settings.System.getInt(getReactApplicationContext().getContentResolver(), "navigation_mode", -1);
                        Log.d("NavBarDetector", "Settings.System navigation_mode raw value: " + navMode);

                        if (navMode != -1) {
                            if (navMode == 0 || navMode == 1) { // 0 = 3 кнопки, 1 = 2 кнопки (Pie)
                                isVisible = true;
                                Log.d("NavBarDetector", "API 24-29: Settings.System navigation_mode: " + navMode + " (buttons enabled)");
                            } else { // 2 = жесты (Android 10+)
                                isVisible = false;
                                Log.d("NavBarDetector", "API 24-29: Settings.System navigation_mode: " + navMode + " (gestures enabled)");
                            }
                        } else { // Если navigation_mode не найдено, пробуем system_navigation_keys_enabled
                            Log.w("NavBarDetector", "Settings.System navigation_mode not found or invalid (-1). Trying system_navigation_keys_enabled.");
                            int keysEnabled = Settings.System.getInt(getReactApplicationContext().getContentResolver(), "system_navigation_keys_enabled", 1); // Дефолт 1 (true)
                            isVisible = (keysEnabled == 1);
                            Log.d("NavBarDetector", "API 24-29: Settings.System system_navigation_keys_enabled: " + (isVisible ? "true" : "false") + " (raw value: " + keysEnabled + ")");
                        }

                        // --- Определение navBarHeight (для старых API, полагаемся на ресурс) ---
                        Resources resources = getReactApplicationContext().getResources();
                        int navBarResId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
                        if (navBarResId > 0) {
                            navBarHeight = resources.getDimensionPixelSize(navBarResId);
                            Log.d("NavBarDetector", "API 24-29 - Resource nav_bar_height: " + navBarHeight);
                        } else {
                            Log.w("NavBarDetector", "API 24-29 - Resource nav_bar_height not found, navBarHeight = 0.");
                        }

                        // Fallback для navBarHeight, если isVisible true, но navBarHeight 0 (редкий случай)
                        if (isVisible && navBarHeight <= 0) {
                            float density = getReactApplicationContext().getResources().getDisplayMetrics().density;
                            navBarHeight = (int) (48.0f * density);
                            Log.w("NavBarDetector", "API 24-29: isVisible is true, but navBarHeight was 0. Using default 48dp fallback: " + navBarHeight);
                        }
                    }

                    Log.d("NavBarDetector", "Final isVisible: " + isVisible + ", Final navBarHeight: " + navBarHeight);

                    WritableMap result = Arguments.createMap();
                    result.putBoolean("isVisible", isVisible);
                    result.putInt("navigationBarHeight", navBarHeight);
                    promise.resolve(result);
                });
            }
        });
    }
}