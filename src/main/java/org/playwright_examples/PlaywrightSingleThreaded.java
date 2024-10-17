package org.playwright_examples;

import java.io.*;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.net.URI;
import java.util.List;
import java.net.http.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.*;


public class PlaywrightSingleThreaded {
    public static void main(String[] args) {
        String FILE_PATH = "Invoice-642892.pdf";
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(false));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();
            page.navigate("https://acme-test.uipath.com/login");

            // Login
            page.getByLabel("Email:").fill("your-email");
            page.getByLabel("Password:").fill("your-password");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            // Navigate to Work Items page
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Work Items")).click();

            // Get total number of pages (pagination count)
            int pageNumber = page.locator("ul.page-numbers, li.page, a.page-numbers").count(); // Fix locator for pagination

            // Iterate through all pages
            for (int i = 1; i < pageNumber-1; i++) {
                // Extract WI3 type links
                List<List<String>> wiidAndLinks = (List<List<String>>) page.locator("table tbody tr")
                        .evaluateAll("(rows) => rows.filter(row => row.cells[3].textContent.trim() === 'WI3')"
                                + ".map(row => [row.cells[1].textContent.trim(), row.querySelector('a').href, row.cells[4].textContent.trim()])");



                wiidAndLinks.forEach(link -> {
                            String wiid = link.get(0);  // WIID
                            String itemLink = link.get(1);  // URL to navigate
                            String status = link.get(2);  // Status

                            // Skip the item if the status is "Completed"
                            if ("Completed".equalsIgnoreCase(status)) {
                                System.out.println("Skipping WIID " + wiid + " as its status is Completed.");
                            } else {
                                page.navigate(link.get(1));
                                String pdfUrl = page.locator("div.col-lg-5 a").nth(0).getAttribute("href");
                                System.out.println(pdfUrl);

                                HttpClient client = HttpClient.newHttpClient();

                                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(pdfUrl)).header("Accept","application/pdf").GET().build();

                                try {
                                    HttpResponse<byte[]> response = client.send(request,HttpResponse.BodyHandlers.ofByteArray());

                                    if (response.statusCode() == 200) {
                                        try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
                                            fos.write(response.body());
                                            System.out.println("PDF saved to: " + FILE_PATH);
                                        } catch (IOException e) {
                                            System.err.println("Error saving the file: " + e.getMessage());
                                        }
                                    }else {
                                        System.out.println("Failed to download PDF. HTTP response code: " + response.statusCode());
                                    }
                                } catch (IOException | InterruptedException e) {
                                    throw new RuntimeException(e);
                                }

                                String filepath = "Invoice-642892.pdf";
                                // Use try-with-resources to ensure resources are closed
                                try (FileInputStream fstream = new FileInputStream(new File(filepath))) {
                                    // Create content handler for PDF extraction
                                    BodyContentHandler contentHandler = new BodyContentHandler();
                                    Metadata metadata = new Metadata();
                                    ParseContext context1 = new ParseContext();

                                    // Create a PDFParser object
                                    PDFParser pdfParser = new PDFParser();

                                    // Parse the document
                                    pdfParser.parse(fstream, contentHandler, metadata, context1);

                                    String invoiceText = contentHandler.toString();
                                    // Patterns for extracting data
                                    String invoiceIdPattern = "(?i)(Invoice (ID|No\\.?):?\\s*(\\d+))";
                                    String totalPattern = "(?i)(Total(?:\\s*:\\s*|\\s+)([\\d,.]+)\\s*(\\w{3}))";
                                    String subtotalPattern = "(?i)(Subtotal\\s*:\\s*([\\d,.]+)\\s*(\\w{3}))";
                                    String taxPattern = "(?i)(Tax\\s*:\\s*([\\d,.]+)\\s*(\\w{3}))";
                                    String itemDescriptionPattern = "(?i)Item Description\\s+(.+?)\\s+(Quantity|Unit Price)";
                                    String taxIdPattern = "(?i)Tax ID\\s*:\\s*([A-Z]+\\d+)";
                                    String datePattern = "(?i)Date\\s*:\\s*([\\d-]+)";
                                    String currencyPattern = "(?i)(Total|Subtotal|Tax)\\s*:?\\s*[\\d,.]+\\s*([A-Z]{3})";


                                    // Extract values
                                    String invoiceId = extractValue(invoiceText, invoiceIdPattern, 3);
                                    String total = extractValue(invoiceText, totalPattern, 2);
                                    String subtotal = extractValue(invoiceText, subtotalPattern, 2);
                                    String tax = extractValue(invoiceText, taxPattern, 2);
                                    String itemDescription = extractValue(invoiceText, itemDescriptionPattern, 1);
                                    String taxId = extractValue(invoiceText, taxIdPattern, 1);
                                    String date = extractValue(invoiceText, datePattern, 1);
                                    String currency = extractValue(invoiceText, currencyPattern, 2);

                                    Boolean taxIdFlag = false;


                                    if(Objects.equals(taxId, "Not Found")){
                                        taxIdFlag = true;
                                    }else{
                                        taxIdFlag = false;
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
                                    }



                                    page.navigate("https://acme-test.uipath.com/work-items/update/"+link.get(0));
                                    if (taxIdFlag){
                                        page.locator("#newComment").fill("Vendor with tax is not present"+ invoiceId);
                                        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("---")).click();
                                        page.getByRole(AriaRole.LISTBOX).getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Completed")).click();
                                        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update Work Item")).click();

                                    }else {
                                        page.locator("#newComment").fill("Completed"+ invoiceId);

                                        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("---")).click();
                                        page.getByRole(AriaRole.LISTBOX).getByRole(AriaRole.OPTION, new Locator.GetByRoleOptions().setName("Completed")).click();
                                        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Update Work Item")).click();
                                    }

                                } catch (FileNotFoundException e) {
                                    System.err.println("File not found: " + e.getMessage());
                                } catch (IOException e) {
                                    System.err.println("I/O error: " + e.getMessage());
                                } catch (TikaException e) {
                                    System.err.println("Tika parsing error: " + e.getMessage());
                                } catch (SAXException e) {
                                    System.err.println("SAX parsing error: " + e.getMessage());
                                }
                            }
                });



                // Navigate to the next page if not the last one
                if (i < pageNumber) {
                    page.navigate("https://acme-test.uipath.com/work-items?page=" + (i + 1));
                }

            }

            System.out.println("Process Completed");
            browser.close();
            playwright.close();
            System.exit(0);
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