package application;

import java.util.concurrent.TimeUnit;

import javax.tools.JavaFileObject;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import android.app.Activity;

public class AndroidActivity extends Activity {
	
	/**
	 * When testing with JDK 17 it seems that only one error per line is reported
	 * See com.sun.tools.javac.util.Log.shouldReport(JavaFileObject file, int pos)
	 */
	@Override
	@SuppressWarnings("CheckReturnValue")
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
