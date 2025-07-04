package com.jldevelopers.pininputview;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class PinInputView extends FrameLayout {

    private int pinLength = 4;
    private EditText[] pinDigits;
    private LinearLayout pinContainer;
    private TextView labelTextView;
    private OnPinEnteredListener onPinEnteredListener;

    // Style attributes
    private int hintColor;
    private int floatingLabelColor;
    private int boxBackgroundColor;
    private int boxStrokeColor;
    private int boxStrokeHighlightColor;
    private int errorColor;
    private float boxCornerRadius;
    private float boxStrokeWidth;
    private float digitSpacing;
    private boolean maskInput;

    private boolean isLabelFloating = false;
    private float labelTranslationY;
    private final float floatingLabelScale = 0.75f;

    private boolean isErrorState = false;

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
            hintColor = a.getColor(R.styleable.PinInputView_hintColor, Color.GRAY);
            floatingLabelColor = a.getColor(R.styleable.PinInputView_floatingLabelColor, Color.BLUE);
            boxBackgroundColor = a.getColor(R.styleable.PinInputView_boxBackgroundColor, Color.WHITE);
            boxStrokeColor = a.getColor(R.styleable.PinInputView_boxStrokeColor, Color.LTGRAY);
            boxStrokeHighlightColor = a.getColor(R.styleable.PinInputView_boxStrokeHighlightColor, Color.BLUE);
            errorColor = a.getColor(R.styleable.PinInputView_errorColor, Color.RED);
            boxCornerRadius = a.getDimension(R.styleable.PinInputView_boxCornerRadius, dpToPx(8));
            boxStrokeWidth = a.getDimension(R.styleable.PinInputView_boxStrokeWidth, dpToPx(1));
            digitSpacing = a.getDimension(R.styleable.PinInputView_digitSpacing, dpToPx(8));
            maskInput = a.getBoolean(R.styleable.PinInputView_maskInput, false);
            pinLength = a.getInt(R.styleable.PinInputView_pinLength, 4);
        } finally {
            a.recycle();
        }

        labelTranslationY = dpToPx(16);

        pinContainer = new LinearLayout(context);
        pinContainer.setOrientation(LinearLayout.HORIZONTAL);
        pinContainer.setPadding((int) dpToPx(8), (int) dpToPx(15), (int) dpToPx(8), (int) dpToPx(5)); // Left, Top, Right, Bottom
        pinContainer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(pinContainer);

        labelTextView = new TextView(context);
        labelTextView.setTextSize(16);
        labelTextView.setTextColor(hintColor);
        labelTextView.setVisibility(GONE);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = (int) dpToPx(10);
        labelParams.topMargin = (int) dpToPx(10); // Increase or decrease as needed
        labelTextView.setLayoutParams(labelParams);
        addView(labelTextView);

        initPinDigits(context);
    }

    private void initPinDigits(Context context) {
        pinDigits = new EditText[pinLength];
        LayoutInflater inflater = LayoutInflater.from(context);

        for (int i = 0; i < pinLength; i++) {
            EditText digit = (EditText) inflater.inflate(R.layout.pin_input_layout, this, false);
            digit.setInputType(maskInput ? (EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD)
                    : EditorInfo.TYPE_CLASS_NUMBER);
            digit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
            digit.setCursorVisible(false);
            digit.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            digit.setMaxLines(1);
            digit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});

            LayoutParams params = new LayoutParams((int) dpToPx(40), (int) dpToPx(40));
            if (i != 0) params.leftMargin = (int) digitSpacing;
            digit.setLayoutParams(params);
            digit.setBackground(createBoxBackground(boxStrokeColor));

            final int index = i;
            digit.addTextChangedListener(new PinTextWatcher(index));

            digit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!isErrorState) {
                    digit.setBackground(createBoxBackground(hasFocus ? boxStrokeHighlightColor : boxStrokeColor));
                }
                animateLabel(hasFocus || !getPin().isEmpty());
                if (hasFocus) showKeyboard(v);
            });

            digit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            digit.setContentDescription("PIN digit " + (i + 1));

            pinContainer.addView(digit);
            pinDigits[i] = digit;
        }
    }

    private GradientDrawable createBoxBackground(int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(boxBackgroundColor);
        drawable.setCornerRadius(boxCornerRadius);
        drawable.setStroke((int) boxStrokeWidth, strokeColor);
        return drawable;
    }

    public void setHint(String hint) {
        labelTextView.setText(hint);
        labelTextView.setVisibility(VISIBLE);
        labelTextView.setTranslationY(0);
        labelTextView.setScaleX(1f);
        labelTextView.setScaleY(1f);
        labelTextView.setTextColor(hintColor);

        if (!getPin().isEmpty()) {
            labelTextView.setTranslationY(-labelTranslationY);
            labelTextView.setScaleX(floatingLabelScale);
            labelTextView.setScaleY(floatingLabelScale);
            labelTextView.setTextColor(floatingLabelColor);
            isLabelFloating = true;
        }
    }

    public void setPinLength(int length) {
        if (length <= 0) throw new IllegalArgumentException("PIN length must be greater than 0");
        this.pinLength = length;
        // Remove existing digits
        pinContainer.removeAllViews();
        // Re-initialize digits
        initPinDigits(getContext());
        // Reset error state and label
        setErrorState(false);
        animateLabel(false);
    }

    private void animateLabel(boolean floatUp) {
        if (labelTextView.getText().toString().isEmpty() || floatUp == isLabelFloating) return;

        isLabelFloating = floatUp;
        ValueAnimator animator = ValueAnimator.ofFloat(floatUp ? 0 : -labelTranslationY, floatUp ? -labelTranslationY : 0);
        ArgbEvaluator evaluator = new ArgbEvaluator();

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            labelTextView.setTranslationY(value);

            float scale = floatUp
                    ? 1f + (floatingLabelScale - 1f) * animation.getAnimatedFraction()
                    : floatingLabelScale + (1f - floatingLabelScale) * animation.getAnimatedFraction();

            labelTextView.setScaleX(scale);
            labelTextView.setScaleY(scale);

            int color = (int) evaluator.evaluate(animation.getAnimatedFraction(),
                    floatUp ? hintColor : floatingLabelColor,
                    floatUp ? floatingLabelColor : hintColor);
            labelTextView.setTextColor(color);
        });

        animator.setDuration(200);
        animator.start();
    }

    public String getPin() {
        StringBuilder sb = new StringBuilder();
        for (EditText digit : pinDigits) {
            sb.append(digit.getText().toString());
        }
        return sb.toString();
    }

    public void clear() {
        for (EditText digit : pinDigits) {
            digit.setText("");
            digit.setBackground(createBoxBackground(boxStrokeColor));
        }
        if (pinLength > 0) pinDigits[0].requestFocus();
        animateLabel(false);
        setErrorState(false);
    }

    public void setOnPinEnteredListener(OnPinEnteredListener listener) {
        this.onPinEnteredListener = listener;
    }

    public void setErrorState(boolean error) {
        isErrorState = error;
        for (EditText digit : pinDigits) {
            digit.setBackground(createBoxBackground(error ? errorColor : boxStrokeColor));
        }
    }

    private void showKeyboard(View v) {
        v.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private class PinTextWatcher implements TextWatcher {
        private final int index;

        PinTextWatcher(int index) {
            this.index = index;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override public void afterTextChanged(Editable s) {
            if (s.length() == 1) {
                if (index < pinLength - 1) {
                    pinDigits[index + 1].requestFocus();
                } else if (onPinEnteredListener != null) {
                    onPinEnteredListener.onPinEntered(getPin());
                }
                animateLabel(true);
            } else if (s.length() == 0 && index > 0) {
                pinDigits[index - 1].requestFocus();
                pinDigits[index - 1].setText("");
            }
            if (getPin().isEmpty()) animateLabel(false);
        }
    }
}
