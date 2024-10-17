package org.playwright_examples;

import java.io.*;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;

import java.net.URI;
import java.util.List;
import java.net.http.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Playwright_Multithreaded {

    public static void main(String[] args) {
        // Create an executor service with two threads for parallel execution
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // First browser instance (1 to n)
        executorService.submit(() -> {
            runFromStartToEnd();
        });

        // Second browser instance (n to 1)
        executorService.submit(() -> {
            runFromEndToStart();
        });

        executorService.shutdown();
    }

    public static void runFromStartToEnd() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://acme-test.uipath.com/login");

            // Login process
            page.getByLabel("Email:").fill("your-email");
            page.getByLabel("Password:").fill("your-password");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Work Items")).click();

            // Navigate to Work Items page and handle iteration logic
            int pageNumber = page.locator("ul.page-numbers, li.page, a.page-numbers").count();
            for (int i = 1; i <= pageNumber-1; i++) {
                handlePage(page, i);
            }

            System.out.println("Process Completed");
            page.close();
            context.close();
            browser.close();
            playwright.close();
        }
    }

    public static void runFromEndToStart() {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://acme-test.uipath.com/login");

            // Login process
            page.getByLabel("Email:").fill("your-email");
            page.getByLabel("Password:").fill("your-password");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();


            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Work Items")).click();

            // Navigate to Work Items page and handle iteration logic in reverse
            int pageNumber = page.locator("ul.page-numbers, li.page, a.page-numbers").count();
            for (int i = pageNumber-2; i >= 1; i--) {
                handlePage(page, i);
            }

            System.out.println("Process Completed");
            page.close();
            context.close();
            browser.close();
            playwright.close();

        }
    }

    public static void handlePage(Page page, int pageNumber) {
        try {
            // Navigate to the specified page
            page.navigate("https://acme-test.uipath.com/work-items?page=" + pageNumber);

            // Extract WI3 type links
            List<List<String>> wiidAndLinks = (List<List<String>>) page.locator("table tbody tr")
                    .evaluateAll("(rows) => rows.filter(row => row.cells[3].textContent.trim() === 'WI3')"
                            + ".map(row => [row.cells[1].textContent.trim(), row.querySelector('a').href, row.cells[4].textContent.trim()])");

            // Extract WIID, link, and Status for rows where Type is WI1 and Status is 'Open'
            List<List<String>> wiidAndLinks1 = (List<List<String>>) page.locator("table tbody tr")
                    .evaluateAll("(rows) => rows.filter(row => row.cells[3]?.textContent.trim() === 'WI1' && row.cells[4]?.textContent.trim() === 'Open')"
                            + ".map(row => [row.cells[1]?.textContent.trim(), row.querySelector('a')?.href, row.cells[4]?.textContent.trim()])");

            // Print WIID and links
            for (List<String> row : wiidAndLinks1) {
                System.out.println("WIID: " + row.get(0) + ", Link: " + row.get(1) + ", Status: " + row.get(2));
            }

            // Process each WI3 type work item
            wiidAndLinks.forEach(link -> {
                String wiid = link.get(0); // WIID
                String itemLink = link.get(1); // URL to navigate
                String status = link.get(2); // Status

                // Skip the item if the status is "Completed"
                if ("Completed".equalsIgnoreCase(status)) {
                    System.out.println("Skipping WIID " + wiid + " as its status is Completed.");
                } else {
                    page.navigate(itemLink);
                    String pdfUrl = page.locator("div.col-lg-5 a").nth(0).getAttribute("href");
                    System.out.println(pdfUrl);

                    // Download the PDF
                    downloadPDF(pdfUrl, "Invoice-642892.pdf");

                    // Extract the invoice data from the PDF
                    String filepath = "Invoice-642892.pdf";
                    extractInvoiceData(page, filepath, wiid);
                }
            });
        } catch (Exception e) {
            System.err.println("Error handling page " + pageNumber + ": " + e.getMessage());
        }
    }

    private static void downloadPDF(String pdfUrl, String filePath) {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(pdfUrl)).header("Accept", "application/pdf").GET().build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(response.body());
                    System.out.println("PDF saved to: " + filePath);
                } catch (IOException e) {
                    System.err.println("Error saving the PDF: " + e.getMessage());
                }
            } else {
                System.out.println("Failed to download PDF. HTTP response code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error downloading PDF: " + e.getMessage());
        }
    }

    private static void extractInvoiceData(Page page, String filepath, String wiid) {
        try (FileInputStream fstream = new FileInputStream(new File(filepath))) {
            // PDF parsing setup
            BodyContentHandler contentHandler = new BodyContentHandler();
            Metadata metadata = new Metadata();
            ParseContext context = new ParseContext();
            PDFParser pdfParser = new PDFParser();
            pdfParser.parse(fstream, contentHandler, metadata, context);

            String invoiceText = contentHandler.toString();

            // Extract fields using regex
            String invoiceId = extractValue(invoiceText, "(?i)(Invoice (ID|No\\.?):?\\s*(\\d+))", 3);
            String total = extractValue(invoiceText, "(?i)(Total(?:\\s*:\\s*|\\s+)([\\d,.]+)\\s*(\\w{3}))", 2);
            String subtotal = extractValue(invoiceText, "(?i)(Subtotal\\s*:\\s*([\\d,.]+)\\s*(\\w{3}))", 2);
            String tax = extractValue(invoiceText, "(?i)(Tax\\s*:\\s*([\\d,.]+)\\s*(\\w{3}))", 2);
            String itemDescription = extractValue(invoiceText, "(?i)Item Description\\s+(.+?)\\s+(Quantity|Unit Price)", 1);
            String taxId = extractValue(invoiceText, "(?i)Tax ID\\s*:\\s*([A-Z]+\\d+)", 1);
            String date = extractValue(invoiceText, "(?i)Date\\s*:\\s*([\\d-]+)", 1);
            String currency = extractValue(invoiceText, "(?i)(Total|Subtotal|Tax)\\s*:?\\s*[\\d,.]+\\s*([A-Z]{3})", 2);

            // Handling tax ID logic
            if (Objects.equals(taxId, "Not Found")) {
                updateWorkItem(page, wiid, "Vendor with tax is not present: " + invoiceId, true);
            } else {
                // Fill invoice details in UI
                page.navigate("https://acme-test.uipath.com/invoices/add");
                page.locator("#invoiceNumber").fill(invoiceId);
                page.locator("#vendorTaxID").fill(taxId);
                page.locator("#invoiceDate").fill(date);
                page.locator("#invoiceCurrency").fill(currency);
                page.locator("#itemDescription").fill(itemDescription);
                page.locator("#unTaxedAmount").fill(subtotal);
                page.locator("#taxedAmount").fill(tax);
                page.locator("#totalAmount").fill(total);
                page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add Invoice Details")).click();

                updateWorkItem(page, wiid, "Completed: " + invoiceId, false);
            }

        } catch (Exception e) {
            System.err.println("Error extracting invoice data for WIID " + wiid + ": " + e.getMessage());
        }
    }

    private static void updateWorkItem(Page page, String wiid, String comment, boolean taxIdMissing) {
        try {
            page.navigate("https://acme-test.uipath.com/work-items/update/" + wiid);
            page.locator("#newComment").fill(comment);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("---")).click();
            if (taxIdMissing) {
                page.getByRole(AriaRole.LISTBOX).getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Completed")).click();
            } else {
                page.getByRole(AriaRole.LISTBOX).getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Completed")).click();
            }
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update Work Item")).click();
        } catch (Exception e) {
            System.err.println("Error updating work item for WIID " + wiid + ": " + e.getMessage());
        }
    }

    // Method to extract value using regex pattern
    private static String extractValue(String text, String regex, int group) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(group);
        }
        return "Not Found";
    }

}

