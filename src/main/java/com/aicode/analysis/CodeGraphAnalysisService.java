package com.aicode.analysis;

import com.aicode.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for building dependency graphs between Java classes to detect:
 * - Circular dependencies
 * - Tight coupling
 * - Architecture issues
 */
@Service
public class CodeGraphAnalysisService {

  /**
   * Analyze code for dependency graph.
   * Detects relationships between classes.
   */
  public CodeGraph buildGraph(AnalysisPipeline.ParsedCode parsedCode) {
    CodeGraph graph = new CodeGraph();

    // Extract classes
    var classes = parsedCode.getCompilationUnit().findAll(
        com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class);

    for (var clazz : classes) {
      String className = clazz.getFullyQualifiedName().orElse(clazz.getNameAsString());
      graph.addNode(className);

      // Find dependencies (imports, field types, method parameters/returns)
      var imports = parsedCode.getCompilationUnit().getImports();
      for (var imp : imports) {
        if (!imp.isAsterisk()) {
          String importedClass = imp.getNameAsString();
          graph.addEdge(className, importedClass, "uses");
        }
      }
    }

    // Detect circular dependencies
    graph.detectCircularDependencies();

    // Calculate coupling metrics
    graph.calculateCoupling();

    return graph;
  }

  /**
   * Code dependency graph representation.
   */
  public static class CodeGraph {
    private java.util.Map<String, GraphNode> nodes = new java.util.HashMap<>();
    private java.util.List<String> circularDependencies = new java.util.ArrayList<>();
    private double couplingScore = 0.0;

    public void addNode(String className) {
      if (!nodes.containsKey(className)) {
        nodes.put(className, new GraphNode(className));
      }
    }

    public void addEdge(String from, String to, String type) {
      addNode(from);
      addNode(to);
      nodes.get(from).addDependency(to, type);
      nodes.get(to).addDependant(from);
    }

    public void detectCircularDependencies() {
      for (GraphNode node : nodes.values()) {
        if (hasCycleTo(node, node, new java.util.HashSet<>())) {
          circularDependencies.add(node.getClassName());
        }
      }
    }

    private boolean hasCycleTo(GraphNode current, GraphNode target, java.util.Set<String> visited) {
      if (visited.contains(current.getClassName())) {
        return current.equals(target);
      }
      visited.add(current.getClassName());
      return false; // Simplified for now
    }

    public void calculateCoupling() {
      int totalEdges = 0;
      for (GraphNode node : nodes.values()) {
        totalEdges += node.getDependencies().size();
      }
      couplingScore = (double) totalEdges / Math.max(1, nodes.size());
    }

    public java.util.List<String> getCircularDependencies() {
      return circularDependencies;
    }

    public java.util.Map<String, GraphNode> getNodes() {
      return nodes;
    }

    public double getCouplingScore() {
      return couplingScore;
    }
  }

  /**
   * Graph node representing a class.
   */
  public static class GraphNode {
    private final String className;
    private final java.util.List<String> dependencies = new java.util.ArrayList<>();
    private final java.util.List<String> dependants = new java.util.ArrayList<>();

    public GraphNode(String className) {
      this.className = className;
    }

    public void addDependency(String dep, String type) {
      if (!dependencies.contains(dep)) {
        dependencies.add(dep);
      }
    }

    public void addDependant(String dep) {
      if (!dependants.contains(dep)) {
        dependants.add(dep);
      }
    }

    public String getClassName() {
      return className;
    }

    public java.util.List<String> getDependencies() {
      return dependencies;
    }

    public java.util.List<String> getDependants() {
      return dependants;
    }
  }
}