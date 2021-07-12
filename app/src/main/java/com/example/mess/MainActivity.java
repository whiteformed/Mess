package com.example.mess;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.example.mess.ActivityHierarchy.LoggedBasicActivity;
import com.example.mess.Fragments.ChatsFragment;
import com.example.mess.Fragments.UsersFragment;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import static com.example.mess.Mess.ONLINE;

public class MainActivity extends LoggedBasicActivity {
    public static int PAGE_USERS = 1;

    private Mess application;

    private ViewPager viewPager;

    private ChatsFragment chatsFragment;
    private UsersFragment usersFragment;

    private boolean isActivityActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        application = (Mess) getApplication();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("");

        application.readLoggedUser(this);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());

        chatsFragment = new ChatsFragment();
        usersFragment = new UsersFragment();
        adapter.addFragment(chatsFragment, getString(R.string.chats));
        adapter.addFragment(usersFragment, getString(R.string.users));

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_options, menu);

        MenuItem logoutItem = menu.findItem(R.id.actionLogout);
        MenuItem searchItem = menu.findItem(R.id.actionSearch);
        MenuItem settingsItem = menu.findItem(R.id.actionSettings);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                usersFragment.onQueryTextChange(newText);
                return false;
            }
        });

        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                logoutItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                settingsItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                searchItem.setVisible(false);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                invalidateOptionsMenu();
                usersFragment.toggleNoMatchesText(false);
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionLogout:
                showLogoutDialog();
                return true;
            case R.id.actionSearch:
                viewPager.setCurrentItem(PAGE_USERS, true);
                return true;
            case R.id.actionSettings:
                if (application.getLoggedUser() != null)
                    startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.logout_message))
                .setPositiveButton(getString(R.string.yes), (dialog, which) ->
                        application.signOut(MainActivity.this))
                .setNegativeButton(getString(R.string.no), null)
                .create().show();
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private ArrayList<Fragment> fragments;
        private ArrayList<String> titles;

        ViewPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            fragments = new ArrayList<>();
            titles = new ArrayList<>();
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        void addFragment(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            return titles.get(position);
        }
    }

    public void toggleNoUserMatchesText(boolean visible) {
        usersFragment.toggleNoMatchesText(visible);
    }

    @Override
    protected void onResume() {
        application.setCurrentActivity(this);
        application.setAppActive(true);
        isActivityActive = true;
        super.onResume();
        application.updateUserStatus(ONLINE);
        if (chatsFragment != null) chatsFragment.handleChanges();
        if (usersFragment != null) usersFragment.handleChanges();
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.setCurrentActivity(null);
        application.setAppActive(false);
        isActivityActive = false;
        application.disconnectUserDelayed();
    }

    public boolean isActivityNotActive() {
        return !isActivityActive;
    }

    public ChatsFragment getChatsFragment() {
        return chatsFragment;
    }
}
