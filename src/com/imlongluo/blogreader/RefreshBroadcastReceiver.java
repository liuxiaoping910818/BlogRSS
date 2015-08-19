package  com.imlongluo.blogreader;

import com.imlongluo.blogreader.service.FetcherService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RefreshBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Constants.ACTION_REFRESHFEEDS.equals(intent.getAction())) {
			context.startService(new Intent(context, FetcherService.class).putExtras(intent)); // a thread would mark the process as inactive
		} else if (Constants.ACTION_STOPREFRESHFEEDS.equals(intent.getAction())) {
			context.stopService(new Intent(context, FetcherService.class));
		}
	}
	
}
