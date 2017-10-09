package sirius.kernel.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides functionality to build a config and generate formatted HOCON text.
 *
 * <p>
 * If scopes contain more than one variable, they will be unfolded and formatted as a block. Example:
 * <pre>
 * scopeA.foo = true
 * scopeA.bar = false
 * scopeB.foo = false
 * </pre>
 * will become
 * <pre>
 * scopeB.foo = false
 *
 * scopeA {
 *     foo = true
 *     bar = false
 * }
 * </pre>
 */
public class ConfigBuilder {

    private static final String INDENTATION = "    ";

    /**
     * Contains the root scope
     */
    private Scope config = new Scope();

    /**
     * Adds a variable to the config.
     *
     * <p>
     * Note: a string value needs to be surrounded by quotes
     *
     * @param name  the full name of the variable, e.g. "scopeA.scope1.variable"
     * @param value the value of the variable
     */
    public void addVariable(String name, String value) {
        String[] parts = name.split("\\.");
        Scope currentScope = config;

        for (int i = 0; i < parts.length - 1; i++) {
            currentScope = currentScope.getScope(parts[i]);
        }

        if (currentScope.hasVariable(parts[parts.length - 1])) {
            throw new IllegalArgumentException("Variable '" + name + "' already exists");
        }

        currentScope.addVariable(parts[parts.length - 1], value);
    }

    @Override
    public String toString() {
        return config.toString();
    }

    /**
     * Provides a base class for the elements of the config.
     */
    private abstract static class Node {

        private String name;

        Node() {
        }

        Node(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Provides the scope
     */
    private static class Scope extends Node {

        private boolean isRoot = false;
        private Map<String, Variable> variables = new LinkedHashMap<>();
        private Map<String, Scope> scopes = new LinkedHashMap<>();

        /**
         * Creates a new root scope.
         */
        Scope() {
            isRoot = true;
        }

        /**
         * Creates a new named scope.
         *
         * @param name the name of the scope
         */
        Scope(String name) {
            super(name);
        }

        /**
         * Returns a scope with the given name or creates one, if none exists with that name.
         *
         * @param name the name of the scope to return or create
         * @return the scope with the given name
         */
        private Scope getScope(String name) {
            Scope scope = scopes.get(name);

            if (scope == null) {
                scope = new Scope(name);

                scopes.put(name, scope);
            }

            return scope;
        }

        /**
         * Determines if a variable with the given name exists.
         *
         * @param name the name of the variable
         * @return <tt>true</tt> if the variable exists, <tt>false</tt> otherwise
         */
        private boolean hasVariable(String name) {
            return variables.containsKey(name);
        }

        /**
         * Creates a variable with the given name and value.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        private void addVariable(String name, String value) {
            variables.put(name, new Variable(name, value));
        }

        /**
         * Determines if the scope and sub scopes contain only one variable
         *
         * @return <tt>true</tt> if the scope only contains one variable, <tt>false</tt> otherwise
         */
        private boolean hasOnlyOneVariable() {
            // If there are only variables, simply check the count
            if (scopes.isEmpty()) {
                return variables.size() == 1;
            }

            // No need to check all sub scopes, because multiple scopes means multiple variables. Only check if there is
            // one scope and no variables
            return scopes.size() == 1 && variables.isEmpty() && scopes.values().iterator().next().hasOnlyOneVariable();
        }

        /**
         * Renders the config into a nicely formatted HOCON text and returns it
         *
         * @return the config as a formatted string
         */
        @Override
        public String toString() {
            StringBuilder variablesStringBuilder = new StringBuilder();
            StringBuilder scopeStringBuilder = new StringBuilder();

            for (Variable variable : variables.values()) {
                if (variablesStringBuilder.length() > 0) {
                    variablesStringBuilder.append("\n");
                }

                variablesStringBuilder.append(variable.toString());
            }

            for (Scope scope : scopes.values()) {
                // If the scope has only one variable, the scope will be collapsed and can reside with the variables
                if (scope.hasOnlyOneVariable()) {
                    if (variablesStringBuilder.length() > 0) {
                        variablesStringBuilder.append("\n");
                    }

                    variablesStringBuilder.append(scope.toString());
                } else {
                    if (scopeStringBuilder.length() > 0) {
                        scopeStringBuilder.append("\n\n");
                    }

                    scopeStringBuilder.append(scope.toString());
                }
            }

            return mergeVariablesAndScopes(variablesStringBuilder.toString(), scopeStringBuilder.toString());
        }

        /**
         * Merges the variable and scope blocks, indents them and returns the resulting scope.
         *
         * @param variablesString the variables of the scope
         * @param scopesString the sub scopes of the scope
         * @return the resulting formatted text
         */
        private String mergeVariablesAndScopes(String variablesString, String scopesString) {
            String mergedString;

            // Put an empty line between variables and the first scope block
            if (!variablesString.isEmpty() && !scopesString.isEmpty()) {
                mergedString = variablesString + "\n\n" + scopesString;
            } else {
                mergedString = variablesString + scopesString;
            }

            if (!isRoot) {
                StringBuilder stringBuilder = new StringBuilder();

                if (hasOnlyOneVariable()) {
                    // Prepend the variable with the scope name
                    stringBuilder.append(getName()).append(".").append(variablesString);
                } else {
                    // Wrap the block inside the scope
                    stringBuilder.append(getName()).append(" {\n");
                    stringBuilder.append(mergedString.replaceAll("(?m)^", INDENTATION));
                    stringBuilder.append("\n}");
                }

                // Remove indentation in empty lines
                return stringBuilder.toString().replaceAll("(?m)^\\s+$", "");
            }

            return mergedString;
        }
    }

    /**
     * Provides the variables
     */
    private static class Variable extends Node {
        private String value;

        /**
         * Create a new variable with the given name and value.
         *
         * @param name the name of the variable
         * @param value the value of the variable
         */
        Variable(String name, String value) {
            super(name);
            this.value = value;
        }

        @Override
        public String toString() {
            return getName() + " = " + value;
        }
    }
}