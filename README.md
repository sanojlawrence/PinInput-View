## PinInput View


🎯 PinInputView
A fully customizable and modern PIN input UI component for Android. Supports animated floating labels, individual digit boxes, error states, and optional dot masking for secure PIN entry.

✨ Features
✅ Individual boxes for each PIN digit.
✅ Animated floating label (hint).
✅ Highlight active box on focus.
✅ Automatic move to next digit.
✅ Smooth delete & back-navigation.
✅ Optional PIN masking (dots) for security.
✅ Error state (red border) support.
✅ Dynamic PIN length (e.g., 4, 6, or custom).
✅ Customizable colors, radius, and spacing via XML attributes.
✅ Accessibility-friendly (screen reader support).

💼 Setup
1️⃣ Add the custom view to your project
Add PinInputView.java and pin_digit_view.xml to your project.

2️⃣ Add attrs.xml
Add these inside your res/values/attrs.xml:

```
<declare-styleable name="PinInputView">
    <attr name="hintColor" format="color" />
    <attr name="floatingLabelColor" format="color" />
    <attr name="boxBackgroundColor" format="color" />
    <attr name="boxStrokeColor" format="color" />
    <attr name="boxStrokeHighlightColor" format="color" />
    <attr name="errorColor" format="color" />
    <attr name="boxCornerRadius" format="dimension" />
    <attr name="boxStrokeWidth" format="dimension" />
    <attr name="digitSpacing" format="dimension" />
    <attr name="maskInput" format="boolean" />
    <attr name="pinLength" format="integer" />
</declare-styleable>
```

⚙️ XML Usage

```
<com.your.package.PinInputView
    android:id="@+id/pinView"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:pinLength="6"
    app:maskInput="true"
    app:hintColor="#808080"
    app:floatingLabelColor="#3F51B5"
    app:boxBackgroundColor="#FFFFFF"
    app:boxStrokeColor="#DDDDDD"
    app:boxStrokeHighlightColor="#3F51B5"
    app:errorColor="#FF0000"
    app:boxCornerRadius="8dp"
    app:boxStrokeWidth="1dp"
    app:digitSpacing="8dp" />
```
🧑‍💻 Code Usage
Set hint text
```
pinView.setHint("Enter PIN");
```
Get entered PIN
```
String pin = pinView.getPin();
```
Clear PIN
```
pinView.clear();
```
Listen for complete PIN entry
```
pinView.setOnPinEnteredListener(pin -> {
    Log.d("PinInput", "Entered PIN: " + pin);
    // Check PIN validity, etc.
});
```
Set PIN length dynamically
```
pinView.setPinLength(4); // or 6 or any other value
```
Show error state
```
pinView.setErrorState(true); // red border appears
```
Remove error state
```
pinView.setErrorState(false);
```

💡 Customizable XML attributes
Attribute	Type	Description
pinLength	integer	Number of PIN digits (default 4)
maskInput	boolean	Show dots instead of numbers (default false)
hintColor	color	Hint label color when inactive
floatingLabelColor	color	Hint label color when active/floating
boxBackgroundColor	color	Box background color
boxStrokeColor	color	Box stroke color when inactive
boxStrokeHighlightColor	color	Box stroke color when focused
errorColor	color	Box stroke color in error state
boxCornerRadius	dimension	Box corner radius
boxStrokeWidth	dimension	Box stroke width
digitSpacing	dimension	Space between digit boxes

🦾 Accessibility
Each digit box has an accessibility label ("PIN digit 1", "PIN digit 2", etc.)

Works with screen readers.

Floating label acts as accessible hint.

💎 Visual Example

Enter PIN
[ ● ] [ ● ] [ ● ] [ ● ]

- Floating label moves up on focus.
- Active box highlighted.
- Dots shown if masking enabled.
- Error state (red border) when needed.
💬 Final notes
✅ Built to match modern Material Design patterns.
✅ Easily extensible (animations, shake effect on error, etc.).
✅ Looks great in both light and dark themes.

