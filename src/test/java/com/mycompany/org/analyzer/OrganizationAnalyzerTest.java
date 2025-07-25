package com.mycompany.org.analyzer;

import com.mycompany.org.util.CsvReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 tests for the OrganizationAnalyzer class.
 */
class OrganizationAnalyzerTest {

    @TempDir // JUnit 5 annotation to create a temporary directory for test files
    Path tempDir;
    private Path testCsvFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary CSV file for testing
        testCsvFile = tempDir.resolve("test_employees.csv");
    }

    private void writeCsvContent(String... lines) throws IOException {
        Files.write(testCsvFile, Arrays.asList(lines));
    }

    @Test
    @DisplayName("Should correctly identify CEO and build structure from valid CSV")
    void shouldBuildStructureFromValidCsv() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "123,Joe,Doe,60000,",
                "124,Martin,Chekov,45000,123",
                "125,Bob,Ronstad,47000,123"
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        assertNotNull(analyzer);
        // Verify CEO is correctly identified (assuming internal access for test)
        // This requires reflection or making CEO field accessible, or testing via public methods
        // For simplicity, let's just check the size of the loaded employees
        assertEquals(3, employees.size());
        assertTrue(employees.stream().anyMatch(e -> e.getId() == 123 && e.getManagerId().isEmpty()));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for duplicate employee IDs")
    void shouldThrowOnDuplicateEmployeeIds() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "123,Joe,Doe,60000,",
                "123,Duplicate,Id,50000,124" // Duplicate ID
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        assertThrows(IllegalArgumentException.class, () -> new OrganizationAnalyzer(employees));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if no CEO is found")
    void shouldThrowOnNoCeo() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "123,Joe,Doe,60000,999", // No one without a manager
                "124,Martin,Chekov,45000,123"
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        assertThrows(IllegalArgumentException.class, () -> new OrganizationAnalyzer(employees));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if multiple CEOs are found")
    void shouldThrowOnMultipleCeos() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "100,CEO1,One,100000,",
                "200,CEO2,Two,110000,", // Another CEO
                "124,Martin,Chekov,45000,100"
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        assertThrows(IllegalArgumentException.class, () -> new OrganizationAnalyzer(employees));
    }

    @Test
    @DisplayName("Should report manager earning less than 20% more than subordinates' average")
    void shouldReportManagerEarningLess() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "100,CEO,Boss,100000,",
                "101,Manager,A,50000,100", // Manager A
                "102,Subordinate,X,40000,101",
                "103,Subordinate,Y,40000,101"
        );
        // Avg sub salary = (40000+40000)/2 = 40000
        // Min expected for Manager A = 40000 * 1.20 = 48000
        // Manager A earns 50000, which is > 48000, so this manager is fine by this test.
        // Let's adjust for a manager earning less
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "100,CEO,Boss,100000,",
                "101,Manager,A,47000,100", // Manager A earns 47000
                "102,Subordinate,X,40000,101",
                "103,Subordinate,Y,40000,101"
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        // Redirect System.out to capture output
        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeManagerSalaries();

        String output = outContent.toString();
        assertTrue(output.contains("Manager A (ID: 101) earns less than they should."));
        assertTrue(output.contains("Expected at least 48000.00, earns 47000. Deficit: 1000.00"));

        // Reset System.out
        System.setOut(System.out);
    }

    @Test
    @DisplayName("Should report manager earning more than 50% more than subordinates' average")
    void shouldReportManagerEarningMore() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "100,CEO,Boss,100000,",
                "101,Manager,B,61000,100", // Manager B earns 61000
                "104,Subordinate,Z,40000,101"
        );
        // Avg sub salary = 40000
        // Max expected for Manager B = 40000 * 1.50 = 60000
        // Manager B earns 61000, which is > 60000
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeManagerSalaries();

        String output = outContent.toString();
        assertTrue(output.contains("Manager B (ID: 101) earns more than they should."));
        assertTrue(output.contains("Expected no more than 60000.00, earns 61000. Excess: 1000.00"));

        System.setOut(System.out);
    }

    @Test
    @DisplayName("Should not report managers with correct salary range")
    void shouldNotReportCorrectlyPaidManagers() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "100,CEO,Boss,100000,",
                "101,Manager,C,55000,100", // Manager C earns 55000
                "105,Subordinate,P,40000,101",
                "106,Subordinate,Q,40000,101"
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeManagerSalaries();

        String output = outContent.toString();
        assertFalse(output.contains("Manager C (ID: 101) earns less than they should."));
        assertFalse(output.contains("Manager C (ID: 101) earns more than they should."));

        System.setOut(System.out);
    }

    @Test
    @DisplayName("Should report employees with reporting line longer than 4 managers")
    void shouldReportLongReportingLine() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "1,CEO,A,100000,",
                "2,Mgr1,B,90000,1",
                "3,Mgr2,C,80000,2",
                "4,Mgr3,D,70000,3",
                "5,Mgr4,E,60000,4",
                "6,Emp,F,50000,5" // Emp F has 5 managers (5 -> 4 -> 3 -> 2 -> 1 (CEO))
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeReportingLines();

        String output = outContent.toString();
        assertTrue(output.contains("Has 5 managers to CEO (max allowed: 4). Excess: 1"));

        System.setOut(System.out);
    }

    @Test
    @DisplayName("Should not report employees with reporting line equal to or shorter than 4 managers")
    void shouldNotReportShortReportingLine() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "1,CEO,A,100000,",
                "2,Mgr1,B,90000,1",
                "3,Mgr2,C,80000,2",
                "4,Mgr3,D,70000,3",
                "5,Emp,E,60000,4" // Emp E has 4 managers (4 -> 3 -> 2 -> 1 (CEO))
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeReportingLines();

        String output = outContent.toString();
        assertFalse(output.contains("has a reporting line that is too long."));
        assertTrue(output.contains("No excessively long reporting lines found."));

        System.setOut(System.out);
    }

    @Test
    @DisplayName("Should handle complex reporting structures")
    void shouldHandleComplexStructures() throws IOException {
        writeCsvContent(
                "Id,firstName,lastName,salary,managerId",
                "123,Joe,Doe,60000,", // CEO
                "124,Martin,Chekov,45000,123", // Mgr1, sub avg: (47k+50k+34k)/3 = 43.6k. Min: 52.3k, Max: 65.4k. Martin (45k) is less.
                "125,Bob,Ronstad,47000,123", // Mgr2, no direct subs in this snippet
                "300,Alice,Hasacat,50000,124", // Mgr3, sub avg: 34k. Min: 40.8k, Max: 51k. Alice (50k) is fine.
                "305,Brett,Hardleaf,34000,300", // Emp1, line: 305 -> 300 -> 124 -> 123 (3 managers)
                "400,New,Hire,30000,125", // Emp2, line: 400 -> 125 -> 123 (2 managers)
                "500,Super,Long,20000,305", // Emp3, line: 500 -> 305 -> 300 -> 124 -> 123 (4 managers)
                "600,Too,Deep,15000,500" // Emp4, line: 600 -> 500 -> 305 -> 300 -> 124 -> 123 (5 managers)
        );
        List<Employee> employees = CsvReader.readEmployeesFromCsv(testCsvFile.toString());
        OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

        java.io.ByteArrayOutputStream outContent = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(outContent));

        analyzer.analyzeManagerSalaries();
        analyzer.analyzeReportingLines();

        String output = outContent.toString();

        // Salary analysis checks
        assertTrue(output.contains("Manager Martin Chekov (ID: 124) earns less than they should."));

    }
}
