package com.artifex.mupdfdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.SparseArray;

import com.artifex.mupdfdemo.MuPDFActivity.UtilsPDF;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kopykitab.ereader.R;
import com.kopykitab.ereader.settings.AppSettings;
import com.kopykitab.ereader.settings.Constants;
import com.kopykitab.ereader.settings.Utils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class MuPDFCore {
    /* load our native library */
    static {
        System.loadLibrary("mupdf");
    }

    /* Readable members */
    private int numPages = -1;
    private float pageWidth;
    private float pageHeight;
    private long globals;
    private byte fileBuffer[];
    private String file_format;
    private boolean isUnencryptedPDF;
    private final boolean wasOpenedFromBuffer;
    private Context mContext;
    private String productId;
    private static HashMap<String, Set<List<Float>>> annotationsData;
    private static HashMap<String, HashMap<Integer, Integer>> addedAnnotations, deletedAnnotations;
    private Set<Integer> visitedPageNumber;
    private static String customerId;
    private List<Float> lastAddedAnnotation = null;

    /* The native functions */
    private native long openFile(String filename);

    private native long openBuffer(String magic);

    private native String fileFormatInternal();

    private native boolean isUnencryptedPDFInternal();

    private native int countPagesInternal();

    private native void gotoPageInternal(int localActionPageNum);

    private native float getPageWidth();

    private native float getPageHeight();

    private native void drawPage(Bitmap bitmap,
                                 int pageW, int pageH,
                                 int patchX, int patchY,
                                 int patchW, int patchH,
                                 long cookiePtr);

    private native void updatePageInternal(Bitmap bitmap,
                                           int page,
                                           int pageW, int pageH,
                                           int patchX, int patchY,
                                           int patchW, int patchH,
                                           long cookiePtr);

    private native RectF[] searchPage(String text);

    private native TextChar[][][][] text();

    private native byte[] textAsHtml();

    private native void addMarkupAnnotationInternal(PointF[] quadPoints, int type);

    private native void addInkAnnotationInternal(PointF[][] arcs);

    private native void deleteAnnotationInternal(int annot_index);

    private native int passClickEventInternal(int page, float x, float y);

    private native void setFocusedWidgetChoiceSelectedInternal(String[] selected);

    private native String[] getFocusedWidgetChoiceSelected();

    private native String[] getFocusedWidgetChoiceOptions();

    private native int getFocusedWidgetSignatureState();

    private native String checkFocusedSignatureInternal();

    private native boolean signFocusedSignatureInternal(String keyFile, String password);

    private native int setFocusedWidgetTextInternal(String text);

    private native String getFocusedWidgetTextInternal();

    private native int getFocusedWidgetTypeInternal();

    private native LinkInfo[] getPageLinksInternal(int page);

    private native RectF[] getWidgetAreasInternal(int page);

    private native Annotation[] getAnnotationsInternal(int page);

    private native OutlineItem[] getOutlineInternal();

    private native boolean hasOutlineInternal();

    private native boolean needsPasswordInternal();

    private native boolean authenticatePasswordInternal(String password);

    private native MuPDFAlertInternal waitForAlertInternal();

    private native void replyToAlertInternal(MuPDFAlertInternal alert);

    private native void startAlertsInternal();

    private native void stopAlertsInternal();

    private native void destroying();

    private native boolean hasChangesInternal();

    private native void saveInternal();

    private native long createCookie();

    private native void destroyCookie(long cookie);

    private native void abortCookie(long cookie);

    private native String getKeySalt();

    private native void drawPageNew(Bitmap bitmap,
                                    int pageW, int pageH,
                                    int patchX, int patchY,
                                    int patchW, int patchH, int filterMode,
                                    int background, int opacity);

    private native void updatePageInternalNew(Bitmap bitmap,
                                              int page,
                                              int pageW, int pageH,
                                              int patchX, int patchY,
                                              int patchW, int patchH,
                                              int mode, int bgValue, int opacity);

    public native boolean javascriptSupported();

    public class Cookie {
        private final long cookiePtr;

        public Cookie() {
            cookiePtr = createCookie();
            if (cookiePtr == 0)
                throw new OutOfMemoryError();
        }

        public void abort() {
            abortCookie(cookiePtr);
        }

        public void destroy() {
            // We could do this in finalize, but there's no guarantee that
            // a finalize will occur before the muPDF context occurs.
            destroyCookie(cookiePtr);
        }
    }

    private boolean isNightMode = false;

    public MuPDFCore(Context context, String filename) throws Exception {
        mContext = context;

        globals = openFile(filename);
        if (globals == 0) {
            throw new Exception(String.format(context.getString(R.string.cannot_open_file_Path), filename));
        }
        file_format = fileFormatInternal();
        isUnencryptedPDF = isUnencryptedPDFInternal();
        wasOpenedFromBuffer = false;

        annotationsData = new HashMap<String, Set<List<Float>>>();
        addedAnnotations = new HashMap<String, HashMap<Integer, Integer>>();
        deletedAnnotations = new HashMap<String, HashMap<Integer, Integer>>();
        visitedPageNumber = new HashSet<Integer>();
        lastAddedAnnotation = new ArrayList<Float>();

        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
        loadExternalAnnotations();
    }

    public MuPDFCore(Context context, byte buffer[], String magic) throws Exception {
        mContext = context;

        fileBuffer = buffer;
        globals = openBuffer(magic != null ? magic : "");
        if (globals == 0) {
            throw new Exception(context.getString(R.string.cannot_open_buffer));
        }
        file_format = fileFormatInternal();
        isUnencryptedPDF = isUnencryptedPDFInternal();
        wasOpenedFromBuffer = true;

        annotationsData = new HashMap<String, Set<List<Float>>>();
        addedAnnotations = new HashMap<String, HashMap<Integer, Integer>>();
        deletedAnnotations = new HashMap<String, HashMap<Integer, Integer>>();
        visitedPageNumber = new HashSet<Integer>();
        lastAddedAnnotation = new ArrayList<Float>();

        customerId = AppSettings.getInstance(mContext).get("CUSTOMER_ID");
        loadExternalAnnotations();
    }

    public List<Float> getAllCoordinatesForNotesAnnotation(String mainKey, int annotation_index) {
        Set<List<Float>> records = annotationsData.get(mainKey);
        List<List<Float>> tempRecords = new LinkedList<List<Float>>(records);

        return tempRecords.get(annotation_index);
    }

    private void loadExternalAnnotations() {
        HashMap<String, Set<List<Float>>> oldAnnotationsData = getAnnotationsFromOldJSONFile();
        HashMap<String, Set<List<Float>>> newAnnotationsData = getAnnotationsFromNewJSONFile();

        if (newAnnotationsData == null && oldAnnotationsData != null) {
            annotationsData = oldAnnotationsData;
        } else if (oldAnnotationsData == null && newAnnotationsData != null) {
            annotationsData = newAnnotationsData;
        } else if (newAnnotationsData != null && oldAnnotationsData != null) {
            annotationsData.putAll(oldAnnotationsData);
            Set<String> newAnnotationsDataKeys = newAnnotationsData.keySet();
            for (String newAnnotationsDataKey : newAnnotationsDataKeys) {
                Set<List<Float>> oldAnnotationsDataCoordinates = annotationsData.get(newAnnotationsDataKey);
                if (oldAnnotationsDataCoordinates != null && oldAnnotationsDataCoordinates.size() > 0) {
                    oldAnnotationsDataCoordinates.addAll(newAnnotationsData.get(newAnnotationsDataKey));
                } else {
                    oldAnnotationsDataCoordinates = newAnnotationsData.get(newAnnotationsDataKey);
                }

                annotationsData.put(newAnnotationsDataKey, oldAnnotationsDataCoordinates);
            }
        }
    }

    /* get annotation from NEW JSON file */
    private HashMap<String, Set<List<Float>>> getAnnotationsFromNewJSONFile() {
        HashMap<String, Set<List<Float>>> annotationsDataFromNewJSONFile = new HashMap<String, Set<List<Float>>>();
        try {
            File jsonFile = new File(Utils.getDirectory(mContext) + Constants.ANNOT_JSON_FILENAME);
            if (jsonFile.exists()) {
                annotationsDataFromNewJSONFile = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, Set<List<Float>>>>() {
                }.getType());

                return annotationsDataFromNewJSONFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /* get annotation from OLD JSON file and change in the format of NEW JSON file*/
    private HashMap<String, Set<List<Float>>> getAnnotationsFromOldJSONFile() {
        HashMap<String, Set<List<Float>>> annotationsDataFromOldJSONFile = new HashMap<String, Set<List<Float>>>();
        try {
            File jsonFile = new File(Utils.getDirectory(mContext) + Constants.OLD_ANNOT_JSON_FILENAME);
            if (jsonFile.exists()) {
                HashMap<String, HashMap<Integer, Set<ArrayList<Float>>>> fileData = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, Set<ArrayList<Float>>>>>() {
                }.getType());
                if (fileData.size() > 0) {
                    Set<String> allAnnotationsMainKeys = fileData.keySet();
                    for (String annotationsMainKey : allAnnotationsMainKeys) {
                        HashMap<Integer, Set<ArrayList<Float>>> allAnnotations = fileData.get(annotationsMainKey);
                        Set<List<Float>> coordinates = new LinkedHashSet<List<Float>>();
                        Set<Integer> allAnnotationsKeys = allAnnotations.keySet();
                        for (Integer annotationsKey : allAnnotationsKeys) {
                            Iterator<ArrayList<Float>> annotationPoints = allAnnotations.get(annotationsKey).iterator();
                            while (annotationPoints.hasNext()) {
                                List<Float> allCoordinates = new ArrayList<Float>();
                                //add annotation key as first coordinate
                                allCoordinates.add(annotationsKey.floatValue());
                                List<Float> annotation = annotationPoints.next();

                                for (int i = 0; i < annotation.size() - 4; i++) {
                                    allCoordinates.add(annotation.get(i).floatValue());
                                }
                                //add coordinates from old JSON file as external annotation
                                coordinates.add(allCoordinates);
                            }
                        }
                        //add all old JSON file coordinates with their respective key wise
                        annotationsDataFromOldJSONFile.put(annotationsMainKey, coordinates);
                    }
                }

                return annotationsDataFromOldJSONFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Set<List<Float>> getAnnotationsByPageNumber(int mPageNumber) {

        boolean pageVisitedOrNot = visitedPageNumber.add(mPageNumber);
        if (pageVisitedOrNot) {
            String mainKey = AppSettings.getInstance(mContext).get("CUSTOMER_ID") + "_" + productId + "___" + mPageNumber;
            Set<List<Float>> allAnnotations = annotationsData.get(mainKey);

            return allAnnotations;
        } else {

            return null;
        }
    }

    public int countPages() {
        if (numPages < 0)
            numPages = countPagesSynchronized();

        return numPages;
    }

    public String fileFormat() {
        return file_format;
    }

    public boolean isUnencryptedPDF() {
        return isUnencryptedPDF;
    }

    public boolean wasOpenedFromBuffer() {
        return wasOpenedFromBuffer;
    }

    private synchronized int countPagesSynchronized() {
        return countPagesInternal();
    }

    /* Shim function */
    private void gotoPage(int page) {
        if (page > numPages - 1)
            page = numPages - 1;
        else if (page < 0)
            page = 0;
        gotoPageInternal(page);
        this.pageWidth = getPageWidth();
        this.pageHeight = getPageHeight();
    }

    public synchronized PointF getPageSize(int page) {
        gotoPage(page);
        return new PointF(pageWidth, pageHeight);
    }

    public MuPDFAlert waitForAlert() {
        MuPDFAlertInternal alert = waitForAlertInternal();
        return alert != null ? alert.toAlert() : null;
    }

    public void replyToAlert(MuPDFAlert alert) {
        replyToAlertInternal(new MuPDFAlertInternal(alert));
    }

    public void stopAlerts() {
        stopAlertsInternal();
    }

    public void startAlerts() {
        startAlertsInternal();
    }

    public synchronized void onDestroy() {
        destroying();
        globals = 0;
    }

    public synchronized void drawPage(Bitmap bm, int page,
                                      int pageW, int pageH,
                                      int patchX, int patchY,
                                      int patchW, int patchH,
                                      MuPDFCore.Cookie cookie) {
        gotoPage(page);

        if (isNightMode) {
            drawPageNew(bm, pageW, pageH, patchX, patchY, patchW, patchH, 1, 0xff, 100);
        } else {
            drawPage(bm, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr);
        }
    }

    public void setNightMode(boolean isNightMode) {
        this.isNightMode = isNightMode;
    }

    public boolean getNightMode() {
        return isNightMode;
    }

    public synchronized void updatePage(Bitmap bm, int page,
                                        int pageW, int pageH,
                                        int patchX, int patchY,
                                        int patchW, int patchH,
                                        MuPDFCore.Cookie cookie) {

        if (isNightMode) {
            updatePageInternalNew(bm, page, pageW, pageH, patchX, patchY, patchW, patchH, 1, 0xff, 100);
        } else {
            updatePageInternal(bm, page, pageW, pageH, patchX, patchY, patchW, patchH, cookie.cookiePtr);
        }
    }

    public synchronized PassClickResult passClickEvent(int page, float x, float y) {
        boolean changed = passClickEventInternal(page, x, y) != 0;

        switch (WidgetType.values()[getFocusedWidgetTypeInternal()]) {
            case TEXT:
                return new PassClickResultText(changed, getFocusedWidgetTextInternal());
            case LISTBOX:
            case COMBOBOX:
                return new PassClickResultChoice(changed, getFocusedWidgetChoiceOptions(), getFocusedWidgetChoiceSelected());
            case SIGNATURE:
                return new PassClickResultSignature(changed, getFocusedWidgetSignatureState());
            default:
                return new PassClickResult(changed);
        }

    }

    public synchronized boolean setFocusedWidgetText(int page, String text) {
        boolean success;
        gotoPage(page);
        success = setFocusedWidgetTextInternal(text) != 0 ? true : false;

        return success;
    }

    public synchronized void setFocusedWidgetChoiceSelected(String[] selected) {
        setFocusedWidgetChoiceSelectedInternal(selected);
    }

    public synchronized String checkFocusedSignature() {
        return checkFocusedSignatureInternal();
    }

    public synchronized boolean signFocusedSignature(String keyFile, String password) {
        return signFocusedSignatureInternal(keyFile, password);
    }

    public synchronized LinkInfo[] getPageLinks(int page) {
        return getPageLinksInternal(page);
    }

    public synchronized RectF[] getWidgetAreas(int page) {
        return getWidgetAreasInternal(page);
    }

    public synchronized Annotation[] getAnnoations(int page) {
        Annotation[] annotations = getAnnotationsInternal(page);

        return annotations;
    }

    public synchronized RectF[] searchPage(int page, String text) {
        gotoPage(page);
        return searchPage(text);
    }

    public synchronized byte[] html(int page) {
        gotoPage(page);
        return textAsHtml();
    }

    public synchronized TextWord[][] textLines(int page) {
        gotoPage(page);
        TextChar[][][][] chars = text();

        // The text of the page held in a hierarchy (blocks, lines, spans).
        // Currently we don't need to distinguish the blocks level or
        // the spans, and we need to collect the text into words.
        ArrayList<TextWord[]> lns = new ArrayList<TextWord[]>();

        for (TextChar[][][] bl : chars) {
            if (bl == null)
                continue;
            for (TextChar[][] ln : bl) {
                ArrayList<TextWord> wds = new ArrayList<TextWord>();
                TextWord wd = new TextWord();

                for (TextChar[] sp : ln) {
                    for (TextChar tc : sp) {
                        if (tc.c != ' ') {
                            wd.Add(tc);
                        } else if (wd.w.length() > 0) {
                            wds.add(wd);
                            wd = new TextWord();
                        }
                    }
                }

                if (wd.w.length() > 0)
                    wds.add(wd);

                if (wds.size() > 0)
                    lns.add(wds.toArray(new TextWord[wds.size()]));
            }
        }

        return lns.toArray(new TextWord[lns.size()][]);
    }

    public synchronized void addMarkupAnnotation(int page, PointF[] quadPoints, Annotation.Type type) {
        boolean isNew = false;
        String mainKey = customerId + "_" + productId + "___" + page;
        Set<List<Float>> records = annotationsData.get(mainKey);
        if (records == null || records.size() <= 0) {
            records = new LinkedHashSet<List<Float>>();
        }
        List<Float> coordinates = new ArrayList<Float>();
        coordinates.add(Integer.valueOf(type.ordinal()).floatValue());
        try {
            for (PointF coordinate : quadPoints) {
                coordinates.add(Float.valueOf(coordinate.x));
                coordinates.add(Float.valueOf(coordinate.y));
            }
            isNew = records.add(coordinates);
            annotationsData.put(mainKey, records);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isNew) {
            triggerAddedAnnotation(mainKey, type);
        }

        gotoPage(page);
        addMarkupAnnotationInternal(quadPoints, type.ordinal());
    }

    private void triggerAddedAnnotation(String mainKey, Annotation.Type type) {
        HashMap<Integer, Integer> noOfAnnotationWithType = addedAnnotations.get(mainKey);
        HashMap<Integer, Integer> tempNoOfAnnotationWithType = new HashMap<Integer, Integer>();
        if (noOfAnnotationWithType != null) {
            tempNoOfAnnotationWithType.putAll(noOfAnnotationWithType);
            Integer count = noOfAnnotationWithType.get(Integer.valueOf(type.ordinal()));
            if (count != null) {
                tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), ++count);
            } else {
                tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), 1);
            }
        } else {
            tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), 1);
        }
        addedAnnotations.put(mainKey, tempNoOfAnnotationWithType);
    }

    public List<Float> setLastAddedAnnotation(ArrayList<PointF> quadPoints, Annotation.Type type) {
        List<Float> coordinates = new ArrayList<Float>();
        coordinates.add(Integer.valueOf(type.ordinal()).floatValue());
        try {
            for (PointF coordinate : quadPoints) {
                coordinates.add(Float.valueOf(coordinate.x));
                coordinates.add(Float.valueOf(coordinate.y));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        lastAddedAnnotation = coordinates;

        return lastAddedAnnotation;
    }

    public List<Float> getLastAddedAnnotation() {
        return lastAddedAnnotation;
    }

    public synchronized void addInkAnnotation(int page, PointF[][] arcs) {
        gotoPage(page);
        addInkAnnotationInternal(arcs);
    }

    public synchronized void deleteAnnotation(int page, int annot_index, Annotation.Type type) {
        String mainKey = customerId + "_" + productId + "___" + page;
        Set<List<Float>> records = annotationsData.get(mainKey);

        try {
            if (records != null && records.size() > 0) {
                List<List<Float>> tempRecords = new ArrayList<List<Float>>(records);
                tempRecords.remove(annot_index);
                if (tempRecords.size() > 0) {
                    annotationsData.put(mainKey, new LinkedHashSet<List<Float>>(tempRecords));
                } else {
                    annotationsData.remove(mainKey);
                }
            }

            triggerDeletedAnnotation(mainKey, type);

        } catch (Exception e) {
            e.printStackTrace();
        }

        gotoPage(page);
        deleteAnnotationInternal(annot_index);
    }

    public void deleteAnnotation(Integer page, String coordinates) {
        String mainKey = customerId + "_" + productId + "___" + page;
        Set<List<Float>> records = annotationsData.get(mainKey);
        List<Float> result = new ArrayList<>();
        for (String coord : coordinates.split("\\s*,\\s*")) {
            result.add(Float.parseFloat(coord));
        }

        try {
            List<List<Float>> tempRecords = new ArrayList<List<Float>>(records);
            int annot_index = tempRecords.indexOf(result);
            tempRecords.remove(annot_index);
            if (tempRecords.size() > 0) {
                annotationsData.put(mainKey, new LinkedHashSet<List<Float>>(tempRecords));
            } else {
                annotationsData.remove(mainKey);
            }

            Integer annotationKey = Float.valueOf(result.get(0)).intValue();
            if (annotationKey.equals(Integer.valueOf(Annotation.Type.HIGHLIGHT.ordinal()))) {
                triggerDeletedAnnotation(mainKey, Annotation.Type.HIGHLIGHT);
            } else if (annotationKey.equals(Integer.valueOf(Annotation.Type.UNDERLINE.ordinal()))) {
                triggerDeletedAnnotation(mainKey, Annotation.Type.UNDERLINE);
            } else if (annotationKey.equals(Integer.valueOf(Annotation.Type.STRIKEOUT.ordinal()))) {
                triggerDeletedAnnotation(mainKey, Annotation.Type.STRIKEOUT);
            }

            gotoPage(page);
            deleteAnnotationInternal(annot_index);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteNotesAnnotation(int page, int annot_index) {
        String mainKey = customerId + "_" + productId + "___" + page;
        String coordinatesWithBrackets = getAllCoordinatesForNotesAnnotation(mainKey, annot_index).toString();
        String coordinates = coordinatesWithBrackets.substring(1, coordinatesWithBrackets.length() - 1);

        UtilsPDF.deleteNotesDataFromJSONFile(mContext, customerId, productId, page, coordinates);
    }

    private void triggerDeletedAnnotation(String mainKey, Annotation.Type type) {
        HashMap<Integer, Integer> noOfAnnotationWithType = deletedAnnotations.get(mainKey);
        HashMap<Integer, Integer> tempNoOfAnnotationWithType = new HashMap<Integer, Integer>();
        if (noOfAnnotationWithType != null) {
            tempNoOfAnnotationWithType.putAll(noOfAnnotationWithType);
            Integer count = noOfAnnotationWithType.get(Integer.valueOf(type.ordinal()));
            if (count != null) {
                tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), ++count);
            } else {
                tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), 1);
            }
        } else {
            tempNoOfAnnotationWithType.put(Integer.valueOf(type.ordinal()), 1);
        }
        deletedAnnotations.put(mainKey, tempNoOfAnnotationWithType);
    }

    public synchronized boolean hasOutline() {
        return hasOutlineInternal();
    }

    public synchronized OutlineItem[] getOutline() {
        return getOutlineInternal();
    }

    public synchronized boolean needsPassword() {
        return needsPasswordInternal();
    }

    public synchronized boolean authenticatePassword(String password) {
        return authenticatePasswordInternal(password);
    }

    public synchronized boolean hasChanges() {
        return hasChangesInternal();
    }

    public synchronized void save() {
        //saveInternal();

        if (hasChanges()) {
            saveAnnotationsDataToJson();
        }
    }

    private void saveAnnotationsDataToJson() {
        //save annotation to JSON file
        try {
            //write data to new JSON file
            FileWriter newJsonFile = new FileWriter(new File(Utils.getDirectory(mContext) + Constants.ANNOT_JSON_FILENAME));
            newJsonFile.write(new Gson().toJson(annotationsData));
            newJsonFile.flush();
            newJsonFile.close();

            //write data to old JSON file
            HashMap<String, HashMap<Integer, Set<List<Float>>>> fileData = new HashMap<String, HashMap<Integer, Set<List<Float>>>>();
            File jsonFile = new File(Utils.getDirectory(mContext) + Constants.OLD_ANNOT_JSON_FILENAME);
            if (jsonFile.exists()) {
                fileData = new Gson().fromJson(new FileReader(jsonFile), new TypeToken<HashMap<String, HashMap<Integer, HashSet<ArrayList<Float>>>>>() {
                }.getType());
            }
            for (Integer pageNumber : visitedPageNumber) {
                String annotationMainKey = customerId + "_" + productId + "___" + pageNumber;
                HashMap<Integer, Set<List<Float>>> tempAnnotationsData = new HashMap<Integer, Set<List<Float>>>();
                Set<List<Float>> allCoordinatesMain = annotationsData.get(annotationMainKey);
                if (allCoordinatesMain != null && allCoordinatesMain.size() > 0) {
                    for (List<Float> coordinates : annotationsData.get(annotationMainKey)) {
                        Integer annotationType = Integer.valueOf(coordinates.remove(0).intValue());
                        Set<List<Float>> allCoordinates = tempAnnotationsData.get(annotationType);
                        if (allCoordinates == null || allCoordinates.size() <= 0) {
                            allCoordinates = new LinkedHashSet<List<Float>>();
                        }
                        //add left, right, top, and bottom coordinates as in OLD JSON File
                        Float left = coordinates.get(0), right = coordinates.get(0), top = coordinates.get(1), bottom = coordinates.get(1);
                        for (int j = 2; j < coordinates.size(); j++) {
                            Float coordinate = coordinates.get(j);
                            if ((j % 2) == 0) {
                                if (left > coordinate) {
                                    left = coordinate;
                                }
                                if (right < coordinate) {
                                    right = coordinate;
                                }
                            } else {
                                if (top > coordinate) {
                                    top = coordinate;
                                }
                                if (bottom < coordinate) {
                                    bottom = coordinate;
                                }
                            }
                        }

                        coordinates.add(left);
                        coordinates.add(top);
                        coordinates.add(right);
                        coordinates.add(bottom);
                        allCoordinates.add(coordinates);
                        tempAnnotationsData.put(annotationType, allCoordinates);
                    }

                    fileData.put(annotationMainKey, tempAnnotationsData);
                } else {
                    fileData.remove(annotationMainKey);
                }

                //trigger annotations data
                if (addedAnnotations != null && addedAnnotations.size() > 0) {
                    HashMap<Integer, Integer> annotationsCount = addedAnnotations.get(annotationMainKey);
                    if (annotationsCount != null && annotationsCount.size() > 0) {
                        for (Integer annotationTypeKey : annotationsCount.keySet()) {
                            String label = productId + "_" + pageNumber + "_" + annotationsCount.get(annotationTypeKey);
                            if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.HIGHLIGHT.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Highlight", customerId, label);
                            } else if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.UNDERLINE.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Underline", customerId, label);
                            } else if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.STRIKEOUT.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Strikeout", customerId, label);
                            }
                        }
                    }
                }

                if (deletedAnnotations != null && deletedAnnotations.size() > 0) {
                    HashMap<Integer, Integer> annotationsCount = deletedAnnotations.get(annotationMainKey);
                    if (annotationsCount != null && annotationsCount.size() > 0) {
                        for (Integer annotationTypeKey : annotationsCount.keySet()) {
                            String label = productId + "_" + pageNumber + "_" + annotationsCount.get(annotationTypeKey);
                            if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.HIGHLIGHT.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Highlight", customerId, label);
                            } else if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.UNDERLINE.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Underline", customerId, label);
                            } else if (annotationTypeKey.equals(Integer.valueOf(Annotation.Type.STRIKEOUT.ordinal()))) {
                                Utils.triggerGAEvent(mContext, "Pdf_Annotation_Delete_Strikeout", customerId, label);
                            }
                        }
                    }
                }
            }

            FileWriter oldJsonFile = new FileWriter(new File(Utils.getDirectory(mContext) + Constants.OLD_ANNOT_JSON_FILENAME));
            oldJsonFile.write(new Gson().toJson(fileData));
            oldJsonFile.flush();
            oldJsonFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getKey(String productId) {
        this.productId = productId;
        String original = getKeySalt() + productId;

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(original.getBytes());
            byte[] digest = md.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return original;
        }
    }

    public SparseArray<RectF[]> searchTextAll(String text, int startFrom, SparseArray<RectF[]> searchResults) {
        int fetchPages = startFrom + Constants.FETCH_SEARCH_TEXT;
        if (fetchPages > numPages) {
            fetchPages = numPages;
        }

        for (int i = startFrom; i < fetchPages; i++) {
            RectF[] result = searchPage(i, text);
            if (result.length > 0) {
                searchResults.put(i, result);
            }
        }

        return searchResults;
    }
}