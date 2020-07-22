package com.artifex.mupdfdemo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.artifex.mupdfdemo.MuPDFActivity.UtilsPDF;
import com.artifex.mupdfdemo.NotesOrBookmarksAdapter.DeleteAnnotationListener;
import com.artifex.mupdfdemo.NotesOrBookmarksAdapter.RefreshPrintListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.R;
import com.kopykitab.ereader.components.CircularProgressView;
import com.kopykitab.ereader.components.MultipartUtility;
import com.kopykitab.ereader.models.AnnotationItem;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

class NotesOrBookmarksAdapterDividerItemDecoration extends RecyclerView.ItemDecoration {
    private Drawable divider;

    public NotesOrBookmarksAdapterDividerItemDecoration(Drawable divider) {
        this.divider = divider;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent) {
        super.onDraw(c, parent);
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + divider.getIntrinsicHeight();

            divider.setBounds(left, top, right, bottom);
            divider.draw(c);
        }
    }
}

class NotesOrBookmarksAdapter extends RecyclerView.Adapter<NotesOrBookmarksAdapter.ViewHolder> {
    private Context mContext;
    private String customerId, productId, bookName;
    private HashMap<String, String> book;
    private int notesOrBookmarksType, buttonFlipperChild = 0, printTaken;
    private ViewFlipper noteOrBookmarksFlip;
    private List<HashMap<String, String>> allAnnotationData;
    private HashMap<Integer, AnnotationItem> printAnnotations = new HashMap<Integer, AnnotationItem>();
    private RefreshPrintListener refreshPrintListener;
    private DeleteAnnotationListener deleteAnnotationListener;

    public NotesOrBookmarksAdapter(Context mContext, HashMap<String, String> book, int notesOrBookmarksType, ViewFlipper noteOrBookmarksFlip, List<HashMap<String, String>> allAnnotationData) {
        this.mContext = mContext;
        this.book = book;
        this.notesOrBookmarksType = notesOrBookmarksType;
        this.noteOrBookmarksFlip = noteOrBookmarksFlip;
        this.allAnnotationData = allAnnotationData;

        //customerId
        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");

        //product ID
        if (book != null && book.containsKey("product_id")) {
            productId = book.get("product_id");
        } else {
            productId = "-1";
        }

        //book name
        if (book != null && book.containsKey("name")) {
            bookName = book.get("name");
        } else {
            bookName = "";
        }
    }

    public NotesOrBookmarksAdapter(Context mContext, HashMap<String, String> book, int notesOrBookmarksType, ViewFlipper noteOrBookmarksFlip, List<HashMap<String, String>> allAnnotationData, RefreshPrintListener refreshPrintListener, DeleteAnnotationListener deleteAnnotationListener) {
        this.mContext = mContext;
        this.book = book;
        this.notesOrBookmarksType = notesOrBookmarksType;
        this.noteOrBookmarksFlip = noteOrBookmarksFlip;
        this.allAnnotationData = allAnnotationData;
        this.refreshPrintListener = refreshPrintListener;
        this.deleteAnnotationListener = deleteAnnotationListener;

        //customerId
        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");

        //product ID
        if (book != null && book.containsKey("product_id")) {
            productId = book.get("product_id");
        } else {
            productId = "-1";
        }

        //book name
        if (book != null && book.containsKey("name")) {
            bookName = book.get("name");
        } else {
            bookName = "";
        }

        printTaken = Utils.annotationsPrintTaken;
    }

    public void updateNotes(int position, HashMap<String, String> annotationData) {
        allAnnotationData.get(position).putAll(annotationData);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return allAnnotationData.size();
    }

    public void refreshAdapter(int printTaken) {
        buttonFlipperChild = 1;
        notifyDataSetChanged();

        refreshPrintListener.refreshPrintAnnotationInfo(printTaken, Utils.annotationsPrintLimit);
        this.printTaken = printTaken;
    }

    public void initialStateAdapter() {
        buttonFlipperChild = 0;
        printAnnotations = new HashMap<Integer, AnnotationItem>();
        notifyDataSetChanged();
    }

    public boolean addPrintAnnotation(HashMap<String, String> annotationData, String date_added, int position) {
        if (printTaken < Utils.annotationsPrintLimit) {
            String chapterName = annotationData.get("chapter_name");
            String pageNumber = annotationData.get("page_number");
            String notes = annotationData.get("notes");
            String annotationText = annotationData.get("annotation_text");

            AnnotationItem annotationItem = new AnnotationItem(chapterName, pageNumber, date_added, notes, annotationText);
            printAnnotations.put(position, annotationItem);
            printTaken++;

            refreshPrintListener.refreshPrintAnnotationInfo(printTaken, Utils.annotationsPrintLimit);

            return true;
        } else {
            Constants.showToast("You can take print only 10 pages", mContext);
        }

        return false;
    }

