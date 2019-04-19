package com.sunfusheng.spi.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.sunfusheng.spi.core.ServiceProvider;
import com.sunfusheng.spi.module.interfaces.AbsApplicationDelegate;
import com.sunfusheng.spi.module.interfaces.AbsMainFragment;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView vInfo = findViewById(R.id.vInfo);

        StringBuilder sb = new StringBuilder();
        List<AbsApplicationDelegate> applicationDelegates = ServiceProvider.getProviders(AbsApplicationDelegate.class);
        sb.append("【AbsApplicationDelegate List】: ");
        for (AbsApplicationDelegate delegate : applicationDelegates) {
            sb.append("\n").append(delegate.getClass().getSimpleName());
        }

        List<AbsMainFragment> mainFragments = ServiceProvider.getProviders(AbsMainFragment.class);
        sb.append("\n\n【IMainFragment List】: ");
        for (AbsMainFragment fragment : mainFragments) {
            sb.append("\n").append(fragment.getClass().getSimpleName());
        }

        vInfo.setText(sb);
    }

}
