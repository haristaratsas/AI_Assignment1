import java.io.*;
import java.util.*;

public class ForwardChainingHorn {

    static class Predicate {
        String name;
        List<String> arguments;

        public Predicate(String name, List<String> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Predicate predicate = (Predicate) obj;
            return name.equals(predicate.name) && arguments.equals(predicate.arguments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments);
        }

        @Override
        public String toString() {
            return name + arguments;
        }
    }

    static class Rule {
        List<Predicate> conditions;
        Predicate conclusion;

        public Rule(List<Predicate> conditions, Predicate conclusion) {
            this.conditions = conditions;
            this.conclusion = conclusion;
        }

        @Override
        public String toString() {
            return "IF " + conditions + " THEN " + conclusion;
        }
    }

    Set<Predicate> facts = new HashSet<>();
    List<Rule> rules = new ArrayList<>();

    public void loadKnowledgeBase(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean readingFacts = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (readingFacts) {
                    if (line.contains("->")) {
                        readingFacts = false;
                    } else {
                        facts.add(parsePredicate(line));
                        continue;
                    }
                }
                if (!readingFacts) {
                    String[] parts = line.split("->");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid rule: " + line);
                    }
                    List<Predicate> conditions = new ArrayList<>();
                    for (String condStr : parts[0].split("\\^")) {
                        conditions.add(parsePredicate(condStr.trim()));
                    }
                    Predicate conclusion = parsePredicate(parts[1].trim());
                    rules.add(new Rule(conditions, conclusion));
                }
            }
        }
    }

    public Predicate parsePredicate(String input) {
        int start = input.indexOf('(');
        int end = input.lastIndexOf(')');
        String name;
        List<String> arguments = new ArrayList<>();
        if (start != -1 && end != -1 && start < end) {
            name = input.substring(0, start).trim();
            String argsStr = input.substring(start + 1, end);
            for (String arg : argsStr.split(",")) {
                arguments.add(arg.trim());
            }
        } else {
            name = input.trim();
        }
        return new Predicate(name, arguments);
    }

    public boolean forwardChaining(Predicate query) {
        Set<Predicate> inferred = new HashSet<>(facts);
        boolean addedNewFact;
        do {
            addedNewFact = false;
            for (Rule rule : rules) {
                List<Map<String, String>> substitutions = findAllSubstitutions(rule.conditions, inferred);
                for (Map<String, String> theta : substitutions) {
                    Predicate conclusion = substitute(rule.conclusion, theta);
                    if (!inferred.contains(conclusion)) {
                        inferred.add(conclusion);
                        addedNewFact = true;
                        System.out.println("Inferred: " + conclusion);
                        if (unify(conclusion, query, new HashMap<>())) {
                            return true;
                        }
                    }
                }
            }
        } while (addedNewFact);
        return inferred.contains(query);
    }

    private List<Map<String, String>> findAllSubstitutions(List<Predicate> conditions, Set<Predicate> inferred) {
        List<Map<String, String>> results = new ArrayList<>();
        findAllSubstitutionsRecursive(conditions, inferred, new HashMap<>(), results);
        return results;
    }

    private void findAllSubstitutionsRecursive(List<Predicate> conditions, Set<Predicate> inferred, Map<String, String> currentTheta, List<Map<String, String>> results) {
        if (conditions.isEmpty()) {
            results.add(new HashMap<>(currentTheta));
            return;
        }
        Predicate first = conditions.get(0);
        List<Predicate> rest = conditions.subList(1, conditions.size());
        for (Predicate fact : inferred) {
            Map<String, String> thetaCopy = new HashMap<>(currentTheta);
            if (unify(first, fact, thetaCopy)) {
                findAllSubstitutionsRecursive(rest, inferred, thetaCopy, results);
            }
        }
    }

    private boolean unify(Predicate p1, Predicate p2, Map<String, String> theta) {
        if (!p1.name.equals(p2.name) || p1.arguments.size() != p2.arguments.size()) {
            return false;
        }
        for (int i = 0; i < p1.arguments.size(); i++) {
            String arg1 = p1.arguments.get(i);
            String arg2 = p2.arguments.get(i);
            if (!unifyArgs(arg1, arg2, theta)) {
                return false;
            }
        }
        return true;
    }

    private boolean unifyArgs(String arg1, String arg2, Map<String, String> theta) {
        if (arg1.equals(arg2)) {
            return true;
        }
        if (isVariable(arg1)) {
            return unifyVar(arg1, arg2, theta);
        }
        if (isVariable(arg2)) {
            return unifyVar(arg2, arg1, theta);
        }
        return false;
    }

    private boolean unifyVar(String var, String value, Map<String, String> theta) {
        if (theta.containsKey(var)) {
            return unifyArgs(theta.get(var), value, theta);
        } else if (theta.containsKey(value)) {
            return unifyArgs(var, theta.get(value), theta);
        } else {
            theta.put(var, value);
            return true;
        }
    }

    private boolean isVariable(String arg) {
        return Character.isLowerCase(arg.charAt(0));
    }

    private Predicate substitute(Predicate predicate, Map<String, String> theta) {
        List<String> newArgs = new ArrayList<>();
        for (String arg : predicate.arguments) {
            if (theta.containsKey(arg)) {
                newArgs.add(theta.get(arg));
            } else {
                newArgs.add(arg);
            }
        }
        return new Predicate(predicate.name, newArgs);
    }

    public static void main(String[] args) {
        ForwardChainingHorn reasoner = new ForwardChainingHorn();
        try {
            reasoner.loadKnowledgeBase("knowledge_base.txt");
            System.out.println("Loaded facts: " + reasoner.facts);
            System.out.println("Loaded rules: " + reasoner.rules);
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the query (e.g., Criminal(West)): ");
            String queryInput = scanner.nextLine();
            Predicate query = reasoner.parsePredicate(queryInput);
            System.out.println("Query: " + query);
            boolean result = reasoner.forwardChaining(query);
            if (result) {
                System.out.println("The query " + query + " has been proven.");
            } else {
                System.out.println("The query " + query + " cannot be proven.");
            }
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
