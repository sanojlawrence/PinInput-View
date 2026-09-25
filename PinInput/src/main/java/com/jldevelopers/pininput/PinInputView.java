package com.jldevelopers.pininput;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

public class PinInputView extends FrameLayout {

    private int pinLength = 4;
    private EditText[] pinDigits;
    private LinearLayout pinContainer;
    private TextView labelTextView;
    private OnPinEnteredListener onPinEnteredListener;

    // Style attributes
    private int labelColor;
    private int floatingLabelColor;
    private int boxBackgroundColor;
    private int boxStrokeColor;
    private int boxStrokeHighlightColor;
    private int errorColor;
    private float boxCornerRadius;
    private float boxStrokeWidth;
    private float digitSpacing;
    private boolean maskInput;

    private boolean isErrorState = false;
    private boolean isLabelFloating = false;
    private float labelTranslationY;
    private final float floatingLabelScale = 0.9f;

    // New modern UI attributes
    private int digitTextColor;
    private int digitTextSize;
    private int animationDuration;
    private boolean enableRippleEffect;
    private int digitElevation;
    private int digitWidth;
    private int digitHeight;
    private int surfaceColor;
    private Typeface digitTypeface;
    private Drawable normalBg;
    private Drawable focusedBg;
    private Drawable errorBg;

    // Animation fields
    private ValueAnimator currentDigitAnimator;

    public PinInputView(Context context) {
        super(context);
        init(context, null);
    }

    public PinInputView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PinInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        removeAllViews();
        setClipToPadding(false);
        setClipChildren(false);

