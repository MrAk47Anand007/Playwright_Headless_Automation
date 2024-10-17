
# ACME System Automation with Playwright and PDF Parsing

This project is a Java-based automation solution for interacting with the **ACME System3**, extracting data from work items, and automating the process of downloading and parsing PDF invoices using **Playwright** and **Apache Tika**.

## Key Features

- **Playwright Automation**: Automates web interactions with ACME System3 to extract data from work items (specifically WI3 entries).
- **PDF Downloading**: Downloads PDF invoices linked to each work item using HTTP requests.
- **Apache Tika Integration**: Parses downloaded PDFs to extract key invoice data like Invoice ID, Subtotal, Tax, and Vendor Tax ID.
- **Dynamic Data Input**: Automatically populates ACME System3 web forms with the extracted PDF data.
- **Multithreading**: Supports multithreaded browser sessions for faster processing.

## Prerequisites

- **Java 11+**: Ensure that you have JDK 11 or higher installed.
- **Maven**: This project uses Maven for dependency management.
- **Playwright**: Install Playwright via Maven dependencies for browser automation.
- **Apache Tika**: Used for extracting text content from PDFs.

### Playwright and Tika Setup

In your `pom.xml` file, include the necessary dependencies:

```xml
<dependencies>
    <!-- Playwright Dependency -->
    <dependency>
        <groupId>com.microsoft.playwright</groupId>
        <artifactId>playwright</artifactId>
        <version>1.22.0</version>
    </dependency>

    <!-- Apache Tika Dependency -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>1.28.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers</artifactId>
        <version>1.28.0</version>
    </dependency>
</dependencies>
```

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/acme-playwright-automation.git
   cd acme-playwright-automation
   ```

2. **Install dependencies**:
   Make sure Maven is installed and run:
   ```bash
   mvn clean install
   ```

3. **Run the Single-threaded Automation**:
   To run the basic automation that processes work items one at a time:
   ```bash
   mvn exec:java -Dexec.mainClass="com.acme.automation.PlaywrightSingleThreaded"
   ```

4. **Run the Multithreaded Automation**:
   To run the multithreaded automation that processes work items using two browser instances:
   ```bash
   mvn exec:java -Dexec.mainClass="com.acme.automation.Playwright_Multithreaded"
   ```

## Project Structure

- **PlaywrightSingleThreaded.java**: Handles the automation in a single-threaded manner, processing work items sequentially.
- **Playwright_Multithreaded.java**: A multithreaded version of the automation that uses two concurrent browser sessions to process work items faster.
- **PDFDownloader.java**: Utility class for downloading PDFs from the provided URL links.
- **PDFParser.java**: Uses Apache Tika to parse the downloaded PDFs and extract invoice details using regular expressions.
- **WorkItemProcessor.java**: Contains logic to fill web forms with the extracted PDF data and handle missing tax ID scenarios.

## How it Works

1. **Web Automation with Playwright**:
   - Logs into the ACME System3.
   - Extracts work items related to WI3 from a paginated table.
   - For each work item, downloads the associated PDF invoice.
  
2. **PDF Processing with Apache Tika**:
   - Extracts key invoice fields such as Invoice ID, Subtotal, Tax, and Vendor Tax ID from the downloaded PDFs.
  
3. **Data Entry**:
   - Automatically fills the extracted invoice details into the ACME System3 web forms.

4. **Multithreading**:
   - Two browser instances process pages in parallel: one from page 1 to N, and another from page N to 1, improving efficiency.

## Usage

### Single-threaded Execution

Run the `PlaywrightSingleThreaded` class to execute the workflow sequentially. This mode is useful for simpler, smaller datasets.

### Multithreaded Execution

Run the `Playwright_Multithreaded` class for parallel browser sessions. This approach speeds up the workflow by processing pages concurrently.

### PDF Parsing

The `PDFParser` class automatically extracts text and key invoice fields from downloaded PDFs using **Apache Tika** and **Regular Expressions**.

## Future Enhancements

- Add error handling and logging for robustness.
- Support for more complex PDF structures.
- Integration with additional RPA tools like UiPath or Automation Anywhere for more sophisticated workflows.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Commit your changes (`git commit -m 'Add some feature'`).
4. Push to the branch (`git push origin feature-branch`).
5. Open a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