    public void removePrintAnnotation(int position) {
        printAnnotations.remove(position);
        printTaken--;

        refreshPrintListener.refreshPrintAnnotationInfo(printTaken, Utils.annotationsPrintLimit);
    }

    public void printAnnotations() {
        if (Utils.annotationsPrintTaken <= Utils.annotationsPrintLimit) {
            if (printAnnotations != null && printAnnotations.size() > 0) {
                try {
                    File printAnnotationFile = createHTMLFile(printAnnotations);
                    if (AppSettings.getInstance(mContext).get("email_status").equals("1")) {
                        new CreateHTMLFileAndSendEmail(printAnnotations, true, printAnnotationFile).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        String emailBody = "Hello " + AppSettings.getInstance(mContext).get("customer_name") + ","
                                + "\n\nYou are part of our elite customer base who have access to premium PDF print option."
                                + "\n\nPlease find your attachment and print your notes"
                                + "\n\nGive us your feedback at info@kopykitab.com and let us know if you like this feature."
                                + "\n\nBest Regards,"
                                + "\nTeam Kopykitab.com";

                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setType("text/*");
                        emailIntent.setData(Uri.parse("mailto:"));
                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{AppSettings.getInstance(mContext).get("customer_email")});
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Kopykitab - Your Notes have arrived & Ready to Print.");
                        emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(printAnnotationFile.toURI().toString()));
                        mContext.startActivity(emailIntent);

                        new CreateHTMLFileAndSendEmail(printAnnotations, false, printAnnotationFile).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Constants.showToast("Please add at least on page to print", mContext);
            }
        } else {
            Constants.showToast("You can take print only 10 pages", mContext);
        }

        if (Utils.isNetworkConnected(mContext)) {
            Utils.triggerGAEventOnline(mContext, "Print_Annotation_Next", productId, customerId);
        }
    }

