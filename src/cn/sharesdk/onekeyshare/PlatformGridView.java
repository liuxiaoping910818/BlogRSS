/*
 * 瀹樼綉鍦扮珯:http://www.ShareSDK.cn
 * 鎶�湳鏀寔QQ: 4006852216
 * 瀹樻柟寰俊:ShareSDK   锛堝鏋滃彂甯冩柊鐗堟湰鐨勮瘽锛屾垜浠皢浼氱涓�椂闂撮�杩囧井淇″皢鐗堟湰鏇存柊鍐呭鎺ㄩ�缁欐偍銆傚鏋滀娇鐢ㄨ繃绋嬩腑鏈変换浣曢棶棰橈紝涔熷彲浠ラ�杩囧井淇′笌鎴戜滑鍙栧緱鑱旂郴锛屾垜浠皢浼氬湪24灏忔椂鍐呯粰浜堝洖澶嶏級
 *
 * Copyright (c) 2013骞�ShareSDK.cn. All rights reserved.
 */

package cn.sharesdk.onekeyshare;

import static cn.sharesdk.framework.utils.R.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import m.framework.ui.widget.viewpager.ViewPagerAdapter;
import m.framework.ui.widget.viewpager.ViewPagerClassic;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;
import cn.sharesdk.framework.CustomPlatform;
import cn.sharesdk.framework.FakeActivity;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.utils.UIHandler;

