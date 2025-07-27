package com.mycompany.org.analyzer;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes the organizational structure for salary discrepancies and long reporting lines.
 */
public class OrganizationAnalyzer implements OrganizationAnalyzerInterface {

    private final Map<Integer, Employee> employeesById;
    private final Map<Integer, List<Employee>> subordinatesByManagerId;
    private final Employee ceo;

    // Constants for salary analysis
    private static final double MIN_SALARY_FACTOR = 1.20; // Manager should earn at least 20% more
    private static final double MAX_SALARY_FACTOR = 1.50; // Manager should earn no more than 50% more
    private static final int MAX_REPORTING_LINE_DEPTH = 4; // Max managers between employee and CEO

    /**
     * Constructs an OrganizationAnalyzer with a list of employees.
     * Initializes internal data structures for efficient lookup and analysis.
     *
     * @param employees A list of all employees, including the CEO.
     * @throws IllegalArgumentException If the CEO cannot be identified or if there are duplicate employee IDs.
     */
    public OrganizationAnalyzer(List<Employee> employees) {
        // Initialize employeesById map for quick lookups
        this.employeesById = employees.stream()
                .collect(Collectors.toMap(Employee::getId, employee -> employee,
                        (existing, replacement) -> {
                            throw new IllegalArgumentException("Duplicate employee ID found: " + existing.getId());
                        }));

        // Initialize subordinatesByManagerId map
        this.subordinatesByManagerId = employees.stream()
                .filter(employee -> employee.getManagerId().isPresent())
                .collect(Collectors.groupingBy(employee -> employee.getManagerId().get()));

        // Identify the CEO
        List<Employee> ceos = employees.stream()
                .filter(employee -> employee.getManagerId().isEmpty())
                .collect(Collectors.toList());

        if (ceos.size() != 1) {
            throw new IllegalArgumentException("Expected exactly one CEO, but found " + ceos.size());
        }
        this.ceo = ceos.get(0);
    }

    /**
     * Analyzes manager salaries to identify those earning less or more than they should.
     * Prints the results to the console.
     */
    @Override
    public void analyzeManagerSalaries() {
        System.out.println("\n--- Manager Salary Analysis ---");
        DecimalFormat df = new DecimalFormat("#.##"); // For formatting currency differences

        boolean foundDiscrepancy = false;

        // Iterate through all employees who are managers (have subordinates)
        for (Map.Entry<Integer, List<Employee>> entry : subordinatesByManagerId.entrySet()) {
            int managerId = entry.getKey();
            List<Employee> subordinates = entry.getValue();

            Employee manager = employeesById.get(managerId);

            // Skip if manager not found (should not happen if data is consistent) or if no subordinates
            if (manager == null || subordinates.isEmpty()) {
                continue;
            }

            double averageSubordinateSalary = subordinates.stream()
                    .mapToLong(Employee::getSalary)
                    .average()
                    .orElse(0.0); // Should not be 0.0 if subordinates is not empty, but good practice

            double minExpectedManagerSalary = averageSubordinateSalary * MIN_SALARY_FACTOR;
            double maxExpectedManagerSalary = averageSubordinateSalary * MAX_SALARY_FACTOR;

            long managerSalary = manager.getSalary();

            if (managerSalary < minExpectedManagerSalary) {
                double difference = minExpectedManagerSalary - managerSalary;
                System.out.println(String.format("Manager %s %s (ID: %d) earns less than they should." +
                                " Expected at least %.2f, earns %d. Deficit: %.2f",
                        manager.getFirstName(), manager.getLastName(), manager.getId(),
                        minExpectedManagerSalary, managerSalary, difference));
                foundDiscrepancy = true;
            } else if (managerSalary > maxExpectedManagerSalary) {
                double difference = managerSalary - maxExpectedManagerSalary;
                System.out.println(String.format("Manager %s %s (ID: %d) earns more than they should." +
                                " Expected no more than %.2f, earns %d. Excess: %.2f",
                        manager.getFirstName(), manager.getLastName(), manager.getId(),
                        maxExpectedManagerSalary, managerSalary, difference));
                foundDiscrepancy = true;
            }
        }

        if (!foundDiscrepancy) {
            System.out.println("No salary discrepancies found among managers.");
        }
    }

    /**
     * Analyzes reporting lines to identify employees with too many managers between them and the CEO.
     * Prints the results to the console.
     */
    @Override
    public void analyzeReportingLines() {
        System.out.println("\n--- Reporting Line Analysis ---");
        boolean foundLongLine = false;

        // Iterate through all employees, excluding the CEO
        for (Employee employee : employeesById.values()) {
            if (employee.equals(ceo)) {
                continue; // CEO has no managers above them
            }

            int managerCount = getManagerCountToCEO(employee);

            if (managerCount > MAX_REPORTING_LINE_DEPTH) {
                int excessManagers = managerCount - MAX_REPORTING_LINE_DEPTH;
                System.out.println(String.format("Employee %s %s (ID: %d) has a reporting line that is too long." +
                                " Has %d managers to CEO (max allowed: %d). Excess: %d",
                        employee.getFirstName(), employee.getLastName(), employee.getId(),
                        managerCount, MAX_REPORTING_LINE_DEPTH, excessManagers));
                foundLongLine = true;
            }
        }

        if (!foundLongLine) {
            System.out.println("No excessively long reporting lines found.");
        }
    }

    /**
     * Recursively calculates the number of managers between an employee and the CEO.
     *
     * @param employee The employee to start counting from.
     * @return The number of managers, or -1 if the CEO is not found in the reporting line (should not happen with valid data).
     */
    private int getManagerCountToCEO(Employee employee) {
        int count = 0;
        Employee current = employee;

        // Traverse up the hierarchy until the CEO is reached or no manager is found
        while (current.getManagerId().isPresent()) {
            count++;
            int managerId = current.getManagerId().get();
            current = employeesById.get(managerId);

            if (current == null) {
                // This indicates an invalid managerId in the data, pointing to a non-existent employee
                System.err.println("Warning: Manager with ID " + managerId + " not found for employee " + employee.getId());
                return -1; // Or throw an exception, depending on desired error handling
            }
            if (current.equals(ceo)) {
                return count; // Reached the CEO
            }
        }
        // If loop finishes and CEO is not reached, it implies an employee without a manager who is not the CEO
        // or a broken chain. For this problem, CEO is the only one without a manager.
        return count;
    }
}