    @Override
    public NotesOrBookmarksAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.notes_bookmarks_view_items, parent, false);
        ViewHolder vh = new ViewHolder(v);

        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final HashMap<String, String> annotationData = allAnnotationData.get(position);

        //call method for getting formatted date like : 4th Apr, 2017
        final String date_added = getFormattedDate(annotationData.get("date_added"));

        if (bookName != null && !bookName.isEmpty()) {
            holder.bookNameView.setVisibility(View.VISIBLE);
            holder.bookNameView.setText(bookName);
        } else {
            holder.bookNameView.setVisibility(View.GONE);
        }
        holder.chapterInfoView.setText(annotationData.get("chapter_name"));
        holder.dateAddedView.setText(date_added);
        holder.pageNumberView.setText("Page No:" + annotationData.get("page_number"));
        holder.notesView.setText(annotationData.get("notes"));

        final String annotationText = annotationData.get("annotation_text");
        if (annotationText != null && !annotationText.isEmpty()) {
            holder.annotationTextView.setText(annotationText);
            holder.annotationTextView.setVisibility(View.VISIBLE);
        } else {
            holder.annotationTextView.setVisibility(View.GONE);
        }

        if (printAnnotations.containsKey(position)) {
            holder.buttonsViewFlipper.setDisplayedChild(2);
        } else {
            holder.buttonsViewFlipper.setDisplayedChild(buttonFlipperChild);
        }

        holder.deleteNotesOrBookmarkButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                allAnnotationData.remove(position);
                notifyDataSetChanged();

                if (allAnnotationData == null || allAnnotationData.size() <= 0) {
                    if (notesOrBookmarksType == 0) {
                        noteOrBookmarksFlip.setDisplayedChild(1);
                    } else if (notesOrBookmarksType == 1) {
                        noteOrBookmarksFlip.setDisplayedChild(2);
                    }
                }

                if (notesOrBookmarksType == 0) {
                    deleteAnnotationListener.deleteAnnotation(Integer.valueOf(annotationData.get("page_number")) - 1, annotationData.get("coordinate"));
                    UtilsPDF.deleteNotesDataFromJSONFile(mContext, customerId, productId, (Integer.valueOf(annotationData.get("page_number")) - 1), annotationData.get("coordinate"));
                } else if (notesOrBookmarksType == 1) {
                    updateBookmarksJSONFile(Integer.valueOf(annotationData.get("page_number")));
                }
            }
        });

        holder.notesButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                final AlertDialog alertDialog = UtilsPDF.createAlertBox(mContext);
                alertDialog.show();

                ImageButton cancelNotes = ((ImageButton) alertDialog.findViewById(R.id.cancelNotesButton));
                Button saveNotesButton = ((Button) alertDialog.findViewById(R.id.saveNotesButton));
                final EditText editText = ((EditText) alertDialog.findViewById(R.id.dialogEditText));

                editText.setText(annotationData.get("notes"));
                editText.setSelection(annotationData.get("notes").length());

                saveNotesButton.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        String notesMsg = editText.getText().toString().trim();
                        CharSequence text = "";
                        if (!notesMsg.isEmpty()) {
                            HashMap<String, String> notesData = new HashMap<String, String>();
                            notesData.put("notes", notesMsg);
                            notesData.put("date_added", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()));

                            Integer pageNumber = (Integer.valueOf(annotationData.get("page_number")) - 1);
                            if (notesOrBookmarksType == 0) {
                                notesData.put("annotation_text", annotationData.get("annotation_text"));

                                //save notes to Notes JSON file
                                UtilsPDF.saveNotes(mContext, notesData, customerId, productId, pageNumber, annotationData.get("coordinate"));

                                annotationData.put("annotation_text", annotationData.get("annotation_text"));
                            } else if (notesOrBookmarksType == 1) {

                                //save notes to bookmark JSON file
                                updateNotesInBookmarksJSONFile(pageNumber, notesData);
                            }

                            //update notes
                            annotationData.put("notes", notesMsg);
                            annotationData.put("date_added", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime()));

                            //update data to recyclerView
                            updateNotes(position, annotationData);

                            //disable alert dialog
                            alertDialog.dismiss();

                            text = "Notes are Updated...";
                        } else {
                            text = "Notes can't be empty...";
                        }

                        //show toast
                        Toast toast = Toast.makeText(mContext, text, Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

                cancelNotes.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        alertDialog.dismiss();
                    }
                });
            }
        });

        holder.shareButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

                String customerName = AppSettings.getInstance(mContext).get("customer_name");
                Integer pageNumber = (Integer.valueOf(annotationData.get("page_number")) - 1);

                String shareSubject = "Your friend " + customerName + " shared study success with you";
                String shareBody = "I am reading " + bookName;
                if (notesOrBookmarksType == 0) {
                    shareBody += "\n\nI have created a note on the book";

                    String coordinate = annotationData.get("coordinate");
                    if (coordinate.startsWith("8.0")) {
                        Utils.triggerGAEvent(mContext, "Pdf_Annotation_Share_Highlight", customerId, productId + "_" + pageNumber);
                    } else if (coordinate.startsWith("9.0")) {
                        Utils.triggerGAEvent(mContext, "Pdf_Annotation_Share_Underline", customerId, productId + "_" + pageNumber);
                    } else if (coordinate.startsWith("11.0")) {
                        Utils.triggerGAEvent(mContext, "Pdf_Annotation_Share_Strikeout", customerId, productId + "_" + pageNumber);
                    }
                } else if (notesOrBookmarksType == 1) {
                    shareBody += "\n\nI have found below part of the book very important";

                    Utils.triggerGAEvent(mContext, "Pdf_Annotation_Share_Bookmark", customerId, productId + "_" + pageNumber);
                }

                String chapterName = annotationData.get("chapter_name");
                if (!chapterName.equals(""))
                    shareBody += "\nChapter Name : " + chapterName;

                shareBody += "\nPage No : " + (pageNumber + 1);

                String note = annotationData.get("notes");
                if (!note.equals(""))
                    shareBody += "\nMy Note : " + note;

                shareBody += "\n\nShared Via : " + Constants.BASE_URL;

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareSubject);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                mContext.startActivity(Intent.createChooser(sharingIntent, "Share Notes OR Bookmarks Via :"));
            }
        });

        holder.addPrintLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (addPrintAnnotation(annotationData, date_added, position)) {
                    holder.buttonsViewFlipper.setDisplayedChild(2);
                }
            }
        });

        holder.removePrintLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.buttonsViewFlipper.setDisplayedChild(1);
                removePrintAnnotation(position);
            }
        });

        holder.notesOrBookmarksView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                int currentChild = holder.buttonsViewFlipper.getDisplayedChild();
                if (currentChild == 1) {
                    if (addPrintAnnotation(annotationData, date_added, position)) {
                        holder.buttonsViewFlipper.setDisplayedChild(2);
                    }
                } else if (currentChild == 2) {
                    holder.buttonsViewFlipper.setDisplayedChild(1);
                    removePrintAnnotation(position);
                } else if (currentChild == 0) {
                    ((MuPDFActivity) mContext).onActivityResult(0, (Integer.valueOf(allAnnotationData.get(position).get("page_number")) - 1), null);
                }
            }
        });
    }

    /* load bookmarked page number from JSON file and update the JSON File */
    protected void updateBookmarksJSONFile(Integer pageNumber) {
        File jsonFile = new File(Utils.getDirectory(mContext) + Constants.BOOKMARKED_PAGE_JSON_FILENAME);
        try {
            if (jsonFile.exists()) {
                HashMap<String, HashMap<Integer, HashMap<String, String>>> fileDataBookmark = new HashMap<String, HashMap<Integer, HashMap<String, String>>>();
                fileDataBookmark = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, HashMap<String, String>>>>() {
                }.getType());
                if (fileDataBookmark != null && fileDataBookmark.size() > 0) {
                    String mainKey = customerId + "_" + productId;
                    if (fileDataBookmark.get(mainKey) != null) {
                        HashMap<String, String> notesData = fileDataBookmark.get(mainKey).remove(pageNumber - 1);

                        //update Bookmark icon
                        MuPDFActivity.updateBookmarkPageIconWhenDelete(fileDataBookmark.get(mainKey), (pageNumber - 1));
                        Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Bookmark", customerId, productId + "_" + (pageNumber - 1));
                        String notes = notesData.get("notes");
                        if (!notes.equals("")) {
                            Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Bookmark_Notes", customerId, productId + "_" + (pageNumber - 1));
                        }

                        //update JSON File
                        try {
                            FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(mContext) + Constants.BOOKMARKED_PAGE_JSON_FILENAME));
                            newJsonFile.write(new Gson().toJson(fileDataBookmark));
                            newJsonFile.flush();
                            newJsonFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateNotesInBookmarksJSONFile(Integer pageNumber, HashMap<String, String> notesData) {
        String mainKey = customerId + "_" + productId;
        File jsonFile = new File(Utils.getDirectory(mContext) + Constants.BOOKMARKED_PAGE_JSON_FILENAME);
        try {
            if (jsonFile.exists()) {
                HashMap<String, HashMap<Integer, HashMap<String, String>>> fileDataBookmark = new HashMap<String, HashMap<Integer, HashMap<String, String>>>();
                fileDataBookmark = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, HashMap<String, String>>>>() {
                }.getType());
                if (fileDataBookmark != null && fileDataBookmark.size() > 0) {
                    HashMap<Integer, HashMap<String, String>> bookmarkedData = fileDataBookmark.get(mainKey);
                    if (bookmarkedData != null) {
                        bookmarkedData.put(pageNumber, notesData);
                        fileDataBookmark.put(mainKey, bookmarkedData);

                        Utils.triggerGAEvent(mContext, "Pdf_Annotation_Bookmark_Notes", customerId, productId + "_" + pageNumber);

                        //update JSON File
                        try {
                            FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(mContext) + Constants.BOOKMARKED_PAGE_JSON_FILENAME));
                            newJsonFile.write(new Gson().toJson(fileDataBookmark));
                            newJsonFile.flush();
                            newJsonFile.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFormattedDate(String date_added) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            date = sdf.parse(date_added);

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            //5th Mar, 2015
            int day = cal.get(Calendar.DATE);

            if (!((day > 10) && (day < 19)))
                switch (day % 10) {
                    case 1:
                        return new SimpleDateFormat("d'st' MMM',' yyyy").format(date);
                    case 2:
                        return new SimpleDateFormat("d'nd' MMM',' yyyy").format(date);
                    case 3:
                        return new SimpleDateFormat("d'rd' MMM',' yyyy").format(date);
                    default:
                        return new SimpleDateFormat("d'th' MMM',' yyyy").format(date);
                }
            return new SimpleDateFormat("d'th' MMM',' yyyy").format(date);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return date_added;
    }

    private File createHTMLFile(HashMap<Integer, AnnotationItem> printAnnotations) {
        File printAnnotationFile = null;
        try {
            printAnnotationFile = new File(Utils.getDirectory(mContext), Constants.PRINT_ANNOTATION_FILE_NAME);
            FileWriter writer = new FileWriter(printAnnotationFile);
            Iterator annotationIterator = printAnnotations.keySet().iterator();
            while (annotationIterator.hasNext()) {
                Integer key = (Integer) annotationIterator.next();
                AnnotationItem annotationItem = printAnnotations.get(key);
                writer.append("<h1>Book Name: " + bookName + "</h1>");
                String chapterName = annotationItem.getChapterName();
                if (chapterName != null && !chapterName.isEmpty()) {
                    writer.append("<h2>Chapter Name: " + annotationItem.getChapterName() + "</h2>");
                }
                writer.append("<b>Page Number: " + annotationItem.getPageNumber() + ", Date: " + annotationItem.getDateAdded() + "</b>");
                writer.append("<p>Notes: " + annotationItem.getNotes() + "</p>");
                writer.append("<p>Annotation Text: " + annotationItem.getAnnotationText() + "</p>");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return printAnnotationFile;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout notesOrBookmarksView;
        TextView bookNameView, chapterInfoView, dateAddedView, pageNumberView, notesView, annotationTextView;
        ViewFlipper buttonsViewFlipper;
        Button deleteNotesOrBookmarkButton, notesButton, shareButton;
        LinearLayout addPrintLayout, removePrintLayout;

        public ViewHolder(View v) {
            super(v);

            notesOrBookmarksView = (RelativeLayout) v.findViewById(R.id.notes_bookmarks_item);
            bookNameView = (TextView) v.findViewById(R.id.book_name);
            chapterInfoView = (TextView) v.findViewById(R.id.chapter_info);
            dateAddedView = (TextView) v.findViewById(R.id.notes_date);
            pageNumberView = (TextView) v.findViewById(R.id.bookmark_page_no);
            notesView = (TextView) v.findViewById(R.id.notes_text);
            annotationTextView = (TextView) v.findViewById(R.id.annotation_text);
            buttonsViewFlipper = (ViewFlipper) v.findViewById(R.id.buttons_flipper);
            deleteNotesOrBookmarkButton = (Button) v.findViewById(R.id.delete_notes_bookmark_button);
            notesButton = (Button) v.findViewById(R.id.note_button);
            shareButton = (Button) v.findViewById(R.id.share_button);
            addPrintLayout = (LinearLayout) v.findViewById(R.id.add_print_layout);
            removePrintLayout = (LinearLayout) v.findViewById(R.id.remove_print_layout);
        }
    }

    public class CreateHTMLFileAndSendEmail extends AsyncTask<Void, Void, String> {
        private boolean shouldAttachFile;
        private File printAnnotationFile;
        private HashMap<Integer, AnnotationItem> printAnnotations;
        private CircularProgressView printProgress;
        private TextView printMessage;
        private ImageButton closeButton;
        private ImageView printDoneIcon;

        public CreateHTMLFileAndSendEmail(HashMap<Integer, AnnotationItem> printAnnotations, boolean shouldAttachFile, File printAnnotationFile) {
            this.shouldAttachFile = shouldAttachFile;
            this.printAnnotations = printAnnotations;
            this.printAnnotationFile = printAnnotationFile;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            final AlertDialog alertDialog = Utils.createAlertBox(mContext, R.layout.print_annotation_dialog);
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setCancelable(false);
            alertDialog.show();

            printProgress = (CircularProgressView) alertDialog.findViewById(R.id.print_progress);
            printMessage = (TextView) alertDialog.findViewById(R.id.print_message);
            closeButton = (ImageButton) alertDialog.findViewById(R.id.print_close);
            printDoneIcon = (ImageView) alertDialog.findViewById(R.id.print_done_icon);
            closeButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();

                    initialStateAdapter();
                    refreshPrintListener.refreshPrintButton();

                }
            });
        }

        @Override
        protected String doInBackground(Void... params) {
            String response = null;

            try {
                MultipartUtility multipart = new MultipartUtility(Constants.ANNOTATION_PRINT_URL, "UTF-8");

                multipart.addFormField("customer_id", customerId);
                multipart.addFormField("login_source", Constants.LOGIN_SOURCE);
                multipart.addFormField("annotations_print_taken_count", Integer.toString(printAnnotations.size()));

                if (shouldAttachFile) {
                    multipart.addFilePart("annotations_file", printAnnotationFile);

                    List<String> responses = multipart.finish();
                    response = responses.get(0);
                    if (printAnnotationFile.exists()) {
                        printAnnotationFile.delete();
                    }
                } else {
                    List<String> responses = multipart.finish();
                    response = responses.get(0);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);

            try {
                Log.i("Print Annotation Response", response);
                if (response != null) {
                    JSONObject responseObject = new JSONObject(response);
                    if (responseObject.getBoolean("status")) {
                        Utils.annotationsPrintTaken = printTaken;

                        closeButton.setVisibility(View.VISIBLE);
                        printProgress.setIndeterminate(false);
                        printProgress.setMaxProgress(100);
                        printDoneIcon.setVisibility(View.VISIBLE);
                        printProgress.setUnfinishedColor(mContext.getResources().getColor(R.color.action_bar_background));
                    }

                    printMessage.setText(responseObject.getString("message"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public interface RefreshPrintListener {
        void refreshPrintButton();

        void refreshPrintAnnotationInfo(int printTaken, int printLimit);
    }

    public interface DeleteAnnotationListener {
        void deleteAnnotation(Integer pageNumber, String coordinate);
    }
}

@SuppressLint("ValidFragment")
public class DetailedNotesOrBookmarksFragment extends Fragment implements RefreshPrintListener {
    private Context mContext;
    private View rootView = null;
    private ViewFlipper noteOrBookmarksFlip;
    private RecyclerView noteOrBookmarksResults;
    private String productId;
    private HashMap<String, String> book;
    private LinkedHashMap<Integer, String> mChapterInfo;
    private int position;
    private NotesOrBookmarksAdapter notesOrBookmarksAdapter;
    private TextView printPageInfo;
    private Button printButton;
    private RefreshPrintListener refreshPrintListener;
    private DeleteAnnotationListener deleteAnnotationListener;
    private boolean enablePrintAnnotationMode;
    private RelativeLayout premiumTransparentView;
    private ImageView premiumCloseView;
    private Button premiumButton;

    public DetailedNotesOrBookmarksFragment() {

    }

    public DetailedNotesOrBookmarksFragment(Context mContext, int position, LinkedHashMap<Integer, String> mChapterInfo, HashMap<String, String> book, DeleteAnnotationListener deleteAnnotationListener, boolean enablePrintAnnotationMode) {
        // TODO Auto-generated constructor stub
        this.mContext = mContext;
        this.position = position;
        this.mChapterInfo = mChapterInfo;
        this.book = book;
        this.deleteAnnotationListener = deleteAnnotationListener;
        this.enablePrintAnnotationMode = enablePrintAnnotationMode;

        if (book != null && book.containsKey("product_id")) {
            productId = book.get("product_id");
        } else {
            productId = "-1";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (mContext != null) {
            rootView = LayoutInflater.from(mContext).inflate(R.layout.notes_bookmarks_view, container, false);

            noteOrBookmarksFlip = (ViewFlipper) rootView.findViewById(R.id.notes_and_bookmarks_flip);
            noteOrBookmarksFlip.setDisplayedChild(0);

            printPageInfo = (TextView) rootView.findViewById(R.id.print_annotation_info);
            noteOrBookmarksResults = (RecyclerView) rootView.findViewById(R.id.notes_and_bookmarks_results);
            printButton = (Button) rootView.findViewById(R.id.print_notes_bookmark_button);
            premiumTransparentView = (RelativeLayout) rootView.findViewById(R.id.premium_transparent_view);
            premiumCloseView = (ImageView) rootView.findViewById(R.id.premium_view_close);
            premiumButton = (Button) rootView.findViewById(R.id.premium_button);

            noteOrBookmarksResults.setHasFixedSize(true);

            noteOrBookmarksResults.setLayoutManager(new LinearLayoutManager(mContext));
            noteOrBookmarksResults.addItemDecoration(new NotesOrBookmarksAdapterDividerItemDecoration(ContextCompat.getDrawable(mContext, R.drawable.item_divider)));
            refreshPrintListener = this;

            new GetNotesOrBookmarksByType(mContext, position).forceLoad();

            if (productId.equals("-1")) {
                printButton.setVisibility(View.GONE);
            }

            printButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    activatePrintAnnotationMode();
                }
            });

            premiumCloseView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    printPageInfo.setVisibility(View.VISIBLE);
                    printButton.setVisibility(View.VISIBLE);
                    premiumTransparentView.setVisibility(View.GONE);
                }
            });

            premiumButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (Utils.isNetworkConnected(getContext())) {
                        Utils.showPremiumActivity(getContext());
                    } else {
                        Utils.networkNotAvailableAlertBox(getContext());
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void refreshPrintButton() {
        printButton.setText("Print");
        printButton.setBackgroundDrawable(mContext.getResources().getDrawable((R.drawable.print_rounded_button)));
    }

    @Override
    public void refreshPrintAnnotationInfo(int printTaken, int printLimit) {
        int printLeft = printLimit - printTaken;
        if (printLeft == 0) {
            printPageInfo.setVisibility(View.GONE);
            printButton.setVisibility(View.GONE);
            premiumTransparentView.setVisibility(View.VISIBLE);
        }

        printPageInfo.setText("You have " + printLeft + "/" + printLimit + " notes left for Print.");
    }

    private class GetNotesOrBookmarksByType extends AsyncTaskLoader<List<HashMap<String, String>>> {

        private int notesOrBookmarksType;
        private HashMap<String, HashMap<Integer, HashMap<String, String>>> fileDataBookmark;
        private HashMap<Integer, HashMap<String, String>> bookmarkedData;
        private List<HashMap<String, String>> allAnnotationData = new ArrayList<HashMap<String, String>>();

        public GetNotesOrBookmarksByType(Context context, int notesOrBookmarksType) {
            super(context);
            this.notesOrBookmarksType = notesOrBookmarksType;
        }

        @Override
        public List<HashMap<String, String>> loadInBackground() {
            // TODO Auto-generated method stub
            if (notesOrBookmarksType == 0) {
                HashMap<String, HashMap<String, HashMap<String, String>>> fileDataNotes = new HashMap<String, HashMap<String, HashMap<String, String>>>();
                HashMap<String, HashMap<String, String>> allNotesAndCoordinates = new HashMap<String, HashMap<String, String>>();
                try {
                    File jsonFile = new File(Utils.getDirectory(mContext) + Constants.ANNOT_ADD_NOTES_JSON_FILENAME);
                    if (jsonFile.exists()) {
                        fileDataNotes = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<String, HashMap<String, String>>>>() {
                        }.getType());
                        if (fileDataNotes != null && fileDataNotes.size() > 0) {
                            String mainKey = AppSettings.getInstance(mContext).get("CUSTOMER_ID") + "_" + productId;

                            List<Integer> sortedPageNumbers = new ArrayList<Integer>();
                            for (String key : fileDataNotes.keySet()) {
                                if (key.startsWith(mainKey)) {
                                    String[] pageNumberArray = key.split("___");
                                    sortedPageNumbers.add(Integer.valueOf(pageNumberArray[1]));
                                }
                            }

                            //sort page number
                            Collections.sort(sortedPageNumbers);

                            for (Integer pageNumber : sortedPageNumbers) {
                                allNotesAndCoordinates = fileDataNotes.get(mainKey + "___" + Integer.toString(pageNumber));
                                if (allNotesAndCoordinates != null && allNotesAndCoordinates.size() > 0) {
                                    for (String coordinate : allNotesAndCoordinates.keySet()) {
                                        HashMap<String, String> notesData = allNotesAndCoordinates.get(coordinate);
                                        HashMap<String, String> allDataWithKeys = new HashMap<String, String>();
                                        allDataWithKeys.put("chapter_name", getChapterNameByPageNumber(pageNumber));
                                        allDataWithKeys.put("page_number", Integer.toString((pageNumber.intValue() + 1)));
                                        allDataWithKeys.put("date_added", notesData.get("date_added"));
                                        allDataWithKeys.put("notes", notesData.get("notes"));
                                        allDataWithKeys.put("annotation_text", notesData.get("annotation_text"));
                                        allDataWithKeys.put("coordinate", coordinate.toString());

                                        allAnnotationData.add(allDataWithKeys);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (notesOrBookmarksType == 1) {
                //load bookmarked page number from JSON file
                File jsonFile = new File(Utils.getDirectory(mContext) + Constants.BOOKMARKED_PAGE_JSON_FILENAME);
                try {
                    if (jsonFile.exists()) {
                        fileDataBookmark = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, HashMap<String, String>>>>() {
                        }.getType());
                        if (fileDataBookmark != null && fileDataBookmark.size() > 0) {
                            String mainKey = AppSettings.getInstance(mContext).get("CUSTOMER_ID") + "_" + productId;
                            if (fileDataBookmark.get(mainKey) != null && fileDataBookmark.get(mainKey).size() > 0) {
                                if (bookmarkedData == null) {
                                    bookmarkedData = new HashMap<Integer, HashMap<String, String>>();
                                }
                                bookmarkedData.putAll(fileDataBookmark.get(mainKey));

                                //sort a key for showing a list based on ascending order of page number
                                List<Integer> sortedKeys = new ArrayList<Integer>(bookmarkedData.keySet());
                                Collections.sort(sortedKeys);

                                for (Integer key : sortedKeys) {
                                    HashMap<String, String> allDataWithKeys = new HashMap<String, String>();

                                    allDataWithKeys.put("chapter_name", getChapterNameByPageNumber(key));
                                    allDataWithKeys.put("page_number", Integer.toString((key.intValue() + 1)));
                                    allDataWithKeys.put("date_added", bookmarkedData.get(key).get("date_added"));
                                    allDataWithKeys.put("notes", bookmarkedData.get(key).get("notes"));

                                    allAnnotationData.add(allDataWithKeys);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return allAnnotationData;
        }

        @Override
        public void deliverResult(final List<HashMap<String, String>> allAnnotationData) {
            // TODO Auto-generated method stub
            super.deliverResult(allAnnotationData);

            if (allAnnotationData.size() > 0) {
                if (notesOrBookmarksType == 0) {
                    printButton.setVisibility(View.VISIBLE);
                    notesOrBookmarksAdapter = new NotesOrBookmarksAdapter(mContext, book, notesOrBookmarksType, noteOrBookmarksFlip, allAnnotationData, refreshPrintListener, deleteAnnotationListener);

                    if (enablePrintAnnotationMode) {
                        activatePrintAnnotationMode();
                    }
                } else {
                    notesOrBookmarksAdapter = new NotesOrBookmarksAdapter(mContext, book, notesOrBookmarksType, noteOrBookmarksFlip, allAnnotationData);
                }
                noteOrBookmarksResults.setLayoutManager(new LinearLayoutManager(mContext));
                noteOrBookmarksResults.setAdapter(notesOrBookmarksAdapter);
            } else {
                if (notesOrBookmarksType == 0) {
                    noteOrBookmarksFlip.setDisplayedChild(1);
                } else if (notesOrBookmarksType == 1) {
                    noteOrBookmarksFlip.setDisplayedChild(2);
                }
            }
            CircularProgressView notesAndBookmarksProgress = (CircularProgressView) rootView.findViewById(R.id.notes_and_bookmarks_progress);
            notesAndBookmarksProgress.setVisibility(View.GONE);
        }

        private String getChapterNameByPageNumber(Integer key) {
            String chapterName = "";
            if (mChapterInfo.size() > 0) {
                if (mChapterInfo.containsKey(key)) {
                    chapterName = mChapterInfo.get(key);
                } else {
                    for (Integer pageKey : mChapterInfo.keySet()) {
                        Integer nextPageKey = nextPageKey(pageKey);
                        if (nextPageKey != null) {
                            if ((pageKey.intValue() > key.intValue()) && (nextPageKey.intValue() > key.intValue())) {
                                chapterName = mChapterInfo.get(pageKey);
                                break;
                            }

                            if (nextPageKey.intValue() > key.intValue()) {
                                chapterName = mChapterInfo.get(pageKey);
                                break;
                            }
                        } else {
                            chapterName = mChapterInfo.get(pageKey);
                            break;
                        }
                    }
                }
            }

            return chapterName;
        }

        private Integer nextPageKey(Integer pageKey) {
            for (Integer nextPageKey : mChapterInfo.keySet()) {
                if (nextPageKey.intValue() > pageKey.intValue())
                    return nextPageKey;
            }

            return null;
        }
    }

    private void activatePrintAnnotationMode() {
        if (Utils.isNetworkConnected(mContext)) {
            String printButtonText = printButton.getText().toString();
            if (printButtonText.equalsIgnoreCase("print")) {
                int printTaken = Utils.annotationsPrintTaken;
                int printLimit = Utils.annotationsPrintLimit;

                if (printLimit == -1) {
                    new CustomerDetails().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    refreshPrintAnnotationInfo(printTaken, printLimit);
                    printPageInfo.setVisibility(View.VISIBLE);
                }
                printButton.setText("Next");
                printButton.setBackgroundDrawable(mContext.getResources().getDrawable((R.drawable.print_button_background)));
                notesOrBookmarksAdapter.refreshAdapter(Utils.annotationsPrintTaken);


                Utils.triggerGAEventOnline(mContext, "Print_Annotation", productId, AppSettings.getInstance(mContext).get("CUSTOMER_ID"));
            } else {
                notesOrBookmarksAdapter.printAnnotations();
            }
        } else {
            Utils.networkNotAvailableAlertBox(mContext);
        }
    }

    protected class CustomerDetails extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            try {
                response = Utils.sendPost(Constants.LOGIN_API_URL + "getCustomerDetails", "customer_id=" + URLEncoder.encode(AppSettings.getInstance(mContext).get("CUSTOMER_ID"), "UTF-8") + "&login_source=" + URLEncoder.encode(Constants.LOGIN_SOURCE, "UTF-8"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                Log.i("Customer Details Response", result);
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.getBoolean("status")) {
                        JSONObject customerDetailsJsonObject = jsonObject.getJSONObject("customer_details");
                        AppSettings.getInstance(mContext).set("email_status", customerDetailsJsonObject.getString("email_status"));
                        if (customerDetailsJsonObject.has("attributes")) {
                            JSONObject attributesObject = customerDetailsJsonObject.getJSONObject("attributes");
                            int printTaken = attributesObject.getInt("annotations_print_taken_count");
                            int printLimit = attributesObject.getInt("annotations_print_limit");
                            Utils.annotationsPrintTaken = printTaken;
                            Utils.annotationsPrintLimit = printLimit;

                            refreshPrintAnnotationInfo(printTaken, printLimit);
                            printPageInfo.setVisibility(View.VISIBLE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
