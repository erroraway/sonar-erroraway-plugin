package com.bug;

import java.util.concurrent.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import android.app.*;

public class AndroidActivity extends Activity {
	
	@Override
	protected void onPause() {
	    Observable
        .interval(1, TimeUnit.SECONDS)
        .subscribe(new Consumer<Long>() {
          @Override public void accept(Long interval) throws Exception {
            System.out.println(interval);
          }
        });
	}
}
