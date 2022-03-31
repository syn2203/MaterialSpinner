package com.jaredrummler.materialspinner;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public class MaterialSpinner extends TextView {

    private OnNothingSelectedListener onNothingSelectedListener;
    private OnItemSelectedListener onItemSelectedListener;
    private MaterialSpinnerBaseAdapter adapter;
    private PopupWindow popupWindow;
    private ListView listView;
    private Drawable arrowDrawable;
    private boolean hideArrow;
    private boolean nothingSelected;
    private int popupWindowMaxHeight;
    private int popupWindowHeight;
    private int selectedIndex;
    private int backgroundColor;
    private int backgroundSelector;
    private int backgroundSelectorItem;
    private int arrowColor;
    private int arrowColorDisabled;
    private int textColor;
    private int hintColor;
    private int popupPaddingTop;
    private int popupPaddingLeft;
    private int popupPaddingBottom;
    private int popupPaddingRight;
    private String hintText;

    public MaterialSpinner(Context context) {
        super(context);
        init(context, null);
    }

    public MaterialSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public MaterialSpinner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MaterialSpinner);
        int defaultColor = getTextColors().getDefaultColor();
        boolean rtl = Utils.isRtl(context);

        int paddingLeft, paddingTop, paddingRight, paddingBottom;
        int defaultPaddingLeft, defaultPaddingTop, defaultPaddingRight, defaultPaddingBottom;
        int defaultPopupPaddingLeft, defaultPopupPaddingTop, defaultPopupPaddingRight, defaultPopupPaddingBottom;

        Resources resources = getResources();
        defaultPaddingLeft = defaultPaddingRight = defaultPaddingBottom = defaultPaddingTop = resources.getDimensionPixelSize(R.dimen.ms__padding_top);
        if (rtl) {
            defaultPaddingRight = resources.getDimensionPixelSize(R.dimen.ms__padding_left);
        } else {
            defaultPaddingLeft = resources.getDimensionPixelSize(R.dimen.ms__padding_left);
        }
        defaultPopupPaddingLeft = defaultPopupPaddingRight = resources.getDimensionPixelSize(R.dimen.ms__popup_padding_left);
        defaultPopupPaddingTop = defaultPopupPaddingBottom = resources.getDimensionPixelSize(R.dimen.ms__popup_padding_top);

        try {
            backgroundColor = ta.getColor(R.styleable.MaterialSpinner_ms_background_color, Color.BLACK);
            backgroundSelector = ta.getResourceId(R.styleable.MaterialSpinner_ms_background_selector, 0);
            backgroundSelectorItem = ta.getResourceId(R.styleable.MaterialSpinner_ms_background_selector_item, 0);
            textColor = ta.getColor(R.styleable.MaterialSpinner_ms_text_color, defaultColor);
            hintColor = ta.getColor(R.styleable.MaterialSpinner_ms_hint_color, defaultColor);
            arrowColor = ta.getColor(R.styleable.MaterialSpinner_ms_arrow_tint, textColor);
            hideArrow = ta.getBoolean(R.styleable.MaterialSpinner_ms_hide_arrow, false);
            hintText = ta.getString(R.styleable.MaterialSpinner_ms_hint) == null ? "" : ta.getString(R.styleable.MaterialSpinner_ms_hint);
            popupWindowMaxHeight = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_dropdown_max_height, 0);
            popupWindowHeight = ta.getLayoutDimension(R.styleable.MaterialSpinner_ms_dropdown_height, WindowManager.LayoutParams.WRAP_CONTENT);
            paddingTop = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_padding_top, defaultPaddingTop);
            paddingLeft = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_padding_left, defaultPaddingLeft);
            paddingBottom = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_padding_bottom, defaultPaddingBottom);
            paddingRight = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_padding_right, defaultPaddingRight);
            popupPaddingTop = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_popup_padding_top, defaultPopupPaddingTop);
            popupPaddingLeft = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_popup_padding_left, defaultPopupPaddingLeft);
            popupPaddingBottom = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_popup_padding_bottom, defaultPopupPaddingBottom);
            popupPaddingRight = ta.getDimensionPixelSize(R.styleable.MaterialSpinner_ms_popup_padding_right, defaultPopupPaddingRight);
            arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        } finally {
            ta.recycle();
        }

        nothingSelected = true;

        setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        setClickable(true);
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);

        setBackgroundResource(R.drawable.ms__selector);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && rtl) {
            setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            setTextDirection(View.TEXT_DIRECTION_RTL);
        }

        if (!hideArrow) {
            arrowDrawable = Utils.getDrawable(context, R.drawable.ms__arrow).mutate();
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
            Drawable[] drawables = getCompoundDrawables();
            if (rtl) {
                drawables[0] = arrowDrawable;
            } else {
                drawables[2] = arrowDrawable;
            }
            setCompoundDrawablesWithIntrinsicBounds(drawables[0], drawables[1], drawables[2], drawables[3]);
        }

        listView = new ListView(context);
        listView.setBackgroundResource(backgroundSelector);
        listView.setId(getId());
        listView.setDivider(null);
        listView.setItemsCanFocus(true);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedIndex = position;
            nothingSelected = false;
            Object item = adapter.get(position);
            adapter.notifyItemSelected(position);
            setText(item.toString());
            collapse();
            if (onItemSelectedListener != null) {
                onItemSelectedListener.onItemSelected(MaterialSpinner.this, position, id, item);
            }
        });

        popupWindow = new PopupWindow(context);
        popupWindow.setContentView(listView);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popupWindow.setElevation(16);
            popupWindow.setBackgroundDrawable(Utils.getDrawable(context, R.drawable.ms__drawable));
        } else {
            popupWindow.setBackgroundDrawable(Utils.getDrawable(context, R.drawable.ms__drop_down_shadow));
        }

        setBackgroundColor(backgroundColor);

        popupWindow.setOnDismissListener(() -> {
            if (nothingSelected && onNothingSelectedListener != null) {
                onNothingSelectedListener.onNothingSelected(MaterialSpinner.this);
            }
            if (!hideArrow) {
                animateArrow(false);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        popupWindow.setWidth(MeasureSpec.getSize(widthMeasureSpec));
        popupWindow.setHeight(calculatePopupWindowHeight());
        if (adapter != null) {
            CharSequence currentText = getText();
            String longestItem = currentText.toString();
            for (int i = 0; i < adapter.getCount(); i++) {
                String itemText = adapter.getItemText(i);
                if (itemText.length() > longestItem.length()) {
                    longestItem = itemText;
                }
            }
            setText(longestItem);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setText(currentText);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isEnabled() && isClickable()) {
                if (!popupWindow.isShowing()) {
                    expand();
                } else {
                    collapse();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("state", super.onSaveInstanceState());
        bundle.putInt("selected_index", selectedIndex);
        bundle.putBoolean("nothing_selected", nothingSelected);
        if (popupWindow != null) {
            bundle.putBoolean("is_popup_showing", popupWindow.isShowing());
            collapse();
        } else {
            bundle.putBoolean("is_popup_showing", false);
        }
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable savedState) {
        if (savedState instanceof Bundle) {
            Bundle bundle = (Bundle) savedState;
            selectedIndex = bundle.getInt("selected_index");
            nothingSelected = bundle.getBoolean("nothing_selected");
            if (adapter != null) {
                if (nothingSelected && !TextUtils.isEmpty(hintText)) {
                    setText(hintText);
                } else {
                    setText(adapter.get(selectedIndex).toString());
                }
                adapter.notifyItemSelected(selectedIndex);
            }
            if (bundle.getBoolean("is_popup_showing")) {
                if (popupWindow != null) {
                    post(() -> expand());
                }
            }
            savedState = bundle.getParcelable("state");
        }
        super.onRestoreInstanceState(savedState);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(enabled ? arrowColor : arrowColorDisabled, PorterDuff.Mode.SRC_IN);
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int position) {
        if (adapter != null) {
            if (position >= 0 && position <= adapter.getCount()) {
                adapter.notifyItemSelected(position);
                selectedIndex = position;
                setText(adapter.get(position).toString());
            } else {
                throw new IllegalArgumentException("Position must be lower than adapter count!");
            }
        }
    }

    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener onItemSelectedListener) {
        this.onItemSelectedListener = onItemSelectedListener;
    }

    public void setOnNothingSelectedListener(@Nullable OnNothingSelectedListener onNothingSelectedListener) {
        this.onNothingSelectedListener = onNothingSelectedListener;
    }

    public <T> void setItems(@NonNull T... items) {
        setItems(Arrays.asList(items));
    }

    public <T> void setItems(@NonNull List<T> items) {
        adapter = new MaterialSpinnerAdapter<>(getContext(), items)
                .setPopupPadding(popupPaddingLeft, popupPaddingTop, popupPaddingRight, popupPaddingBottom)
                .setBackgroundSelector(backgroundSelectorItem)
                .setTextColor(textColor);
        setAdapterInternal(adapter);
    }

    public void setAdapter(@NonNull ListAdapter adapter) {
        this.adapter = new MaterialSpinnerAdapterWrapper(getContext(), adapter)
                .setPopupPadding(popupPaddingLeft, popupPaddingTop, popupPaddingRight, popupPaddingBottom)
                .setBackgroundSelector(backgroundSelectorItem)
                .setTextColor(textColor);
        setAdapterInternal(this.adapter);
    }

    public <T> void setAdapter(MaterialSpinnerAdapter<T> adapter) {
        this.adapter = adapter;
        this.adapter.setTextColor(textColor);
        this.adapter.setBackgroundSelector(backgroundSelectorItem);
        this.adapter.setPopupPadding(popupPaddingLeft, popupPaddingTop, popupPaddingRight, popupPaddingBottom);
        setAdapterInternal(adapter);
    }

    private void setAdapterInternal(@NonNull MaterialSpinnerBaseAdapter adapter) {
        boolean shouldResetPopupHeight = listView.getAdapter() != null;
        adapter.setHintEnabled(!TextUtils.isEmpty(hintText));
        listView.setAdapter(adapter);
        if (selectedIndex >= adapter.getCount()) {
            selectedIndex = 0;
        }
        if (adapter.getItems().size() > 0) {
            if (nothingSelected && !TextUtils.isEmpty(hintText)) {
                setText(hintText);
            } else {
                setText(adapter.get(selectedIndex).toString());
            }
        } else {
            setText("");
        }
        if (shouldResetPopupHeight) {
            popupWindow.setHeight(calculatePopupWindowHeight());
        }
    }

    public <T> List<T> getItems() {
        if (adapter == null) {
            return null;
        }
        return adapter.getItems();
    }

    public void expand() {
        if (canShowPopup()) {
            if (!hideArrow) {
                animateArrow(true);
            }
            nothingSelected = true;
            popupWindow.showAsDropDown(this);
        }
    }

    public void collapse() {
        if (!hideArrow) {
            animateArrow(false);
        }
        popupWindow.dismiss();
    }

    public void setArrowColor(@ColorInt int color) {
        arrowColor = color;
        arrowColorDisabled = Utils.lighter(arrowColor, 0.8f);
        if (arrowDrawable != null) {
            arrowDrawable.setColorFilter(arrowColor, PorterDuff.Mode.SRC_IN);
        }
    }

    private boolean canShowPopup() {
        Activity activity = getActivity();
        if (activity == null || activity.isFinishing()) {
            return false;
        }
        boolean isLaidOut;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            isLaidOut = isLaidOut();
        } else {
            isLaidOut = getWidth() > 0 && getHeight() > 0;
        }
        return isLaidOut;
    }

    private Activity getActivity() {
        Context context = getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void animateArrow(boolean shouldRotateUp) {
        int start = shouldRotateUp ? 0 : 10000;
        int end = shouldRotateUp ? 10000 : 0;
        ObjectAnimator animator = ObjectAnimator.ofInt(arrowDrawable, "level", start, end);
        animator.start();
    }

    public void setDropdownMaxHeight(int height) {
        popupWindowMaxHeight = height;
        popupWindow.setHeight(calculatePopupWindowHeight());
    }

    public void setDropdownHeight(int height) {
        popupWindowHeight = height;
        popupWindow.setHeight(calculatePopupWindowHeight());
    }

    private int calculatePopupWindowHeight() {
        if (adapter == null) {
            return WindowManager.LayoutParams.WRAP_CONTENT;
        }
        float itemHeight = getResources().getDimension(R.dimen.ms__item_height);
        float listViewHeight = adapter.getCount() * itemHeight;
        if (popupWindowMaxHeight > 0 && listViewHeight > popupWindowMaxHeight) {
            return popupWindowMaxHeight;
        } else if (popupWindowHeight != WindowManager.LayoutParams.MATCH_PARENT
                && popupWindowHeight != WindowManager.LayoutParams.WRAP_CONTENT
                && popupWindowHeight <= listViewHeight) {
            return popupWindowHeight;
        } else if (listViewHeight == 0 && adapter.getItems().size() == 1) {
            return (int) itemHeight;
        }
        return WindowManager.LayoutParams.WRAP_CONTENT;
    }

    public PopupWindow getPopupWindow() {
        return popupWindow;
    }

    public ListView getListView() {
        return listView;
    }

    public interface OnItemSelectedListener<T> {
        void onItemSelected(MaterialSpinner view, int position, long id, T item);
    }

    public interface OnNothingSelectedListener {
        void onNothingSelected(MaterialSpinner spinner);
    }
}