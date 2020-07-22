package com.kopykitab.ereader.components.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.kopykitab.ereader.R;
import com.kopykitab.ereader.components.SyncDataFromAPI;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class MenuListAdapter extends BaseExpandableListAdapter implements ExpandableListView.OnChildClickListener {

    private Context mContext;
    private String loadingMessage;
    private DrawerLayout drawerLayout;
    private List<String> menuGroups;
    private LinkedHashMap<String, List<HashMap<String, String>>> menuGroupsItems;

    public MenuListAdapter(Context context, String loadingMessage, DrawerLayout drawerLayout) {
        mContext = context;
        this.loadingMessage = loadingMessage;
        this.drawerLayout = drawerLayout;

        menuGroups = new ArrayList<String>();
        menuGroupsItems = new LinkedHashMap<String, List<HashMap<String, String>>>();
        menuGroupsItems.put("Customer Detail", new ArrayList<HashMap<String, String>>());
        menuGroupsItems.put("My Library", new ArrayList<HashMap<String, String>>());

        List<HashMap<String, String>> items = new ArrayList<HashMap<String, String>>();
        items.add(createMenuItem("Choose your Classes", "streams_icon_colored", null));
        items.add(createMenuItem("Sync", "refresh_icon_colored", null));
        items.add(createMenuItem("Store", "store_icon_colored", null));
        items.add(createMenuItem("Cart", "cart_icon_colored", null));
        items.add(createMenuItem("Scorecard", "scorecard_icon", null));
        items.add(createMenuItem("Notifications", "notification_icon_colored", null));
        menuGroupsItems.put("My Account", items);

        items = new ArrayList<HashMap<String, String>>();
        items.add(createMenuItem("Feedback", "feedback_icon_colored", null));
        items.add(createMenuItem("Settings", "settings_icon_colored", null));
        items.add(createMenuItem("goPremium", "gopremium_icon_colored", null));
        menuGroupsItems.put("Other", items);

        menuGroups = new ArrayList<String>(menuGroupsItems.keySet());
    }

    public HashMap<String, String> createMenuItem(String name, String icon, String intentExtras) {
        HashMap<String, String> item = new HashMap<String, String>();
        item.put("name", name);
        item.put("icon", icon);
        item.put("intent_extras", intentExtras);

        return item;
    }

    public void setMenuGroupsItems(String groupName, List<HashMap<String, String>> items) {
        menuGroupsItems.put(groupName, items);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        // TODO Auto-generated method stub
        return menuGroups.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // TODO Auto-generated method stub
        return menuGroupsItems.get(menuGroups.get(groupPosition)).size();
    }

    @Override
    public List<HashMap<String, String>> getGroup(int groupPosition) {
        // TODO Auto-generated method stub
        return menuGroupsItems.get(menuGroups.get(groupPosition));
    }

    @Override
    public HashMap<String, String> getChild(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return getGroup(groupPosition).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        // TODO Auto-generated method stub
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        if (groupPosition > 0) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.menu_list_section_header, parent, false);
            ((TextView) convertView.findViewById(R.id.section_header_text)).setText(menuGroups.get(groupPosition));
        } else {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.menu_list_item, parent, false);
            com.kopykitab.ereader.components.TextView userName = (com.kopykitab.ereader.components.TextView) convertView.findViewById(R.id.menu_list_option_text);
            userName.setText(AppSettings.getInstance(mContext).get("customer_name"));
            userName.setTextColor(Color.WHITE);
            ((ImageView) convertView.findViewById(R.id.menu_list_option_icon)).setImageResource(mContext.getResources().getIdentifier("drawable/user_icon", null, mContext.getPackageName()));
            convertView.setBackgroundResource(R.drawable.button_state_2);
            convertView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    // TODO Auto-generated method stub
                    Utils.showProfileActivity(mContext);
                    drawerLayout.closeDrawer(Gravity.LEFT);
                }
            });
        }
        ExpandableListView mExpandableListView = (ExpandableListView) parent;
        mExpandableListView.expandGroup(groupPosition);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        if (groupPosition > 0) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.menu_list_item, parent, false);
            HashMap<String, String> item = getChild(groupPosition, childPosition);
            ((TextView) convertView.findViewById(R.id.menu_list_option_text)).setText(item.get("name"));
            ImageView menuIconView = (ImageView) convertView.findViewById(R.id.menu_list_option_icon);
            if (groupPosition == 1) {
                Utils.getImageLoader(mContext).displayImage(Constants.ICON_URL + item.get("icon") + ".png", menuIconView);
            } else {
                menuIconView.setImageResource(mContext.getResources().getIdentifier("drawable/" + item.get("icon"), null, mContext.getPackageName()));
            }

            convertView.setBackgroundResource(R.drawable.menu_list_item_state);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        // TODO Auto-generated method stub
        if (groupPosition > 0) {
            if (groupPosition == 1) {
                Utils.showLibrary(mContext, getChild(groupPosition, childPosition).get("intent_extras"));
            } else if (groupPosition == 2) {
                switch (childPosition) {
                    case 0:
                        Utils.showStreamsSelectionActivity(mContext);
                        break;
                    case 1:
                        if (Utils.isNetworkConnected(mContext)) {
                            new SyncDataFromAPI(mContext, loadingMessage).execute();
                        } else {
                            Utils.networkNotAvailableAlertBox(mContext);
                        }
                        break;
                    case 2:
                        Utils.showStoreActivity(mContext);
                        break;
                    case 3:
                        Utils.showCartActivity(mContext);
                        break;
                    case 4:
                        Utils.showScorecardActivity(mContext);
                        break;
                    case 5:
                        Utils.showNotificationsActivity(mContext);
                        break;
                }
            } else if (groupPosition == 3) {
                switch (childPosition) {
                    case 0:
                        Utils.showFeedbackActivity(mContext);
                        break;
                    case 1:
                        Utils.showSettingsActivity(mContext);
                        break;
                    case 2:
                        if (Utils.isNetworkConnected(mContext)) {
                            Utils.showPremiumActivity(mContext);
                        } else {
                            Utils.networkNotAvailableAlertBox(mContext);
                        }
                        break;
                }
            }
        }
        drawerLayout.closeDrawer(Gravity.LEFT);
        return false;
    }
}
