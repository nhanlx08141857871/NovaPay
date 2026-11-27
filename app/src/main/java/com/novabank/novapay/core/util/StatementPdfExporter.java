package com.novabank.novapay.core.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.novabank.novapay.R;
import com.novabank.novapay.domain.model.transaction.LedgerEntry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Kết xuất Sao kê Giao dịch Điện tử chuẩn A4 (595 x 842 pt) qua PdfDocument.
 */
public final class StatementPdfExporter {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 32;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);

    private StatementPdfExporter() {
    }

    @NonNull
    public static File exportStatementPdf(
            @NonNull Context context,
            @NonNull String customerName,
            @NonNull String accountLabel,
            @NonNull String periodLabel,
            long totalIn,
            long totalOut,
            @NonNull List<LedgerEntry> entries
    ) throws IOException {
        File statementsDir = new File(context.getCacheDir(), "statements");
        if (!statementsDir.exists() && !statementsDir.mkdirs()) {
            throw new IOException("Cannot create statements directory");
        }

        File outFile = new File(statementsDir, "novapay_statement_" + System.currentTimeMillis() + ".pdf");
        PdfDocument document = new PdfDocument();

        Paint navyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        navyPaint.setColor(Color.parseColor("#0F172A"));

        Paint subtleFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtleFillPaint.setColor(Color.parseColor("#F8FAFC"));

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.parseColor("#CBD5E1"));
        borderPaint.setStrokeWidth(1f);
        borderPaint.setStyle(Paint.Style.STROKE);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(Color.parseColor("#E2E8F0"));
        dividerPaint.setStrokeWidth(0.8f);

        TextPaint headerBrandPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerBrandPaint.setColor(Color.WHITE);
        headerBrandPaint.setTextSize(14f);
        headerBrandPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint headerTitlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headerTitlePaint.setColor(Color.parseColor("#10B981"));
        headerTitlePaint.setTextSize(11f);
        headerTitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint metaPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        metaPaint.setColor(Color.parseColor("#0F172A"));
        metaPaint.setTextSize(9.5f);

        TextPaint summaryPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        summaryPaint.setColor(Color.parseColor("#0F172A"));
        summaryPaint.setTextSize(9.5f);
        summaryPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint colHeaderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        colHeaderPaint.setColor(Color.parseColor("#0F172A"));
        colHeaderPaint.setTextSize(8.5f);
        colHeaderPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint cellPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        cellPaint.setColor(Color.parseColor("#1E293B"));
        cellPaint.setTextSize(8.2f);

        TextPaint amountInPaint = new TextPaint(cellPaint);
        amountInPaint.setColor(Color.parseColor("#16A34A"));
        amountInPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint amountOutPaint = new TextPaint(cellPaint);
        amountOutPaint.setColor(Color.parseColor("#DC2626"));
        amountOutPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        TextPaint footerPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        footerPaint.setColor(Color.parseColor("#64748B"));
        footerPaint.setTextSize(8f);

        // Pre-paginate rows so we know totalPages for footer
        final int firstPageStartY = 212;
        final int nextPageStartY = 76;
        final int maxContentY = PAGE_HEIGHT - 44;
        final int rowHeight = 26;

        List<List<LedgerEntry>> pages = paginateEntries(entries, firstPageStartY, nextPageStartY, maxContentY, rowHeight);
        int totalPages = Math.max(1, pages.size());

        try {
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                int pageNumber = pageIndex + 1;
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH,
                        PAGE_HEIGHT,
                        pageNumber
                ).create();
                PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawColor(Color.WHITE);

                float y;
                if (pageNumber == 1) {
                    // Header Banner
                    canvas.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 52, navyPaint);
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_bank),
                            MARGIN + 14,
                            MARGIN + 22,
                            headerBrandPaint
                    );
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_title),
                            MARGIN + 14,
                            MARGIN + 40,
                            headerTitlePaint
                    );

                    // Meta info
                    y = MARGIN + 72;
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_customer, customerName),
                            MARGIN,
                            y,
                            metaPaint
                    );
                    y += 15;
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_account, accountLabel),
                            MARGIN,
                            y,
                            metaPaint
                    );
                    y += 15;
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_period, periodLabel),
                            MARGIN,
                            y,
                            metaPaint
                    );
                    y += 15;
                    canvas.drawText(
                            context.getString(
                                    R.string.core_statement_pdf_exported_at,
                                    DateUtils.formatDateTime(Instant.now())
                            ),
                            MARGIN,
                            y,
                            metaPaint
                    );

                    // Summary Box
                    float boxTop = y + 12;
                    float boxBottom = boxTop + 30;
                    canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxBottom, subtleFillPaint);
                    canvas.drawRect(MARGIN, boxTop, PAGE_WIDTH - MARGIN, boxBottom, borderPaint);
                    canvas.drawText(
                            context.getString(
                                    R.string.core_statement_pdf_summary,
                                    CurrencyFormatter.formatVnd(totalIn),
                                    CurrencyFormatter.formatVnd(totalOut),
                                    entries.size()
                            ),
                            MARGIN + 10,
                            boxTop + 19,
                            summaryPaint
                    );

                    y = boxBottom + 16;
                } else {
                    canvas.drawText(
                            context.getString(R.string.core_statement_pdf_title) + " (" + accountLabel + ")",
                            MARGIN,
                            MARGIN + 16,
                            summaryPaint
                    );
                    y = nextPageStartY - 18;
                }

                // Table Column Header
                y = drawTableHeader(context, canvas, y, subtleFillPaint, dividerPaint, colHeaderPaint);

                List<LedgerEntry> pageEntries = pages.get(pageIndex);
                for (LedgerEntry entry : pageEntries) {
                    y = drawEntryRow(
                            canvas,
                            entry,
                            y,
                            rowHeight,
                            dividerPaint,
                            cellPaint,
                            amountInPaint,
                            amountOutPaint
                    );
                }

                // Footer page number
                String pageFooter = context.getString(R.string.core_statement_pdf_page, pageNumber, totalPages);
                float footerWidth = footerPaint.measureText(pageFooter);
                canvas.drawText(pageFooter, PAGE_WIDTH - MARGIN - footerWidth, PAGE_HEIGHT - 18, footerPaint);

                document.finishPage(page);
            }

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.writeTo(fos);
                fos.flush();
            }
            return outFile;
        } finally {
            document.close();
        }
    }

    private static List<List<LedgerEntry>> paginateEntries(
            @NonNull List<LedgerEntry> entries,
            int firstPageStartY,
            int nextPageStartY,
            int maxContentY,
            int rowHeight
    ) {
        List<List<LedgerEntry>> pages = new ArrayList<>();
        if (entries.isEmpty()) {
            pages.add(new ArrayList<>());
            return pages;
        }
        int firstCapacity = Math.max(1, (maxContentY - firstPageStartY) / rowHeight);
        int nextCapacity = Math.max(1, (maxContentY - nextPageStartY) / rowHeight);

        int index = 0;
        int firstEnd = Math.min(entries.size(), firstCapacity);
        pages.add(new ArrayList<>(entries.subList(0, firstEnd)));
        index = firstEnd;

        while (index < entries.size()) {
            int end = Math.min(entries.size(), index + nextCapacity);
            pages.add(new ArrayList<>(entries.subList(index, end)));
            index = end;
        }
        return pages;
    }

    private static float drawTableHeader(
            @NonNull Context context,
            @NonNull Canvas canvas,
            float topY,
            @NonNull Paint fillPaint,
            @NonNull Paint dividerPaint,
            @NonNull TextPaint colHeaderPaint
    ) {
        float rowBottom = topY + 20;
        canvas.drawRect(MARGIN, topY, PAGE_WIDTH - MARGIN, rowBottom, fillPaint);
        canvas.drawLine(MARGIN, topY, PAGE_WIDTH - MARGIN, topY, dividerPaint);
        canvas.drawLine(MARGIN, rowBottom, PAGE_WIDTH - MARGIN, rowBottom, dividerPaint);

        float textY = topY + 13;
        canvas.drawText(context.getString(R.string.core_statement_pdf_col_time), MARGIN + 4, textY, colHeaderPaint);
        canvas.drawText(context.getString(R.string.core_statement_pdf_col_txid), MARGIN + 86, textY, colHeaderPaint);
        canvas.drawText(context.getString(R.string.core_statement_pdf_col_desc), MARGIN + 162, textY, colHeaderPaint);
        canvas.drawText(context.getString(R.string.core_statement_pdf_col_amount), MARGIN + 365, textY, colHeaderPaint);
        canvas.drawText(context.getString(R.string.core_statement_pdf_col_balance), MARGIN + 455, textY, colHeaderPaint);
        return rowBottom;
    }

    private static float drawEntryRow(
            @NonNull Canvas canvas,
            @NonNull LedgerEntry entry,
            float topY,
            int rowHeight,
            @NonNull Paint dividerPaint,
            @NonNull TextPaint cellPaint,
            @NonNull TextPaint amountInPaint,
            @NonNull TextPaint amountOutPaint
    ) {
        float textY = topY + 16;
        String dateText = entry.getCreatedAt() != null ? DateUtils.formatDateTime(entry.getCreatedAt()) : "-";
        String txText = truncateText(entry.getTxId(), 12);
        String descText = truncateText(entry.getDescription(), 34);
        String signedAmount = CurrencyFormatter.formatSignedVnd(entry.getAmount(), entry.isInflow());
        String balanceAfter = CurrencyFormatter.formatVnd(entry.getBalanceAfter());

        canvas.drawText(dateText, MARGIN + 4, textY, cellPaint);
        canvas.drawText(txText.isEmpty() ? "-" : txText, MARGIN + 86, textY, cellPaint);
        canvas.drawText(descText, MARGIN + 162, textY, cellPaint);
        canvas.drawText(
                signedAmount,
                MARGIN + 365,
                textY,
                entry.isInflow() ? amountInPaint : amountOutPaint
        );
        canvas.drawText(balanceAfter, MARGIN + 455, textY, cellPaint);

        float bottomY = topY + rowHeight;
        canvas.drawLine(MARGIN, bottomY, PAGE_WIDTH - MARGIN, bottomY, dividerPaint);
        return bottomY;
    }

    @NonNull
    private static String truncateText(@Nullable String raw, int maxChars) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim().replace('\n', ' ');
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxChars - 1)) + "…";
    }
}
