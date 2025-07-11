package com.jldevelopers.pininput;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;

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

    public interface OnPinEnteredListener {
        void onPinEntered(String pin);
    }

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

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PinInputView);
        try {
            labelColor = a.getColor(R.styleable.PinInputView_labelColor, Color.parseColor("#78909C"));
            floatingLabelColor = a.getColor(R.styleable.PinInputView_floatingLabelColor, Color.parseColor("#03A9F4"));
            boxBackgroundColor = a.getColor(R.styleable.PinInputView_boxBackgroundColor, Color.TRANSPARENT);
            boxStrokeColor = a.getColor(R.styleable.PinInputView_boxStrokeColor, Color.parseColor("#90A4AE"));
            boxStrokeHighlightColor = a.getColor(R.styleable.PinInputView_boxStrokeHighlightColor, Color.parseColor("#29B6F6"));
            errorColor = a.getColor(R.styleable.PinInputView_errorColor, Color.parseColor("#EF5350"));
            boxCornerRadius = a.getDimension(R.styleable.PinInputView_boxCornerRadius, dpToPx(8));
            boxStrokeWidth = a.getDimension(R.styleable.PinInputView_boxStrokeWidth, dpToPx(1));
            digitSpacing = a.getDimension(R.styleable.PinInputView_digitSpacing, dpToPx(8));
            maskInput = a.getBoolean(R.styleable.PinInputView_maskInput, true);
            pinLength = a.getInt(R.styleable.PinInputView_pinLength, 4);
        } finally {
            a.recycle();
        }

        labelTranslationY = dpToPx(12);

        pinContainer = new LinearLayout(context);
        pinContainer.setOrientation(LinearLayout.HORIZONTAL);
        pinContainer.setPadding((int) dpToPx(16), (int) dpToPx(24), (int) dpToPx(16), (int) dpToPx(5));

        labelTextView = new TextView(context);
        labelTextView.setTextSize(16);
        labelTextView.setTextColor(labelColor);
        LayoutParams labelParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = (int) dpToPx(20);
        labelParams.topMargin = (int) dpToPx(10);
        labelTextView.setLayoutParams(labelParams);
        labelTextView.setVisibility(GONE);

        addView(pinContainer);
        addView(labelTextView);

        initPinDigits(context);
    }

    private void initPinDigits(Context context) {
        pinContainer.removeAllViews();
        pinDigits = new EditText[pinLength];
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < pinLength; i++) {
            EditText digit = (EditText) inflater.inflate(R.layout.pin_input_layout, this, false);
            digit.setInputType(maskInput ? (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD) : InputType.TYPE_CLASS_NUMBER);
            digit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            digit.setMaxEms(1);
            digit.setCursorVisible(false);
            digit.addTextChangedListener(new PinTextWatcher(i));

            final int index = i;
            digit.setOnFocusChangeListener((v, hasFocus) -> {
                if (pinDigits == null || pinDigits[index] == null) return;
                updateDigitBackgrounds();
                animateLabel(hasFocus || !getPin().isEmpty());
                if (hasFocus) showKeyboard(v);
            });

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            if (i > 0) {
                params.setMarginStart((int) digitSpacing);
            }
            digit.setLayoutParams(params);
            pinContainer.addView(digit);
            pinDigits[i] = digit;
        }

        animateLabel(false);
        updateDigitBackgrounds();
    }

    private GradientDrawable createBoxBackground(boolean isFocused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(boxBackgroundColor);
        drawable.setCornerRadius(boxCornerRadius);

        int strokeColor;
        if (isErrorState) {
            strokeColor = errorColor;
        } else if (isFocused) {
            strokeColor = boxStrokeHighlightColor;
        } else {
            strokeColor = boxStrokeColor;
        }
        drawable.setStroke((int) boxStrokeWidth, strokeColor);
        return drawable;
    }

    private void updateDigitBackgrounds() {
        if (pinDigits == null) return;
        for (EditText digit : pinDigits) {
            if (digit != null) {
                boolean hasFocus = digit.isFocused();
                digit.setBackground(createBoxBackground(hasFocus));
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
            labelTextView.setBackgroundColor(Color.WHITE);

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
                    labelTextView.setBackgroundColor(isLabelFloating ? Color.TRANSPARENT : Color.WHITE);
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
        // Check if there is already text or focus
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
            // Animate label up when attached
            post(() -> animateLabel(true));
        }
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
                if (currentIndex < pinLength - 1 && pinDigits[currentIndex + 1] != null) {
                    pinDigits[currentIndex + 1].requestFocus();
                } else if (onPinEnteredListener != null) {
                    onPinEnteredListener.onPinEntered(getPin());
                }
                animateLabel(true);
            } else if (s.length() == 0) {
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

    public void clearErrorState() {
        this.isErrorState = false;
        updateDigitBackgrounds();
    }

}
