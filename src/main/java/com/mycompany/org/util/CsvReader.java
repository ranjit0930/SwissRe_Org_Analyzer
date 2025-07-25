package com.mycompany.org.util;

import com.mycompany.org.analyzer.Employee;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class for reading employee data from a CSV file.
 */
public class CsvReader {

    private static final String COMMA_DELIMITER = ",";
    private static final int ID_INDEX = 0;
    private static final int FIRST_NAME_INDEX = 1;
    private static final int LAST_NAME_INDEX = 2;
    private static final int SALARY_INDEX = 3;
    private static final int MANAGER_ID_INDEX = 4;

    /**
     * Reads employee data from the specified CSV file.
     *
     * @param filePath The path to the CSV file.
     * @return A list of Employee objects.
     * @throws IOException If an I/O error occurs while reading the file.
     * @throws IllegalArgumentException If the CSV format is incorrect or data is invalid.
     */
    public static List<Employee> readEmployeesFromCsv(String filePath) throws IOException {
        List<Employee> employees = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip the header line
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] values = line.split(COMMA_DELIMITER, -1); // -1 to keep trailing empty strings
                if (values.length != 5) {
                    throw new IllegalArgumentException("Invalid CSV line format: " + line);
                }

                try {
                    int id = Integer.parseInt(values[ID_INDEX].trim());
                    String firstName = values[FIRST_NAME_INDEX].trim();
                    String lastName = values[LAST_NAME_INDEX].trim();
                    long salary = Long.parseLong(values[SALARY_INDEX].trim());
                    Optional<Integer> managerId = values[MANAGER_ID_INDEX].trim().isEmpty() ?
                            Optional.empty() : Optional.of(Integer.parseInt(values[MANAGER_ID_INDEX].trim()));

                    employees.add(new Employee(id, firstName, lastName, salary, managerId));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format in CSV line: " + line, e);
                }
            }
        }
        return employees;
    }
}
