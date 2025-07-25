package com.mycompany.org.analyzer;

import com.mycompany.org.util.CsvReader;

import java.io.IOException;
import java.util.List;

/**
 * Main application class to read employee data from a CSV and perform organizational analysis.
 */
public class Main {

    private static final String DEFAULT_CSV_PATH = "employees.csv";

    public static void main(String[] args) {
        String csvFilePath = DEFAULT_CSV_PATH;

        // Allow specifying CSV file path as a command-line argument
        if (args.length > 0) {
            csvFilePath = args[0];
        }

        System.out.println("Reading employee data from: " + csvFilePath);

        try {
            List<Employee> employees = CsvReader.readEmployeesFromCsv(csvFilePath);
            System.out.println("Successfully loaded " + employees.size() + " employees.");

            OrganizationAnalyzer analyzer = new OrganizationAnalyzer(employees);

            // Perform and print manager salary analysis
            analyzer.analyzeManagerSalaries();

            // Perform and print reporting line analysis
            analyzer.analyzeReportingLines();

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            System.err.println("Please ensure the file exists and is accessible at: " + csvFilePath);
        } catch (IllegalArgumentException e) {
            System.err.println("Data validation error: " + e.getMessage());
            System.err.println("Please check the CSV file for incorrect format or invalid data.");
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
