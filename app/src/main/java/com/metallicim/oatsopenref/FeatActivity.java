package com.metallicim.oatsopenref;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;

public class FeatActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.metallicim.oatsopenref.MESSAGE";
    public static final String EXTRA_MESSAGE_NAME = "com.metallicim.oatsopenref.MESSAGE_NAME";

    String pageLink;
    String pageName;
    LinearLayout container;
    JSONArray contents;
    JSONArray feats;
    int headerTextSize = 40;
    Menu mMenu;

    int mThemeID;
    Bookmarks mBookmarks = Bookmarks.getInstance();

    int boxBorder;
    int boxHeaderTextColor;
    int boxInnerBackgroundColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mThemeID = setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feat);

        // Setup message from start intent
        Intent intent = getIntent();
        pageLink = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        pageName = intent.getStringExtra(MainActivity.EXTRA_MESSAGE_NAME);

        // setup toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // setup parent container that everything goes in
        container = (LinearLayout) findViewById(R.id.linear_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // get bookmarks
        mBookmarks = Bookmarks.getInstance();

        // remove everything already in the view
        LinearLayout layout = findViewById(R.id.linear_layout);

        // Fill the page using the XML file pointed to
        try {
            parse(pageLink);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int themeID = setTheme();
        if (mThemeID != themeID) {
            this.recreate();
        }
    }

    private int setTheme() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        String themeColor = sharedPreferences.getString("color", "");
        ParseTheme parseTheme = new ParseTheme();
        int themeID = parseTheme.parseThemeColor(themeColor);
        super.setTheme(themeID);

        Resources.Theme theme = this.getTheme();
        theme.applyStyle(themeID, true);
        TypedValue color = new TypedValue();
        theme.resolveAttribute(android.R.attr.color, color, true);
        boxBorder = color.data;
        theme.resolveAttribute(android.R.attr.keyTextColor, color, true);
        boxHeaderTextColor = color.data;
        theme.resolveAttribute(android.R.attr.shadowColor, color, true);
        boxInnerBackgroundColor = color.data;

        return themeID;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.content_menu, menu);
        mMenu = menu;
        if (mBookmarks.isBookmarked(pageLink)) {
            mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_24dp);
        } else {
            mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_border_24dp);
        }
        return true;
    }

    private AlertDialog askForCollection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.remove_collections);
        final List<String> selectedItems = new ArrayList<>();
        final Bookmarks bookmarks = Bookmarks.getInstance();
        int length = bookmarks.collectionsLength() - 1;
        CharSequence[] items = new CharSequence[length];
        CharSequence[] links = new CharSequence[length];
        boolean[] checkedItems = new boolean[length];
        for (int i = 0; i < length; i++) {
            items[i] = bookmarks.getCollectionName(i+1);
            links[i] = bookmarks.getCollectionLink(i+1);
            checkedItems[i] = bookmarks.bookmarkIsInCollection(links[i].toString(),
                    bookmarks.findBookmarkIndexByLink(pageLink));
            if (checkedItems[i]) {
                selectedItems.add(links[i].toString());
            }
        }
        final CharSequence[] finalLinks = links;
        builder.setMultiChoiceItems(items, checkedItems,
                new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        CharSequence link = finalLinks[which];
                        if (isChecked) {
                            selectedItems.add(link.toString());
                        } else selectedItems.remove(link.toString());
                    }
                });

        final Context context = this;
        final int index = bookmarks.findBookmarkIndexByLink(pageLink);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // user clicked ok
                if (index >= 0) { // if the bookmark exists
                    bookmarks.setBookmarkCollections(selectedItems, index);
                } else { // if the bookmark doesn't exist
                    bookmarks.addBookmark(selectedItems, pageName, pageLink, PageType.feat);
                }
                mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_24dp);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // user clicked cancel
            }
        });
        builder.setNeutralButton(R.string.delete_bookmark, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // user deletes bookmark
                bookmarks.removeBookmark("_all_", pageLink);
                mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_border_24dp);
            }
        });

        return builder.create();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                super.onBackPressed();
                return super.onOptionsItemSelected(item);
            case R.id.action_about:
                intent = new Intent(this, XMLActivity.class);
                String message = "About.xml";
                intent.putExtra(EXTRA_MESSAGE, message);
                intent.putExtra(EXTRA_MESSAGE_NAME, "About");
                startActivity(intent);
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_bookmark:
                if (mBookmarks.collectionsLength() > 1) {
                    // ask for collection
                    askForCollection().show();
                } else {
                    if (!mBookmarks.isBookmarked(pageLink)) {
                        mBookmarks.addBookmark(pageName, pageLink, PageType.feat);
                        mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_24dp);
                    } else {
                        mBookmarks.removeBookmark("_all_", pageLink);
                        mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_bookmark_border_24dp);
                    }
                    mBookmarks.updateFile(this);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // binary search through the feats
    private int getFeatIndex(JSONArray array) throws JSONException {
        int start = 0;
        int end = array.length() - 1;
        int foundIndex = 0;
        while (start <= end) {
            int mid = (start + end) >> 1;// add L and R then divide by 2
            JSONObject obj = (JSONObject) array.get(mid);
            String str = obj.getString("category");
            if (str.compareToIgnoreCase(pageLink) < 0) {
                start = mid + 1;
            } else if (str.compareToIgnoreCase(pageLink) > 0) {
                end = mid - 1;
            } else {
                foundIndex = mid;
                break;
            }
        }
        return foundIndex;
    }

    private void loadFeatsInCategory() throws JSONException {
        int foundIndex = getFeatIndex(contents);
        feats = new JSONArray();
        // loop down through the JSONArray
        int downIndex = foundIndex;
        JSONObject nextObj;
        do {
            JSONObject obj = (JSONObject) contents.get(downIndex);
            feats.put(obj);
            if (downIndex <= 0)
                break;
            nextObj = (JSONObject) contents.get(--downIndex);
        } while(nextObj.getString("category").equals(pageLink));
        // reverse the array
        JSONArray featsTemp = new JSONArray();
        for (int i = feats.length() - 1; i >= 0; i--) {
            featsTemp.put(feats.get(i));
        }
        feats = featsTemp;
        // loop up through the JSONArray
        int upIndex = foundIndex;
        nextObj = (JSONObject) contents.get(++upIndex);
        while(nextObj.getString("category").equals(pageLink)) {
            JSONObject obj = (JSONObject) contents.get(upIndex);
            feats.put(obj);
            if (upIndex >= contents.length()-1)
                break;
            nextObj = (JSONObject) contents.get(++upIndex);
        }
    }

    private void parse(String pageLink) throws JSONException, IOException {
        loadJSONFromAsset(getApplicationContext());
        loadFeatsInCategory();

        displayFeats();
    }

    public TextView featBaseTextView() {
        TextView textView = new TextView(this);
        textView.setPadding(15, 0,15,0);
        return textView;
    }

    public TextView featElementTextView (String text) {
        TextView textView = featBaseTextView();
        textView.setText(text);
        return textView;
    }
    public TextView featElementTextView (SpannableStringBuilder text) {
        TextView textView = featBaseTextView();
        textView.setText(text);
        return textView;
    }

    public SpannableStringBuilder featElement (String title, String text, boolean indent, boolean plural) {
        SpannableStringBuilder featTitle = new SpannableStringBuilder("");
        if (indent)
            featTitle.append("  ");
        featTitle.append(title);
        if (plural)
            featTitle.append("s");
        featTitle.append(" ");
        featTitle.setSpan(new StyleSpan(Typeface.BOLD),
                         0, featTitle.length(),
                         Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableStringBuilder featText = new SpannableStringBuilder(text);
        featTitle.append(featText);
        return featTitle;
    }

    private LinearLayout tutorBox() {
        LinearLayout boxOuter = new LinearLayout(this);
        LinearLayout.LayoutParams boxOuterParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        boxOuterParams.setMargins(10,10,10,10);
        boxOuter.setLayoutParams(boxOuterParams);
        boxOuter.setOrientation(LinearLayout.VERTICAL);
        boxOuter.setBackgroundColor(boxBorder);
        boxOuter.setPadding(10,10,10,10);

        TextView boxHeader = new TextView(this);
        boxHeader.setPadding(15, 0, 15, 5);
        boxHeader.setText("Tutoring");
        boxHeader.setBackgroundColor(boxBorder);
        boxHeader.setTextColor(boxHeaderTextColor);
        boxHeader.setTypeface(null, Typeface.BOLD);
        boxOuter.addView(boxHeader);

        return boxOuter;
    }
    private LinearLayout innerTutorBox() {
        LinearLayout boxInner = new LinearLayout(this);
        LinearLayout.LayoutParams boxInnerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        boxInnerParams.setMargins(0, 0, 0, 0);
        boxInner.setPadding(15, 5, 15, 5);
        boxInner.setLayoutParams(boxInnerParams);
        boxInner.setOrientation(LinearLayout.VERTICAL);
        boxInner.setBackgroundColor(boxInnerBackgroundColor);

        return boxInner;
    }

    public void displayFeats() throws JSONException {
        // TODO category
        for (int i = 0; i < feats.length(); i++) {
            JSONObject obj = (JSONObject) feats.get(i);
            Log.d("OaTS", obj.getString("category") + ": " + obj.getString("name"));
            String category = obj.getString("category");
            String name = obj.getString("name");
            String intro = obj.getString("intro");
            JSONArray prerequisiteJSONArray = obj.getJSONArray("prerequisite");
            List<String> prerequisiteList = new ArrayList<>();
            for (int j = 0; j < prerequisiteJSONArray.length(); j++)
                prerequisiteList.add((String) prerequisiteJSONArray.get(j));
            String benefit = obj.getString("benefit");
            String example = obj.getString("example");
            String normal = obj.getString("normal");
            String special = obj.getString("special");
            JSONObject tutoringJSONObject = obj.getJSONObject("tutoring");
            String tutoringCategories = tutoringJSONObject.getString("categories");
            String tutoringTime = tutoringJSONObject.getString("time");
            String tutoringBenefit = tutoringJSONObject.getString("benefit");

            LinearLayout featLayout = new LinearLayout(this);
            LinearLayout.LayoutParams featLayoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            featLayout.setOrientation(LinearLayout.VERTICAL);
            featLayout.setLayoutParams(featLayoutParams);

            // Start create name header
            TextView nameView = featElementTextView(name);
            nameView.setTextSize((int)(((float)headerTextSize) * 0.75));
            nameView.setTypeface(null, Typeface.BOLD);
            nameView.setGravity(Gravity.END);
            featLayout.addView(nameView);
            // End create name header
            if (!intro.isEmpty()) {
                intro = "  " + intro;
                featLayout.addView(featElementTextView(intro));
            }
            if (prerequisiteList.size() > 0) {
                String strList = "";
                for (int j = 0; j < prerequisiteList.size(); j++) {
                    strList = strList.concat(prerequisiteList.get(j));
                    if (j < prerequisiteList.size() - 1)
                        strList = strList.concat(", ");
                }
                featLayout.addView(featElementTextView(featElement("Prerequisite", strList, true,
                                                                   prerequisiteList.size() > 1)));
            }
            if (!benefit.isEmpty()) {
                featLayout.addView(featElementTextView(
                        featElement("Benefit", benefit, true, false)));
            }
            if (!example.isEmpty()) {
                featLayout.addView(featElementTextView(
                        featElement("Example", example, true, false)));
            }
            if (!normal.isEmpty()) {
                featLayout.addView(featElementTextView(
                        featElement("Normal", normal, true, false)));
            }
            if (!special.isEmpty()) {
                featLayout.addView(featElementTextView(
                        featElement("Special", special, true, false)));
            }

            LinearLayout boxOuter = tutorBox();
            LinearLayout boxInner = innerTutorBox();
            boxOuter.addView(boxInner);

            if (!tutoringCategories.isEmpty()) {
                boxInner.addView(featElementTextView(
                        featElement("Categories", tutoringCategories, false, false)));
            }
            if (!tutoringTime.isEmpty()) {
                boxInner.addView(featElementTextView(
                        featElement("Time", tutoringTime, false, false)));
            }
            if (!tutoringBenefit.isEmpty()) {
                boxInner.addView(featElementTextView(
                        featElement("Benefit", tutoringBenefit, false, false)));
                featLayout.addView(boxOuter);
            }

            container.addView(featLayout);
        }
    }

    public void loadJSONFromAsset(Context context) throws JSONException, IOException {
        String json;
        InputStream is = context.getAssets().open("Feats/feats.json");
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        json = new String(buffer, "UTF-8");
        contents = new JSONArray(json);
    }
}