/** 骞冲彴瀹牸鍒楄〃鏄剧ず宸ュ叿銆�*/
public class PlatformGridView extends LinearLayout implements
		OnClickListener, Callback {
	private static final int MIN_CLICK_INTERVAL = 1000;
	private static final int MSG_PLATFORM_LIST_GOT = 1;
	// 姣忚鏄剧ず鐨勬牸鏁�
	private int LINE_PER_PAGE;
	// 姣忛〉鏄剧ず鐨勮鏁�
	private int COLUMN_PER_LINE;
	// 姣忛〉鏄剧ず鐨勬牸鏁�
	private int PAGE_SIZE;
	// 瀹牸瀹瑰櫒
	private ViewPagerClassic pager;
	// 椤甸潰鎸囩ず鍣�
	private ImageView[] points;
	private Bitmap grayPoint;
	private Bitmap whitePoint;
	// 鏄惁涓嶈烦杞珽ditPage鑰岀洿鎺ュ垎浜�
	private boolean silent;
	// 骞冲彴鏁版嵁
	private Platform[] platformList;
	// 浠庡閮ㄤ紶杩涙潵鐨勫垎浜暟鎹紙鍚垵濮嬪寲鏁版嵁锛�
	private HashMap<String, Object> reqData;
	private OnekeyShare parent;
	private ArrayList<CustomerLogo> customers;
	private HashMap<String, String> hiddenPlatforms;
	private View bgView;
	private long lastClickTime;

	public PlatformGridView(Context context) {
		super(context);
		init(context);
	}

	public PlatformGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(final Context context) {
		calPageSize();
		setOrientation(VERTICAL);

		pager = new ViewPagerClassic(context);
		disableOverScrollMode(pager);
		pager.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		addView(pager);

		// 涓轰簡鏇村ソ鐨剈i鏁堟灉锛屽紑鍚瓙绾跨▼鑾峰彇骞冲彴鍒楄〃
		new Thread() {
			public void run() {
				platformList = ShareSDK.getPlatformList();
				if (platformList == null) {
					platformList = new Platform[0];
				}
				UIHandler.sendEmptyMessage(MSG_PLATFORM_LIST_GOT, PlatformGridView.this);
			}
		}.start();
	}

	private void calPageSize() {
		float scrW = cn.sharesdk.framework.utils.R.getScreenWidth(getContext());
		float scrH = cn.sharesdk.framework.utils.R.getScreenHeight(getContext());
		float whR = scrW / scrH;
		if (whR < 0.6) {
			COLUMN_PER_LINE = 3;
			LINE_PER_PAGE = 3;
		} else if (whR < 0.75) {
			COLUMN_PER_LINE = 3;
			LINE_PER_PAGE = 2;
		} else {
			LINE_PER_PAGE = 1;
			if (whR >= 1.75) {
				COLUMN_PER_LINE = 6;
			} else if (whR >= 1.5) {
				COLUMN_PER_LINE = 5;
			} else if (whR >= 1.3) {
				COLUMN_PER_LINE = 4;
			} else {
				COLUMN_PER_LINE = 3;
			}
		}
		PAGE_SIZE = COLUMN_PER_LINE * LINE_PER_PAGE;
	}

	public boolean handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_PLATFORM_LIST_GOT: {
				afterPlatformListGot();
			}
			break;
		}
		return false;
	}

	/** 鍒濆鍖栧鏍煎垪琛╱i */
	public void afterPlatformListGot() {
		PlatformAdapter adapter = new PlatformAdapter(this);
		pager.setAdapter(adapter);
		int pageCount = 0;
		if (platformList != null) {
			int cusSize = customers == null ? 0 : customers.size();
			int platSize = platformList == null ? 0 : platformList.length;
			int hideSize = hiddenPlatforms == null ? 0 : hiddenPlatforms.size();
			platSize = platSize-hideSize;
			int size = platSize + cusSize;
			pageCount = size / PAGE_SIZE;
			if (size % PAGE_SIZE > 0) {
				pageCount++;
			}
		}
		points = new ImageView[pageCount];
		if (points.length <= 0) {
			return;
		}

		Context context = getContext();
		LinearLayout llPoints = new LinearLayout(context);
		// 濡傛灉椤甸潰鎬绘暟瓒呰繃1锛屽垯璁剧疆椤甸潰鎸囩ず鍣�
		llPoints.setVisibility(pageCount > 1 ? View.VISIBLE: View.GONE);
		LayoutParams lpLl = new LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lpLl.gravity = Gravity.CENTER_HORIZONTAL;
		llPoints.setLayoutParams(lpLl);
		addView(llPoints);

		int dp_5 = cn.sharesdk.framework.utils.R.dipToPx(context, 5);
		int resId = getBitmapRes(getContext(), "gray_point");
		if (resId > 0) {
			grayPoint = BitmapFactory.decodeResource(getResources(), resId);
		}
		resId = getBitmapRes(getContext(), "white_point");
		if (resId > 0) {
			whitePoint = BitmapFactory.decodeResource(getResources(), resId);
		}
		for (int i = 0; i < pageCount; i++) {
			points[i] = new ImageView(context);
			points[i].setScaleType(ScaleType.CENTER_INSIDE);
			points[i].setImageBitmap(grayPoint);
			LayoutParams lpIv = new LayoutParams(dp_5, dp_5);
			lpIv.setMargins(dp_5, dp_5, dp_5, 0);
			points[i].setLayoutParams(lpIv);
			llPoints.addView(points[i]);
		}
		int curPage = pager.getCurrentScreen();
		points[curPage].setImageBitmap(whitePoint);
	}

	/** 灞忓箷鏃嬭浆鍚庯紝姝ゆ柟娉曚細琚皟鐢紝浠ュ埛鏂板鏍煎垪琛ㄧ殑甯冨眬 */
	public void onConfigurationChanged() {
		int curFirst = pager.getCurrentScreen() * PAGE_SIZE;
		calPageSize();
		int newPage = curFirst / PAGE_SIZE;

		removeViewAt(1);
		afterPlatformListGot();

		pager.setCurrentScreen(newPage);
	}

	public void setData(HashMap<String, Object> data, boolean silent) {
		reqData = data;
		this.silent = silent;
	}

	public void setHiddenPlatforms(HashMap<String, String> hiddenPlatforms) {
		this.hiddenPlatforms = hiddenPlatforms;
	}

	/** 璁剧疆鑷繁鍥炬爣鐨勭偣鍑讳簨浠�*/
	public void setCustomerLogos(ArrayList<CustomerLogo> customers) {
		this.customers = customers;
	}

	public void setEditPageBackground(View bgView) {
		this.bgView = bgView;
	}

	/** 璁剧疆鍒嗕韩鎿嶄綔鐨勫洖璋冮〉闈�*/
	public void setParent(OnekeyShare parent) {
		this.parent = parent;
	}

	public void onClick(View v) {
		long time = System.currentTimeMillis();
		if (time - lastClickTime < MIN_CLICK_INTERVAL) {
			return;
		}
		lastClickTime = time;

		Platform plat = (Platform) v.getTag();
		if (plat != null) {
			if (silent) {
				HashMap<Platform, HashMap<String, Object>> shareData
						= new HashMap<Platform, HashMap<String,Object>>();
				shareData.put(plat, reqData);
				parent.share(shareData);
				return;
			}

			String name = plat.getName();
			reqData.put("platform", name);
			// EditPage涓嶆敮鎸佸井淇″钩鍙般�Google+銆丵Q鍒嗕韩銆丳interest銆佷俊鎭拰閭欢锛屾�鏄墽琛岀洿鎺ュ垎浜�
			if ((plat instanceof CustomPlatform)
					|| ShareCore.isUseClientToShare(name)) {
				HashMap<Platform, HashMap<String, Object>> shareData
						= new HashMap<Platform, HashMap<String,Object>>();
				shareData.put(plat, reqData);
				parent.share(shareData);
				return;
			}

			// 璺宠浆EditPage鍒嗕韩
			EditPage page = new EditPage();
			page.setBackGround(bgView);
			bgView = null;
			page.setShareData(reqData);
			if ("true".equals(String.valueOf(reqData.get("dialogMode")))) {
				page.setDialogMode();
			}
			page.showForResult(parent.getContext(), null, new FakeActivity() {
				public void onResult(HashMap<String, Object> data) {
					if (data != null && data.containsKey("editRes")) {
						@SuppressWarnings("unchecked")
						HashMap<Platform, HashMap<String, Object>> editRes
								= (HashMap<Platform, HashMap<String, Object>>) data.get("editRes");
						parent.share(editRes);
					}
				}
			});
			parent.finish();
		}
	}

	// 绂佺敤椤甸潰婊氬姩鐨勨�鍙戝厜鈥濇晥鏋�
	private void disableOverScrollMode(View view) {
		if (Build.VERSION.SDK_INT < 9) {
			return;
		}
		try {
			Method m = View.class.getMethod("setOverScrollMode",
					new Class[] { Integer.TYPE });
			m.setAccessible(true);
			m.invoke(view, new Object[] { Integer.valueOf(2) });
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	/** 瀹牸鍒楄〃鏁版嵁閫傞厤鍣�*/
	private static class PlatformAdapter extends ViewPagerAdapter {
		private GridView[] girds;
		private List<Object> logos;
		private OnClickListener callback;
		private int lines;
		private PlatformGridView platformGridView;

		public PlatformAdapter(PlatformGridView platformGridView) {
			this.platformGridView = platformGridView;
			logos = new ArrayList<Object>();
			Platform[] platforms = platformGridView.platformList;
			HashMap<String, String> hiddenPlatforms = platformGridView.hiddenPlatforms;
			if (platforms != null) {
				if (hiddenPlatforms != null && hiddenPlatforms.size() > 0) {
					ArrayList<Platform> ps = new ArrayList<Platform>();
					for (Platform p : platforms) {
						if (hiddenPlatforms.containsKey(p.getName())) {
							continue;
						}
						ps.add(p);
					}

					platforms = new Platform[ps.size()];
					for (int i = 0; i < platforms.length; i++) {
						platforms[i] = ps.get(i);
					}
				}

				logos.addAll(Arrays.asList(platforms));
			}
			ArrayList<CustomerLogo> customers = platformGridView.customers;
			if (customers != null) {
				logos.addAll(customers);
			}
			this.callback = platformGridView;
			girds = null;

			if (logos != null) {
				int size = logos.size();
				int PAGE_SIZE = platformGridView.PAGE_SIZE;
				int pageCount = size / PAGE_SIZE;
				if (size % PAGE_SIZE > 0) {
					pageCount++;
				}
				girds = new GridView[pageCount];
			}
		}

		public int getCount() {
			return girds == null ? 0 : girds.length;
		}

		public View getView(int position, ViewGroup parent) {
			if (girds[position] == null) {
				int pageSize = platformGridView.PAGE_SIZE;
				int curSize = pageSize * position;
				int listSize = logos == null ? 0 : logos.size();
				if (curSize + pageSize > listSize) {
					pageSize = listSize - curSize;
				}
				Object[] gridBean = new Object[pageSize];
				for (int i = 0; i < pageSize; i++) {
					gridBean[i] = logos.get(curSize + i);
				}

				if (position == 0) {
					int COLUMN_PER_LINE = platformGridView.COLUMN_PER_LINE;
					lines = gridBean.length / COLUMN_PER_LINE;
					if (gridBean.length % COLUMN_PER_LINE > 0) {
						lines++;
					}
				}
				girds[position] = new GridView(this);
				girds[position].setData(lines, gridBean);
			}

			return girds[position];
		}

		/** 灞忓箷婊戝姩鍚庯紝姝ゆ柟娉曚細琚皟鐢�*/
		public void onScreenChange(int currentScreen, int lastScreen) {
			ImageView[] points = platformGridView.points;
			for (int i = 0; i < points.length; i++) {
				points[i].setImageBitmap(platformGridView.grayPoint);
			}

			points[currentScreen].setImageBitmap(platformGridView.whitePoint);
		}

	}

	/** 绠�槗鐨勫鏍煎垪琛ㄦ帶浠�*/
	private static class GridView extends LinearLayout {
		private Object[] beans;
		private OnClickListener callback;
		private int lines;
		private PlatformAdapter platformAdapter;

		public GridView(PlatformAdapter platformAdapter) {
			super(platformAdapter.platformGridView.getContext());
			this.platformAdapter = platformAdapter;
			this.callback = platformAdapter.callback;
		}

		public void setData(int lines, Object[] beans) {
			this.lines = lines;
			this.beans = beans;
			init();
		}

		private void init() {
			int dp_5 = cn.sharesdk.framework.utils.R.dipToPx(getContext(), 5);
			setPadding(0, dp_5, 0, dp_5);
			setOrientation(VERTICAL);

			int size = beans == null ? 0 : beans.length;
			int COLUMN_PER_LINE = platformAdapter.platformGridView.COLUMN_PER_LINE;
			int lineSize = size / COLUMN_PER_LINE;
			if (size % COLUMN_PER_LINE > 0) {
				lineSize++;
			}
			LayoutParams lp = new LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			lp.weight = 1;
			for (int i = 0; i < lines; i++) {
				LinearLayout llLine = new LinearLayout(getContext());
				llLine.setLayoutParams(lp);
				llLine.setPadding(dp_5, 0, dp_5, 0);
				addView(llLine);

				if (i >= lineSize) {
					continue;
				}

				for (int j = 0; j < COLUMN_PER_LINE; j++) {
					final int index = i * COLUMN_PER_LINE + j;
					if (index >= size) {
						LinearLayout llItem = new LinearLayout(getContext());
						llItem.setLayoutParams(lp);
						llLine.addView(llItem);
						continue;
					}

					final LinearLayout llItem = getView(index, callback, getContext());
					llItem.setTag(beans[index]);
					llItem.setLayoutParams(lp);
					llLine.addView(llItem);
				}
			}
		}

		private LinearLayout getView(int position, OnClickListener ocL, Context context) {
			Bitmap logo;
			String label;
			OnClickListener listener;
			if (beans[position] instanceof Platform) {
				logo = getIcon((Platform) beans[position]);
				label = getName((Platform) beans[position]);
				listener = ocL;
			} else {
				logo = ((CustomerLogo) beans[position]).logo;
				label = ((CustomerLogo) beans[position]).label;
				listener = ((CustomerLogo) beans[position]).listener;
			}

			LinearLayout ll = new LinearLayout(context);
			ll.setOrientation(LinearLayout.VERTICAL);

			ImageView iv = new ImageView(context);
			int dp_5 = cn.sharesdk.framework.utils.R.dipToPx(context, 5);
			iv.setPadding(dp_5, dp_5, dp_5, dp_5);
			iv.setScaleType(ScaleType.CENTER_INSIDE);
			LinearLayout.LayoutParams lpIv = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpIv.setMargins(dp_5, dp_5, dp_5, dp_5);
			lpIv.gravity = Gravity.CENTER_HORIZONTAL;
			iv.setLayoutParams(lpIv);
			iv.setImageBitmap(logo);
			ll.addView(iv);

			TextView tv = new TextView(context);
			tv.setTextColor(0xffffffff);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			tv.setSingleLine();
			tv.setIncludeFontPadding(false);
			LinearLayout.LayoutParams lpTv = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			lpTv.gravity = Gravity.CENTER_HORIZONTAL;
			lpTv.weight = 1;
			lpTv.setMargins(dp_5, 0, dp_5, dp_5);
			tv.setLayoutParams(lpTv);
			tv.setText(label);
			ll.addView(tv);
			ll.setOnClickListener(listener);

			return ll;
		}

		private Bitmap getIcon(Platform plat) {
			if (plat == null) {
				return null;
			}

			String name = plat.getName();
			if (name == null) {
				return null;
			}

			String resName = "logo_" + plat.getName();
			int resId = getBitmapRes(getContext(), resName);
			return BitmapFactory.decodeResource(getResources(), resId);
		}

		private String getName(Platform plat) {
			if (plat == null) {
				return "";
			}

			String name = plat.getName();
			if (name == null) {
				return "";
			}

			int resId = cn.sharesdk.framework.utils.R.getStringRes(getContext(), plat.getName());
			if (resId > 0) {
				return getContext().getString(resId);
			}
			return null;
		}

	}

}
