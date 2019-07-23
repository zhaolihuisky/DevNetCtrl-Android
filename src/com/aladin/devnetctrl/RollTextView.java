package com.aladin.devnetctrl;

import android.content.Context;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.widget.TextView;

public class RollTextView extends TextView {
	public RollTextView(Context context) {
		super(context);
	}

	public RollTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setSingleLine();
		this.setEllipsize(TruncateAt.MARQUEE);
		this.setMarqueeRepeatLimit(-1);
	}

	public RollTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isFocused() {
		return true;
	}
}
