import java.io.*;
import java.util.*;

class Rule {
    String head;
    List<String> body;

    public Rule(String head, List<String> body) {
        this.head = head;
        this.body = body;
    }

    @Override
    public String toString() {
        return "IF " + body + " THEN " + head;
    }
}

class KnowledgeBase {
    List<Rule> rules = new ArrayList<>();
    Set<String> facts = new HashSet<>();

    public List<Rule> getRules() {
        return rules;
    }

    public Set<String> getFacts() {
        return facts;
    }

    public void loadKnowledgeBase(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean readingFacts = true;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (readingFacts) {
                    if (line.contains("->")) {
                        readingFacts = false;
                    } else {
                        facts.addAll(Arrays.asList(line.split(",")));
                        continue;
                    }
                }
                if (!readingFacts) {
                    String[] parts = line.split("->");
                    if (parts.length != 2) {
                        throw new IllegalArgumentException("Invalid rule: " + line);
                    }
                    String head = parts[1].trim();
                    List<String> body = Arrays.asList(parts[0].trim().split(","));
                    rules.add(new Rule(head, body));
                }
            }
        }
    }
}

public class ForwardChaining {
    public static boolean entails(KnowledgeBase kb, String query) {
        if (kb.getFacts().contains(query)) {
            return true;
        }

        Map<String, Integer> count = new HashMap<>();
        Set<String> inferred = new HashSet<>(kb.getFacts());
        Queue<String> agenda = new LinkedList<>(kb.getFacts());

        // Initialize count
        for (Rule rule : kb.getRules()) {
            count.put(rule.head, rule.body.size());
        }

        while (!agenda.isEmpty()) {
            String p = agenda.poll();
            System.out.println("Processing: " + p);
            for (Rule rule : kb.getRules()) {
                if (rule.body.contains(p)) {
                    count.put(rule.head, count.get(rule.head) - 1);
                    if (count.get(rule.head) == 0) {
                        System.out.println("Inferred: " + rule.head);
                        if (rule.head.equals(query)) {
                            return true;
                        }
                        if (!inferred.contains(rule.head)) {
                            agenda.offer(rule.head);
                            inferred.add(rule.head);
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            KnowledgeBase kb = new KnowledgeBase();
            kb.loadKnowledgeBase("knowledge_base2.txt");
            System.out.println("Loaded facts: " + kb.getFacts());
            System.out.println("Loaded rules: " + kb.getRules());
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter the query: ");
            String query = scanner.nextLine();
            boolean result = entails(kb, query);
            System.out.println("The query " + query + " has been " + (result ? "proven." : "not proven."));
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}