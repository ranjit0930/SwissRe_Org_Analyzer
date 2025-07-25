package com.mycompany.org.analyzer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * Represents an employee in the organizational structure.
 * Uses Lombok annotations for boilerplate code reduction (getters, setters, constructors, equals, hashCode, toString).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    private int id;
    private String firstName;
    private String lastName;
    private long salary;
    // Optional is used to clearly indicate that a managerId might not be present (for the CEO)
    private Optional<Integer> managerId;

    /**
     * Constructor for employees without a manager (CEO).
     * @param id The employee's unique identifier.
     * @param firstName The employee's first name.
     * @param lastName The employee's last name.
     * @param salary The employee's salary.
     */
    public Employee(int id, String firstName, String lastName, long salary) {
        this(id, firstName, lastName, salary, Optional.empty());
    }
}