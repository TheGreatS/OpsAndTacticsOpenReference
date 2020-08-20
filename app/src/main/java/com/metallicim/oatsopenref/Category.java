package com.metallicim.oatsopenref;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;

public class Category {
    private String mName;
    private String mLink;
    private PageType mType;
    private Context mContext;
    private Resources.Theme mTheme;
    public static final String EXTRA_MESSAGE = "com.metallicim.oatsopenref.MESSAGE";

    public Category(Context parentContext, String name, String link, String type) {
        mContext = parentContext;
        mName = name;
        mLink = link;
        if (type.equals("xml")) {
            mType = PageType.XML;
        } else if (type.equals("feat")) {
            mType = PageType.feat;
        } else if (type.equals("add_collection")) {
            mType = PageType.add_collection;
        } else if (type.equals("remove_collection")) {
            mType = PageType.remove_collection;
        } else {
            mType = PageType.error;
        }
    }

    public String getName() {
        return mName;
    }

    public void setTheme(Resources.Theme theme) {
        mTheme = theme;
    }
    public Resources.Theme getTheme() {
        return mTheme;
    }

    public void gotoSubPage() {
        Intent intent = new Intent(mContext, MainActivity.class);
        switch (mType) {
            case XML:
                intent = new Intent(mContext, XMLActivity.class);
                break;
            case feat:
                intent = new Intent(mContext, FeatActivity.class);
                break;
            default:
                break;
        }
        intent.putExtra(EXTRA_MESSAGE, mLink);
        mContext.startActivity(intent);
    }
}
