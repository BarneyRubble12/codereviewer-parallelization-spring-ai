# Clean Code Standards

## Naming Conventions
- Use descriptive and meaningful names for variables, methods, and classes
- Follow Java naming conventions (camelCase for variables, PascalCase for classes)
- Avoid single-letter variable names (x, y, z) and abbreviations (cnt, lst, str)
- Use consistent naming patterns throughout the codebase
- Choose names that clearly express intent and purpose

## Code Structure
- Keep methods small and focused (ideally under 20 lines)
- Follow single responsibility principle - each method should do one thing
- Avoid deep nesting (more than 3 levels) and complex conditionals
- Use appropriate design patterns to reduce complexity
- Break down large methods into smaller, focused methods

## Method Design
- Limit method parameters (ideally 3 or fewer)
- Avoid long parameter lists
- Use early returns to reduce nesting
- Extract complex conditions into well-named boolean methods
- Remove unused methods and dead code

## Exception Handling
- Implement proper exception handling with specific exception types
- Never use generic Exception catch blocks without good reason
- Provide meaningful error messages that help with debugging
- Log errors appropriately with sufficient context
- Don't swallow exceptions silently

## Code Duplication
- Eliminate code duplication through extraction of common functionality
- Use helper methods for repeated logic
- Apply DRY (Don't Repeat Yourself) principle
- Consider using constants for repeated string literals

## Input Validation
- Always validate method parameters and user inputs
- Check for null values before processing
- Validate input ranges and formats
- Provide clear error messages for invalid inputs
