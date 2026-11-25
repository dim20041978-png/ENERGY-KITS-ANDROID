package ua.energykits.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

final class KitPrintAdapter extends PrintDocumentAdapter {
    private static final int PADDING = 24;
    private final Context context;
    private final String report;
    private final List<Integer> pageLines = new ArrayList<>();
    private StaticLayout layout;
    private PrintAttributes attributes;
    private Rect content;

    KitPrintAdapter(Context context, String report) {
        this.context = context;
        this.report = report;
    }

    @Override
    public void onLayout(PrintAttributes previous, PrintAttributes next,
                         CancellationSignal cancellation, LayoutResultCallback callback,
                         Bundle extras) {
        if (cancellation.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }
        attributes = next;
        PrintedPdfDocument document = new PrintedPdfDocument(context, next);
        try {
            content = new Rect(document.getPageContentRect());
        } finally {
            document.close();
        }
        content.inset(PADDING, PADDING);
        if (content.width() <= 0 || content.height() <= 0) {
            callback.onLayoutFailed("Замалий розмір сторінки");
            return;
        }
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        layout = StaticLayout.Builder.obtain(report, 0, report.length(), paint, content.width())
                .setLineSpacing(3, 1)
                .build();
        pageLines.clear();
        pageLines.add(0);
        int top = 0;
        for (int line = 0; line < layout.getLineCount(); line++) {
            if (layout.getLineBottom(line) - layout.getLineTop(line) > content.height()) {
                callback.onLayoutFailed("Замалий розмір сторінки");
                return;
            }
            if (layout.getLineBottom(line) - top > content.height()) {
                pageLines.add(line);
                top = layout.getLineTop(line);
            }
        }
        PrintDocumentInfo info = new PrintDocumentInfo.Builder("EnergyKit.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageLines.size())
                .build();
        callback.onLayoutFinished(info, !next.equals(previous));
    }

    @Override
    public void onWrite(PageRange[] requested, ParcelFileDescriptor destination,
                        CancellationSignal cancellation, WriteResultCallback callback) {
        PrintedPdfDocument document = new PrintedPdfDocument(context, attributes);
        List<PageRange> written = new ArrayList<>();
        try (OutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(destination)) {
            for (int index = 0; index < pageLines.size() && !cancellation.isCanceled(); index++) {
                if (!includes(requested, index)) continue;
                PdfDocument.Page page = document.startPage(index);
                Canvas canvas = page.getCanvas();
                int firstLine = pageLines.get(index);
                int lastLine = index + 1 < pageLines.size()
                        ? pageLines.get(index + 1) : layout.getLineCount();
                int top = layout.getLineTop(firstLine);
                canvas.clipRect(content.left, content.top, content.right,
                        content.top + layout.getLineTop(lastLine) - top);
                canvas.translate(content.left, content.top - top);
                layout.draw(canvas);
                document.finishPage(page);
                written.add(new PageRange(index, index));
            }
            if (!cancellation.isCanceled() && !written.isEmpty()) {
                document.writeTo(output);
            }
        } catch (IOException error) {
            callback.onWriteFailed("Не вдалося створити PDF");
            return;
        } finally {
            document.close();
        }
        if (cancellation.isCanceled()) callback.onWriteCancelled();
        else callback.onWriteFinished(written.toArray(new PageRange[0]));
    }

    private static boolean includes(PageRange[] ranges, int index) {
        for (PageRange range : ranges) {
            if (index >= range.getStart() && index <= range.getEnd()) return true;
        }
        return false;
    }
}