        //noinspection resource
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PinInputView);
        try {
            labelColor = ContextCompat.getColor(context, R.color.jl_pin_label);
            floatingLabelColor = ContextCompat.getColor(context, R.color.jl_pin_label_focused);
            boxBackgroundColor = ContextCompat.getColor(context, R.color.jl_pin_background);
            boxStrokeColor = ContextCompat.getColor(context, R.color.jl_pin_stroke);
            boxStrokeHighlightColor = ContextCompat.getColor(context, R.color.jl_pin_stroke_focused);
            errorColor = ContextCompat.getColor(context, R.color.jl_pin_error);
            digitTextColor = ContextCompat.getColor(context, R.color.jl_pin_text);
            // Existing attributes
            boxCornerRadius = a.getDimension(R.styleable.PinInputView_boxCornerRadius, dpToPx(14));
            boxStrokeWidth = a.getDimension(R.styleable.PinInputView_boxStrokeWidth, dpToPx(1));
            digitSpacing = a.getDimension(R.styleable.PinInputView_digitSpacing, dpToPx(10));
            maskInput = a.getBoolean(R.styleable.PinInputView_maskInput, true);
            pinLength = a.getInt(R.styleable.PinInputView_pinLength, 6); // Default to 6 for modern look
            surfaceColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface);

            // New modern attributes
            digitTextSize = a.getDimensionPixelSize(R.styleable.PinInputView_digitTextSize, spToPx(18));
            animationDuration = a.getInteger(R.styleable.PinInputView_animationDuration, 200);
            enableRippleEffect = a.getBoolean(R.styleable.PinInputView_enableRippleEffect, true);
            digitElevation = a.getDimensionPixelSize(R.styleable.PinInputView_digitElevation, dpToPx(2));
            digitWidth = a.getDimensionPixelSize(R.styleable.PinInputView_digitWidth, dpToPx(48));
            digitHeight = a.getDimensionPixelSize(R.styleable.PinInputView_digitHeight, dpToPx(56));

            int typefaceValue = a.getInteger(R.styleable.PinInputView_digitTypeface, 0);
            digitTypeface = getTypefaceFromValue(typefaceValue);

        } finally {
            a.recycle();
        }

        labelTranslationY = dpToPx(16);

        normalBg = createBoxBackground(false);
        focusedBg = createBoxBackground(true);

        isErrorState = true;
        errorBg = createBoxBackground(false);
        isErrorState = false;

        // Create main container with modern spacing
        pinContainer = new LinearLayout(context);
        pinContainer.setOrientation(LinearLayout.HORIZONTAL);
        pinContainer.setGravity(Gravity.CENTER);
        pinContainer.setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(16));
        pinContainer.setClipToPadding(false);
        pinContainer.setClipChildren(false);

        // Modern label styling
        labelTextView = new TextView(context);
        labelTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, spToPx(16));
        labelTextView.setTextColor(labelColor);
        labelTextView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));

        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.START;
        labelParams.leftMargin = dpToPx(20);
        labelParams.topMargin = dpToPx(10);
        labelTextView.setLayoutParams(labelParams);
        labelTextView.setVisibility(GONE);

        addView(pinContainer);
        addView(labelTextView);

        initPinDigits(context);
    }

    private Typeface getTypefaceFromValue(int value) {
        return switch (value) {
            case 1 -> Typeface.MONOSPACE;
            case 2 -> Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case 3 -> Typeface.create("sans-serif", Typeface.NORMAL);
            default -> Typeface.DEFAULT;
        };
    }

    private void initPinDigits(Context context) {
        pinContainer.removeAllViews();
        pinDigits = new EditText[pinLength];
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < pinLength; i++) {
            EditText digit = (EditText) inflater.inflate(R.layout.pin_input_layout, this, false);

            // Modern text styling
            digit.setTextColor(digitTextColor);
            digit.setTextSize(TypedValue.COMPLEX_UNIT_PX, digitTextSize);
            digit.setTypeface(digitTypeface);
            digit.setGravity(Gravity.CENTER);

            // Input configuration
            digit.setInputType(maskInput ?
                    (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD) :
                    InputType.TYPE_CLASS_NUMBER);
            digit.setImeOptions(i == pinLength - 1 ? EditorInfo.IME_ACTION_DONE : EditorInfo.IME_ACTION_NEXT);
            digit.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(1) });
            digit.setCursorVisible(true);
            digit.setClickable(true);
            digit.setFocusable(true);
            digit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            digit.setContentDescription("PIN digit " + (i + 1));
            digit.setLongClickable(false);
            digit.setTextIsSelectable(false);

            digit.addTextChangedListener(new PinTextWatcher(i));

            final int index = i;
            digit.setOnFocusChangeListener((v, hasFocus) -> {
                if (pinDigits == null || pinDigits[index] == null) return;
                updateDigitBackgrounds();
                animateLabel(hasFocus || !getPin().isEmpty());
                if (hasFocus) {
                    showKeyboard(v);
                    animateDigitFocus(index);
                }
            });

            digit.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (digit.getText().length() == 0 && index > 0) {
                        pinDigits[index - 1].requestFocus();
                        pinDigits[index - 1].setText("");
                        return true;
                    }
                }
                return false;
            });

            // Modern layout with elevation and ripple
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(digitWidth, digitHeight);
            if (i > 0) {
                params.setMarginStart((int) digitSpacing);
            }
            digit.setLayoutParams(params);

            // Add elevation for modern look
            digit.setElevation(digitElevation);

            pinContainer.addView(digit);
            pinDigits[i] = digit;
        }

        animateLabel(false);
        updateDigitBackgrounds();
    }

    private Drawable createBoxBackground(boolean isFocused) {
        ShapeAppearanceModel shapeModel = new ShapeAppearanceModel()
                .toBuilder()
                .setAllCornerSizes(boxCornerRadius)
                .build();

        MaterialShapeDrawable drawable = new MaterialShapeDrawable(shapeModel);
        drawable.setFillColor(ColorStateList.valueOf(boxBackgroundColor));

        int strokeColor;
        if (isErrorState) {
            strokeColor = errorColor;
        } else if (isFocused) {
            strokeColor = boxStrokeHighlightColor;
        } else {
            strokeColor = boxStrokeColor;
        }

        drawable.setStroke((int) boxStrokeWidth, strokeColor);
        if (enableRippleEffect) {
            return new RippleDrawable(
                    ColorStateList.valueOf(withAlpha(boxStrokeHighlightColor, 0.12f)),
                    drawable,
                    null
            );
        }
        return drawable;
    }

    private int withAlpha(int color, float alpha) {
        int a = Math.round(255 * alpha);
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void animateDigitFocus(int index) {
        if (currentDigitAnimator != null && currentDigitAnimator.isRunning()) {
            currentDigitAnimator.cancel();
        }

        if (index < 0 || index >= pinDigits.length || pinDigits[index] == null) {
            return;
        }

        EditText digit = pinDigits[index];

        float targetScale = 1.05f;
        float currentScale = digit.getScaleX();

        currentDigitAnimator = ValueAnimator.ofFloat(currentScale, targetScale);
        currentDigitAnimator.setDuration(animationDuration);
        currentDigitAnimator.setInterpolator(AnimationUtils.loadInterpolator(getContext(), com.google.android.material.R.interpolator.m3_sys_motion_easing_emphasized));

        currentDigitAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            digit.setScaleX(value);
            digit.setScaleY(value);
        });

        currentDigitAnimator.start();
    }

    private void animateDigitEntry(int index) {
        if (index < 0 || index >= pinDigits.length || pinDigits[index] == null) {
            return;
        }

        EditText digit = pinDigits[index];

        // Create a pop-in animation for new entries
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(digit, "scaleX", 0.8f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(digit, "scaleY", 0.8f, 1.0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY);
        set.setDuration(animationDuration);
        set.setInterpolator(new OvershootInterpolator());
        set.start();
    }

    public interface OnPinEnteredListener {
        void onPinEntered(String pin);
    }

    private void updateDigitBackgrounds() {
        if (pinDigits == null) return;
        for (EditText digit : pinDigits) {
            if (digit == null) continue;

            if (isErrorState) {
                digit.setBackground(errorBg);
            } else if (digit.isFocused()) {
                digit.setBackground(focusedBg);
            } else {
                digit.setBackground(normalBg);
            }
        }
    }

    public void setLabel(String label) {
        if (labelTextView != null) {
            labelTextView.setText(label);
            labelTextView.setVisibility(VISIBLE);
            labelTextView.setTranslationY(0);
            labelTextView.setScaleX(1f);
            labelTextView.setScaleY(1f);
            labelTextView.setTextColor(labelColor);
            labelTextView.setBackgroundColor(surfaceColor);

            if (!getPin().isEmpty()) {
                labelTextView.setTranslationY(-labelTranslationY);
                labelTextView.setScaleX(floatingLabelScale);
                labelTextView.setScaleY(floatingLabelScale);
                labelTextView.setTextColor(floatingLabelColor);
                isLabelFloating = true;
            }
        }
    }

    private void animateLabel(boolean floatUp) {
        if (labelTextView.getText().toString().isEmpty() || floatUp == isLabelFloating) {
            return;
        }

        isLabelFloating = floatUp;

        float startY = labelTextView.getTranslationY();
        float endY = floatUp ? -labelTranslationY : 0f;

        float startScale = floatUp ? 1f : floatingLabelScale;
        float endScale = floatUp ? floatingLabelScale : 1f;

        float startAlpha = floatUp ? 1f : 0.85f;
        float endAlpha = floatUp ? 0.85f : 1f;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(250);
        animator.setInterpolator(floatUp ? new OvershootInterpolator(1.2f) : new DecelerateInterpolator());

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();

            float currentY = startY + (endY - startY) * fraction;
            float currentScale = startScale + (endScale - startScale) * fraction;
            float currentAlpha = startAlpha + (endAlpha - startAlpha) * fraction;

            labelTextView.setTranslationY(currentY);
            labelTextView.setScaleX(currentScale);
            labelTextView.setScaleY(currentScale);
            labelTextView.setAlpha(currentAlpha);

            int color = floatUp
                    ? interpolateColor(labelColor, floatingLabelColor, fraction)
                    : interpolateColor(floatingLabelColor, labelColor, fraction);
            labelTextView.setTextColor(color);
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // When label is floating ➜ remove background & When label is normal ➜ set background white
                labelTextView.setBackgroundColor(isLabelFloating ? Color.TRANSPARENT : surfaceColor);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (floatUp) {
                    // While moving up, already remove background to match floating style
                    labelTextView.setBackgroundColor(Color.TRANSPARENT);
                }
            }
        });

        animator.start();
    }

    private int interpolateColor(int startColor, int endColor, float fraction) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
                ((startR + (int) (fraction * (endR - startR))) << 16) |
                ((startG + (int) (fraction * (endG - startG))) << 8) |
                ((startB + (int) (fraction * (endB - startB))));
    }

    public String getPin() {
        StringBuilder sb = new StringBuilder();
        if (pinDigits == null) return "";
        for (EditText digit : pinDigits) {
            if (digit != null && digit.getText() != null) {
                sb.append(digit.getText().toString());
            }
        }
        return sb.toString();
    }

    public void clear() {
        if (pinDigits == null) return;
        for (EditText digit : pinDigits) {
            if (digit != null) {
                digit.setText("");
            }
        }
        if (pinLength > 0 && pinDigits[0] != null) {
            pinDigits[0].requestFocus();
        }
        animateLabel(false);
        isErrorState = false;
        updateDigitBackgrounds();
    }

    private void showKeyboard(View view) {
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        this.onPinEnteredListener = listener;
    }

    public void setErrorState(boolean error) {
        this.isErrorState = error;
        updateDigitBackgrounds();

        if (error) {
            // Add shake animation for error state
            ObjectAnimator shake = ObjectAnimator.ofFloat(pinContainer, "translationX", 0, 25, -25, 15, -15, 6, -6, 0);
            shake.setDuration(600);
            shake.start();
        }
    }

    public void setPinLength(int length) {
        if (length <= 0) throw new IllegalArgumentException("PIN length must be greater than 0");
        this.pinLength = length;
        initPinDigits(getContext());
        animateLabel(false);
        isErrorState = false;
    }

    private int dpToPx(float dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        boolean shouldFloat = false;

        if (!getPin().isEmpty()) {
            shouldFloat = true;
        } else {
            for (EditText digit : pinDigits) {
                if (digit != null && digit.isFocused()) {
                    shouldFloat = true;
                    break;
                }
            }
        }

        if (shouldFloat) {
            post(() -> animateLabel(true));
        }

        // Force keyboard if first digit is focused
        if (pinDigits != null && pinDigits.length > 0 && pinDigits[0].isFocused()) {
            postDelayed(() -> showKeyboard(pinDigits[0]), 300);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(null);
    }

    private class PinTextWatcher implements TextWatcher {
        private final int currentIndex;

        PinTextWatcher(int index) {
            this.currentIndex = index;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isErrorState) {
                isErrorState = false;
                updateDigitBackgrounds();
            }

            if (s.length() == 1) {
                animateDigitEntry(currentIndex);

                if (currentIndex < pinLength - 1 && pinDigits[currentIndex + 1] != null) {
                    pinDigits[currentIndex + 1].requestFocus();
                } else if (onPinEnteredListener != null) {
                    // Post this to the next UI thread cycle to ensure all digits are properly set
                    post(() -> onPinEnteredListener.onPinEntered(getPin()));
                }
                animateLabel(true);
            } else if (TextUtils.isEmpty(s)) {
                if (currentIndex > 0 && pinDigits[currentIndex - 1] != null) {
                    pinDigits[currentIndex - 1].requestFocus();
                }
                if (getPin().isEmpty()) {
                    animateLabel(false);
                }
            }
            updateDigitBackgrounds();
        }
    }

    private int spToPx(float sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    public void clearErrorState() {
        this.isErrorState = false;
        updateDigitBackgrounds();
    }
}